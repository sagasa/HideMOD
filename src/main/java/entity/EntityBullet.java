package entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.logging.log4j.core.net.DatagramSocketManager;

import handler.PacketHandler;
import hideMod.HideMod;
import helper.RayTracer;
import io.netty.buffer.ByteBuf;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider.WorldSleepResult;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.BulletData;
import types.Explosion;
import types.GunData;
import types.Sound;

/** 銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加 */
public class EntityBullet extends Entity implements IEntityAdditionalSpawnData {

	/*
	 * ダメージ関係はすべてサーバーサイドで判別 弾が消失した場合DWで位置と通知 ブロックに当たった音はここで出す クライアントサイドで音関連を実行する
	 * 毎Tick通った範囲にプレイヤーがいないか見る…改善の余地あり
	 *
	 * 角度はモーションから計算
	 */

	public EntityBullet(World worldIn) {
		super(worldIn);
		this.setSize(0.5F, 0.5F);
		RayTracer = new RayTracer();
	}

	int life = 60;
	public int tick = 0;
	public Entity Shooter;

	// サーバーサイドでしか代入されていないので注意
	/** データ取り出し元 */
	private GunData gunData;
	private BulletData bulletData;
	private RayTracer RayTracer;
	/** 当たったエンティティのリスト 多段ヒット防止用 */
	private List<Entity> AlreadyHit;
	/** あと何体に当たれるか */
	private int bulletPower;
	/** 飛距離 */
	private double FlyingDistance = 0;
	/** 乱数!! */
	private static Random Random = new Random();


	boolean fastTick = true;

	byte deathNaxtTick = 0;

	/* データマネージャーパラメータ */
	private static final DataParameter<Integer> a  = EntityDataManager.createKey(EntityBullet.class, DataSerializers.VARINT);


	public EntityBullet(GunData gun, BulletData bullet, Entity shooter, double x, double y, double z,
			float yaw, float pitch, float offset, boolean isADS) {
		this(shooter.world);

		this.gunData = gun;
		this.bulletData = bullet;
		Shooter = shooter;
		AlreadyHit = new ArrayList<Entity>();
		bulletPower = gunData.BULLET_POWER + bulletData.BULLET_POWER;

		setLocationAndAngles(x,y,z,yaw,pitch);
		setPosition(posX, posY, posZ);

		// データから読み取る
		float spead = gunData.BULLET_SPEED;

		// 向いている方向をモーションに
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionY = -Math.sin(Math.toRadians(rotationPitch));

		float f2 = MathHelper.sqrt(motionX * motionX + motionZ * motionZ + motionY * motionY);
		motionX /= f2;
		motionZ /= f2;
		motionY /= f2;

		motionX *= spead;
		motionY *= spead;
		motionZ *= spead;
	}

	@Override
	protected void entityInit() {
		EntityDataManager dm = getDataManager();

	}

	@Override
	public void onUpdate() {
		// 初期化
		if (fastTick && Vec0 != null) {
			this.motionX = Vec0.x;
			this.motionY = Vec0.y;
			this.motionZ = Vec0.z;
		}

		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;

		this.prevPosX = this.posX + this.motionX;
		this.prevPosY = this.posY + this.motionY;
		this.prevPosZ = this.posZ + this.motionZ;

		tick++;
		if (life < tick) {
			setDead();
		}
		if (!this.world.isRemote) {
			ServerUpdate();
		} else {
			ClientUpdate();
		}
		this.posX = this.prevPosX;
		this.posY = this.prevPosY;
		this.posZ = this.prevPosZ;
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	private void ClientUpdate() {
		// クライアントサイド
		// データ同期
		DataWatcher dw = getDataWatcher();
		byte hitState = dw.getWatchableObjectByte(DATAWATCHER_END);
		if (hitState != 0) {
			posX = dw.getWatchableObjectFloat(DATAWATCHER_POSX);
			posY = dw.getWatchableObjectFloat(DATAWATCHER_POSY);
			posZ = dw.getWatchableObjectFloat(DATAWATCHER_POSZ);
			// System.out.println(((hitState&MASK_HITBLOCK)==MASK_HITBLOCK) +"
			// "+hitState);
			// 地面への着弾音
			if ((hitState & MASK_HITBLOCK) == MASK_HITBLOCK) {
				SoundHandler.playSound(posX, posY, posZ,
						(Sound) bulletData.SOUND_HIT_GROUND);
			}
			// EntityFX fx =
			// Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(2,
			// posX, posY, posZ, 0D, 0D, 0D, new int[0]);
			// fx.renderDistanceWeight = 200;
			setDead();
		}
		// this.worldObj.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,posX,posY,posZ,0,0,0,new
		// int[0]);
		// this.posX, this.posY, this.posZ, 1, 1, 1, new int[0]);
	}

	private void ServerUpdate() {
		// サーバーサイド
		// フラグ回収
		if (deathNaxtTick != 0) {
			setDead();
			return;
		}

		/** ブロック衝突のフラグ */
		boolean isHittoBlock = false;

		/** 前のtickの位置ベクトル */
		Vec3 lvo = new Vec3(posX, posY, posZ);
		/** 今のtickの位置ベクトル */
		Vec3d lvt = new Vec3(prevPosX, prevPosY, prevPosZ);

		/** 弾が消失した位置 */
		Vec3d endPos = lvt;

		/** レイトレーサーの終点の位置ベクトル */
		Vec3d lvend = lvt;

		for (Hit pos : RayTracer.getHitBlock(this, world, lvo, lvt)) {
			Block block = world.getBlockState(pos.blockPos).getBlock();
			// 透過するブロック
			if (!(block instanceof BlockBush || block instanceof BlockReed || block instanceof BlockSign
					|| block instanceof BlockVine)) {
				isHittoBlock = true;
				lvend = endPos = pos.hitVec;
				break;
			}
		}
		DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET, Shooter).setDamageBypassesArmor();

		System.out.println(RayTracer.getHit(new HideCollision(), new Vec3(0, 1, 0), new Vec3(0, -1, 0)));

		// LivingEntityに対するあたり判定
		// BulletPowerが残ってる間HITを取る
		Iterator<Hit> HitEntitys = RayTracer.getHitEntity(this, world, lvo, lvend).iterator();
		// System.out.println(bulletPower);
		while (HitEntitys.hasNext() && bulletPower > 0) {
			Hit hit = HitEntitys.next();
			Entity e = hit.entity;
			// 多段ヒット防止
			if (!AlreadyHit.contains(e)) {
				// ダメージが与えられる対象なら
				if (e instanceof EntityLivingBase && ((EntityLivingBase) e).deathTime == 0 && !(e == Shooter)) {
					// ダメージを算出
					FlyingDistance += lvo.distanceTo(hit.hitVec);
					float damage = getFinalLivingDamage((EntityLivingBase) e, FlyingDistance);

					boolean isHeadShot = false;
					// ヘッドショットを判定するEntity
					if (e instanceof EntityPlayer || e instanceof EntityZombie || e instanceof EntityPigZombie
							|| e instanceof EntitySkeleton || e instanceof EntityVillager) {
						isHeadShot = RayTracer.isHeadShot(e, lvo, lvend);
						if (isHeadShot) {
							damage *= 2;
						}
					}
					// ダメージを与える
					boolean isDamaged = HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);

					// 爆発があるなら
					explode(endPos,(Explosion) bulletData.EXP_ON_HIT_ENTITY);

					System.out.println(isDamaged);
					// パケット
					if (Shooter instanceof EntityPlayerMP&&isDamaged&&damage>0.5) {
						PacketHandler.INSTANCE.sendTo(new PacketHit(isHeadShot), (EntityPlayerMP) Shooter);
					}
					bulletPower--;
					AlreadyHit.add(e);
					// もしこの衝突で消えたなら
					if (bulletPower == 0) {
						endPos = hit.hitVec;
						isHittoBlock = false;
						break;
					}
				}
			}
		}
		// 削除系
		if(bulletData==null){
			setDead();
			return;
		}
		if (bulletPower == 0 || isHittoBlock || life < tick) {
			// 爆破処理
			deathNaxtTick = MASK_HIT;
			if (isHittoBlock) {
				deathNaxtTick = (byte) (deathNaxtTick | MASK_HITBLOCK);
			}
			DataWatcher dw = getDataWatcher();
			dw.updateObject(DATAWATCHER_END, deathNaxtTick);
			dw.updateObject(DATAWATCHER_POSX, (float) endPos.xCoord);
			dw.updateObject(DATAWATCHER_POSY, (float) endPos.yCoord);
			dw.updateObject(DATAWATCHER_POSZ, (float) endPos.zCoord);

			// 時間経過で爆発するなら
			explode(endPos,(Explosion) bulletData.EXP_ON_TIMEOUT);

			// 地面に当たったなら
			explode(endPos,(Explosion) bulletData.EXP_ON_HIT_GROUND);


			// System.out.println(endPos.xCoord + " " + endPos.yCoord + " " +
			// endPos.zCoord+" "+worldObj.getWorldTime());
		}
		// 距離計算
		FlyingDistance += lvo.distanceTo(lvt);
	}

	private void explode(Vec3 endPos,Explosion exp) {
		// 爆発があるなら
		float range = exp.RANGE;
		if (range > 0) {
			List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(endPos.xCoord - range,
					endPos.yCoord - range, endPos.zCoord - range, endPos.xCoord + range, endPos.yCoord + range, endPos.zCoord + range));

			for (Entity e : list) {
				double dis = new Vec3(e.posX, e.posY, e.posZ).distanceTo(endPos);
				if (!(e instanceof EntityLivingBase) || dis > range) {
					continue;
				}
				// ダメージを算出
				float damage = 0;
				if (e instanceof EntityPlayer) {
					damage = exp.DAMAGE_BASE_PLAYER;
					damage -= exp.DAMAGE_COE_PLAYER * dis;
				} else if (e instanceof EntityLiving) {
					damage = exp.DAMAGE_BASE_LIVING;
					damage -= exp.DAMAGE_COE_LIVING * dis;
				}

				DamageSource damagesource = new HideDamage(HideDamageCase.GUN_Explosion, Shooter)
						.setDamageBypassesArmor();
				// ダメージを与える
				HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);
			}
			//サウンド
			SoundHandler.broadcastSound(world, endPos.xCoord, endPos.yCoord, endPos.zCoord, exp.SOUND);
		}
	}

	/** EntityLivingBaseに対するダメージ算出 */
	private float getFinalLivingDamage(EntityLivingBase target, double distance) {
		float damage = 0;
		// プレイヤーなら
		if (target instanceof EntityPlayer) {
			damage = bulletData.HIT_DAMAGE_PLAYER;
			// 銃による変化
			damage *= gunData.PLAYER_DAMAGE_DIAMETER;
			damage += gunData.PLAYER_DAMAGE_ADD;
			// 減衰開始からの距離を作成
			float decayStart = bulletData.DECAY_DAMAGE_START_PLAYER;
			float decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);

			float decayAmount = decayDistance * bulletData.DECAY_DAMAGE_COE_PLAYER;
			// 最大変化量を超えていないか
			float maxAmount = bulletData.DECAY_DAMAGE_MAX_PLAYER;
			decayAmount = Math.abs(decayAmount) < maxAmount ? decayAmount : maxAmount;
			damage += decayAmount;
		} else {
			damage = bulletData.HIT_DAMAGE_LIVING;
			// 銃による変化
			damage *= gunData.LIVING_DAMAGE_DIAMETER;
			damage += gunData.LIVING_DAMAGE_ADD;
			// 減衰開始からの距離を作成
			float decayStart = bulletData.DECAY_DAMAGE_START_LIVING;
			float decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);

			float decayAmount = decayDistance * bulletData.DECAY_DAMAGE_COE_LIVING;
			// 最大変化量を超えていないか
			float maxAmount = bulletData.DECAY_DAMAGE_MAX_LIVING;
			decayAmount = Math.abs(decayAmount) < maxAmount ? decayAmount : maxAmount;
			damage += decayAmount;
		}
		return damage;
	}

	/** クライアントに必要な情報を送る */
	@Override
	public void writeSpawnData(ByteBuf buffer) {
		// System.out.println("write to "+rotationYaw+ " "+rotationPitch+ "
		// "+motionX);
		buffer.writeFloat(rotationYaw);
		buffer.writeFloat(rotationPitch);
		buffer.writeDouble(motionX);
		buffer.writeDouble(motionY);
		buffer.writeDouble(motionZ);
		if (bulletData != null) {
			buffer.writeBoolean(true);
			PacketHandler.writeString(buffer, bulletData.ITEM_INFO.NAME_SHORT);
			PacketHandler.writeString(buffer, gunData.ITEM_INFO.NAME_SHORT);
		} else {
			buffer.writeBoolean(false);
		}

	}

	/** サーバーからの情報を変数に書き込む */
	@Override
	public void readSpawnData(ByteBuf Data) {
		rotationYaw = Data.readFloat();
		rotationPitch = Data.readFloat();
		Vec0 = new Vec3d(Data.readDouble(), Data.readDouble(), Data.readDouble());
		if (Data.readBoolean()) {
			bulletData = ItemMagazine.getBulletData(PacketHandler.readString(Data));
			gunData = ItemGun.getGunData(PacketHandler.readString(Data));
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead(); //TODO
		long top = tag.getLong("ShooterUUID_top");
		long last = tag.getLong("ShooterUUID_last");
		String name = tag.getString("ShooterName");
		Shooter_uuid = new UUID(top, last);
		EntityPlayer shooter = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager()
				.getPlayerByUsername(name);
		if (shooter == null) {
			this.setDead();
		} else {
			Shooter = shooter;
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		if (Shooter != null) {
			tag.setLong("ShooterUUID_top", Shooter_uuid.getMostSignificantBits());
			tag.setLong("ShooterUUID_last", Shooter_uuid.getLeastSignificantBits());
			tag.setString("ShooterName", Shooter.getName());
		} else {
			setDead();
		}
	}

}
