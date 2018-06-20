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
import handler.SoundHandler;
import helper.RayTracer;
import helper.RayTracer.HitBlock;
import helper.RayTracer.HitEntity;
import hideMod.HideMod;
import io.netty.buffer.ByteBuf;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockVine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.DataWatcher;
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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newwork.PacketGuns;
import newwork.PacketHit;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.Sound;
import types.guns.GunData;
import types.guns.GunData.GunDataList;
import types.inGame.HideDamage;
import types.inGame.HideSound;
import types.inGame.HideDamage.HideDamageCase;

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
	public EntityLivingBase Shooter;

	// サーバーサイドでしか代入されていないので注意
	/** データ取り出し元 */
	GunData gunData;
	BulletData bulletData;
	UUID Shooter_uuid;
	RayTracer RayTracer;
	/** 当たったエンティティのリスト 多段ヒット防止用 */
	List<Entity> AlreadyHit;
	/** あと何体に当たれるか */
	int bulletPower;
	/** 飛距離 */
	double FlyingDistance = 0;
	/** 乱数!! */
	static Random Random = new Random();

	/** init時点での速度 */
	Vec3 Vec0;

	boolean fastTick = true;

	byte deathNaxtTick = 0;

	/* データウォッチャーID */
	private static final int DATAWATCHER_END = 5;
	private static final byte MASK_HITBLOCK = 0b10;
	private static final byte MASK_HIT = 0b1;
	private static final int DATAWATCHER_POSX = 6;
	private static final int DATAWATCHER_POSY = 7;
	private static final int DATAWATCHER_POSZ = 8;

	public EntityBullet(World worldIn, EntityLivingBase shooter, GunData gunData, BulletData bulletData, float yaw,
			float pitch) {
		this(worldIn);

		this.gunData = gunData;
		this.bulletData = bulletData;
		Shooter = shooter;
		Shooter_uuid = shooter.getUniqueID();
		AlreadyHit = new ArrayList<Entity>();
		bulletPower = gunData.getDataInt(GunDataList.BULLET_POWER) + bulletData.getDataInt(BulletDataList.BULLET_POWER);
		// System.out.println("Start;
		// "+gunData.getDataInt(GunDataList.BULLET_POWER));
		// System.out.println(this.worldObj.isRemote);

		// Shooter.addChatMessage(new ChatComponentText("発射"));
		// データ格納

		setLocationAndAngles(Shooter.posX, Shooter.posY + Shooter.getEyeHeight(), Shooter.posZ, yaw, pitch);
		setPosition(posX, posY, posZ);

		// データから読み取る
		float spead = gunData.getDataFloat(GunDataList.BULLET_SPEED);

		// 向いている方向をモーションに
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionY = -Math.sin(Math.toRadians(rotationPitch));

		float f2 = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ + motionY * motionY);
		motionX /= f2;
		motionZ /= f2;
		motionY /= f2;

		motionX *= spead;
		motionY *= spead;
		motionZ *= spead;
		Vec0 = new Vec3(motionX, motionY, motionZ);
	}

	@Override
	protected void entityInit() {
		DataWatcher dw = getDataWatcher();
		dw.addObject(DATAWATCHER_END, deathNaxtTick);
		dw.addObject(DATAWATCHER_POSX, 0f);
		dw.addObject(DATAWATCHER_POSY, 0f);
		dw.addObject(DATAWATCHER_POSZ, 0f);
	}

	@Override
	public void onUpdate() {
		// 初期化
		if (fastTick && Vec0 != null) {
			this.motionX = Vec0.xCoord;
			this.motionY = Vec0.yCoord;
			this.motionZ = Vec0.zCoord;
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
		if (!this.worldObj.isRemote) {
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
						(Sound) bulletData.getDataObject(BulletDataList.SOUND_HIT_GROUND));
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
		Vec3 lvt = new Vec3(prevPosX, prevPosY, prevPosZ);

		/** 弾が消失した位置 */
		Vec3 endPos = lvt;

		/** レイトレーサーの終点の位置ベクトル */
		Vec3 lvend = lvt;

		for (HitBlock pos : RayTracer.getHitBlock(this, worldObj, lvo, lvt)) {
			Block block = worldObj.getBlockState(pos.blockPos).getBlock();
			// 透過するブロック
			if (!(block instanceof BlockBush || block instanceof BlockReed || block instanceof BlockSign
					|| block instanceof BlockVine)) {
				isHittoBlock = true;
				lvend = endPos = pos.hitVec;
				break;
			}
		}
		DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET, Shooter).setDamageBypassesArmor();

		// LivingEntityに対するあたり判定
		// BulletPowerが残ってる間HITを取る
		Iterator<HitEntity> HitEntitys = RayTracer.getHitEntity(this, worldObj, lvo, lvend).iterator();
		// System.out.println(bulletPower);
		while (HitEntitys.hasNext() && bulletPower > 0) {
			HitEntity hit = HitEntitys.next();
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
					HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);

					// パケット
					if (Shooter instanceof EntityPlayerMP) {
						PacketHandler.INSTANCE.sendTo(new PacketHit(isHeadShot), (EntityPlayerMP) Shooter);
					}
					bulletPower--;
					AlreadyHit.add(e);
					// もしこの衝突で消えたなら
					if (bulletPower == 0) {
						endPos = hit.hitVec;
						isHittoBlock = false;
					}
				}
			}
		}
		// 削除系
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

			onImpact();
			// System.out.println(endPos.xCoord + " " + endPos.yCoord + " " +
			// endPos.zCoord+" "+worldObj.getWorldTime());
		}
		// 距離計算
		FlyingDistance += lvo.distanceTo(lvt);
		// データ同期

	}

	private void onImpact() {
		// 爆発があるなら
		float range = bulletData.getDataInt(BulletDataList.EXP_RANGE);
		System.out.println(range + bulletData.getDataString(BulletDataList.EXP_DAMAGE_BASE_LIVING));
		if (range > 0) {
			List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(posX - range,
					posY - range, posZ - range, posX + range, posY + range, posZ + range));
			System.out.println(list);
			for (Entity e : list) {
				double dis = new Vec3(e.posX, e.posY, e.posZ).distanceTo(new Vec3(this.posX, this.posY, this.posZ));
				if (!(e instanceof EntityLivingBase) || dis > range) {
					continue;
				}
				// ダメージを算出
				float damage = 0;
				if (e instanceof EntityPlayer) {
					damage = bulletData.getDataFloat(BulletDataList.EXP_DAMAGE_BASE_PLAYER);
					damage -= bulletData.getDataFloat(BulletDataList.EXP_DAMAGE_COE_PLAYER) * dis;
				} else if (e instanceof EntityLiving) {
					damage = bulletData.getDataFloat(BulletDataList.EXP_DAMAGE_BASE_LIVING);
					damage -= bulletData.getDataFloat(BulletDataList.EXP_DAMAGE_COE_LIVING) * dis;
				}

				DamageSource damagesource = new HideDamage(HideDamageCase.GUN_Explosion, Shooter)
						.setDamageBypassesArmor();
				// ダメージを与える
				HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);
			}
		}
	}

	/** EntityLivingBaseに対するダメージ算出 */
	private float getFinalLivingDamage(EntityLivingBase target, double distance) {
		float damage = 0;
		// プレイヤーなら
		if (target instanceof EntityPlayer) {
			damage = bulletData.getDataFloat(BulletDataList.HIT_DAMAGE_PLAYER);
			// 銃による変化
			damage *= gunData.getDataFloat(GunDataList.PLAYER_DAMAGE_DIAMETER);
			damage += gunData.getDataFloat(GunDataList.PLAYER_DAMAGE_ADD);
			// 減衰開始からの距離を作成
			float decayStart = bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_START_PLAYER);
			float decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);

			float decayAmount = decayDistance * bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_COE_PLAYER);
			// 最大変化量を超えていないか
			float maxAmount = bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_MAX_PLAYER);
			decayAmount = Math.abs(decayAmount) < maxAmount ? decayAmount : maxAmount;
			damage += decayAmount;
		} else {
			damage = bulletData.getDataFloat(BulletDataList.HIT_DAMAGE_LIVING);
			// 銃による変化
			damage *= gunData.getDataFloat(GunDataList.LIVING_DAMAGE_DIAMETER);
			damage += gunData.getDataFloat(GunDataList.LIVING_DAMAGE_ADD);
			// 減衰開始からの距離を作成
			float decayStart = bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_START_LIVING);
			float decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);

			float decayAmount = decayDistance * bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_COE_LIVING);
			// 最大変化量を超えていないか
			float maxAmount = bulletData.getDataFloat(BulletDataList.DECAY_DAMAGE_MAX_LIVING);
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
			PacketHandler.writeString(buffer, bulletData.getItemInfo().shortName);
			PacketHandler.writeString(buffer, gunData.getItemInfo().shortName);
		} else {
			buffer.writeBoolean(false);
		}

	}

	/** サーバーからの情報を変数に書き込む */
	@Override
	public void readSpawnData(ByteBuf Data) {
		rotationYaw = Data.readFloat();
		rotationPitch = Data.readFloat();
		Vec0 = new Vec3(Data.readDouble(), Data.readDouble(), Data.readDouble());
		if (Data.readBoolean()) {
			bulletData = ItemMagazine.getBulletData(PacketHandler.readString(Data));
			gunData = ItemGun.getGunData(PacketHandler.readString(Data));
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
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
