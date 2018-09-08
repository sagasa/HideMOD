package entity;

import java.io.IOException;
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
import handler.RecoilHandler;
import handler.SoundHandler;
import hideMod.HideMod;
import helper.RayTracer;
import helper.RayTracer.Hit;
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
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider.WorldSleepResult;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newwork.PacketHit;
import playerdata.HideDamage;
import playerdata.HideDamage.HideDamageCase;
import types.BulletData;
import types.Explosion;
import types.GunData;
import types.Sound;
import types.model.HideCollision;

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

	public float life = 0;

	private RayTracer RayTracer;

	// サーバーのみ
	private float addtick;
	// サーバー・クライアント両方で代入されているべき
	private GunData gunData;
	private BulletData bulletData;
	public Entity Shooter;

	/** 当たったエンティティのリスト 多段ヒット防止用 */
	private List<Entity> AlreadyHit;
	/** あと何体に当たれるか */
	private int bulletPower;
	/** 飛距離 */
	private double FlyingDistance = 0;

	/** Tickスキップ保管用 */
	private long lastWorldTick = 0;

	byte deathNaxtTick = 0;

	/* データマネージャーパラメータ */
	private static final DataParameter<Vec3d> Vec3d = EntityDataManager.createKey(EntityBullet.class,
			PacketHandler.Vec3d);
	private static final DataParameter<Byte> Flag = EntityDataManager.createKey(EntityBullet.class,
			DataSerializers.BYTE);

	private static final byte FLAG_EMPTY = 0b0000000;
	private static final byte FLAG_DEATH_NEXT_TICK = 0b0000001;
	private static final byte FLAG_DEATH_TYPE_GROUND = 0b0000010;
	private static final byte FLAG_DEATH_TYPE_ENTITY = 0b0000100;

	public EntityBullet(GunData gun, BulletData bullet, Entity shooter, double x, double y, double z, float yaw,
			float pitch, float offset, boolean isADS) {
		this(shooter.world);
		this.addtick = offset;
		this.gunData = gun;
		this.bulletData = bullet;
		Shooter = shooter;
		AlreadyHit = new ArrayList<Entity>();
		bulletPower = gunData.BULLET_POWER + bulletData.BULLET_POWER;

		setLocationAndAngles(x, y, z, yaw, pitch);
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
		setPosition(posX, posY, posZ);
	}

	@Override
	protected void entityInit() {
		// 初期化
		EntityDataManager dm = getDataManager();
		dm.register(Vec3d, new Vec3d(0, 0, 0));
		dm.register(Flag, FLAG_EMPTY);
	}

	@Override
	public void onUpdate() {
		onUpdate(world.getTotalWorldTime() - lastWorldTick);
		lastWorldTick = world.getTotalWorldTime();
	}

	private void onUpdate(float tick) {
		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;

		this.prevPosX = this.posX + (this.motionX * tick);
		this.prevPosY = this.posY + (this.motionY * tick);
		this.prevPosZ = this.posZ + (this.motionZ * tick);

		this.life += tick;

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
		Vec3d lvo = new Vec3d(posX, posY, posZ);
		/** 今のtickの位置ベクトル */
		Vec3d lvt = new Vec3d(prevPosX, prevPosY, prevPosZ);

		/** 弾が消失した位置 */
		Vec3d endPos = lvt;

		/** レイトレーサーの終点の位置ベクトル */
		Vec3d lvend = lvt;

		for (Hit pos : RayTracer.getHitBlock(this, world, lvo, lvt)) {
			Block block = world.getBlockState(pos.getBlockPos()).getBlock();
			// 透過するブロック
			if (!(block instanceof BlockBush || block instanceof BlockReed || block instanceof BlockSign
					|| block instanceof BlockVine)) {
				isHittoBlock = true;
				lvend = endPos = pos.hitVec;
				break;
			}
		}
		DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET, Shooter).setDamageBypassesArmor();

		// System.out.println(RayTracer.getHit(new HideCollision(), new Vec3d(0,
		// 1, 0), new Vec3d(0, -1, 0)));

		// LivingEntityに対するあたり判定
		// BulletPowerが残ってる間HITを取る
		Iterator<Hit> HitEntitys = RayTracer.getHitEntity(this, world, lvo, lvend).iterator();
		// System.out.println(bulletPower);
		while (HitEntitys.hasNext() && bulletPower > 0) {
			Hit hit = HitEntitys.next();
			Entity e = hit.entityHit;
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
					explode(endPos, (Explosion) bulletData.EXP_ON_HIT_ENTITY);

					// パケット
					if (Shooter instanceof EntityPlayerMP && isDamaged && damage > 0.5) {
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
		if (bulletData == null) {
			setDead();
			return;
		}
		if (bulletPower == 0 || isHittoBlock || bulletData.BULLET_LIFE < life) {
			EntityDataManager dm = getDataManager();
			Vec3d exppos = endPos.add(lvt.subtract(lvo).normalize().scale(-0.2));
			System.out.println(endPos+" "+exppos);
			if (bulletPower == 0) {
				deathNaxtTick = FLAG_DEATH_NEXT_TICK | FLAG_DEATH_TYPE_ENTITY;
				// エンティティに当たって爆発するなら
				explode(exppos, (Explosion) bulletData.EXP_ON_HIT_ENTITY);
			}
			if (bulletData.BULLET_LIFE < life) {
				deathNaxtTick = FLAG_DEATH_NEXT_TICK;
				// 時間経過で爆発するなら
				explode(exppos, (Explosion) bulletData.EXP_ON_TIMEOUT);
			}
			if (isHittoBlock) {
				deathNaxtTick = FLAG_DEATH_NEXT_TICK | FLAG_DEATH_TYPE_GROUND;
				// 地面に当たったなら
				explode(exppos, (Explosion) bulletData.EXP_ON_HIT_GROUND);
			}
			dm.set(Vec3d, endPos);
			dm.set(Flag, deathNaxtTick);
		}
		// 距離計算
		FlyingDistance += lvo.distanceTo(lvt);
	}

	private void ClientUpdate() {
		// クライアントサイド
		// データ同期
		EntityDataManager dm = getDataManager();
		byte flag = dm.get(Flag);
		if ((flag & FLAG_DEATH_NEXT_TICK) == FLAG_DEATH_NEXT_TICK) {
			Vec3d pos = dm.get(Vec3d);
			posX = pos.x;
			posY = pos.y;
			posZ = pos.z;
			SoundHandler.playSound(posX, posY, posZ, bulletData.SOUND_HIT_GROUND);

			setDead();
		}
	}

	private void explode(Vec3d endPos, Explosion exp) {
		// 爆発があるなら
		float range = exp.RANGE;
		if (range > 0) {
			List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(endPos.x - range,
					endPos.y - range, endPos.z - range, endPos.x + range, endPos.y + range, endPos.z + range));

			for (Entity e : list) {
				if (!(e instanceof EntityLivingBase)) {
					continue;
				}
				// 障害物の判定
				double dis = -1;
				System.out.println(world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY, e.posZ), false, true, false));
				if (world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY, e.posZ), false, true, false) == null) {
					dis = new Vec3d(e.posX, e.posY, e.posZ).distanceTo(endPos);
				}
				if (world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ), false, true,
						false) == null) {
					double d = new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ).distanceTo(endPos);
					dis = dis > d ? d : dis;
				}
				if (dis > range) {
					continue;
				}
				// ダメージを算出
				float damage = 1;
				if (dis != -1) {
					if (e instanceof EntityPlayer) {
						damage = exp.DAMAGE_BASE_PLAYER;
						damage -= exp.DAMAGE_COE_PLAYER * dis;
					} else if (e instanceof EntityLiving) {
						damage = exp.DAMAGE_BASE_LIVING;
						damage -= exp.DAMAGE_COE_LIVING * dis;
					}
				}

				DamageSource damagesource = new HideDamage(HideDamageCase.GUN_Explosion, Shooter);
				// ダメージを与える
				HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);
			}
			// サウンド
			SoundHandler.broadcastSound(world, endPos.x, endPos.y, endPos.z, exp.SOUND);
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
		lastWorldTick = world.getTotalWorldTime();
		// System.out.println("to Client");
		buffer.writeFloat(rotationYaw);
		buffer.writeFloat(rotationPitch);
		buffer.writeDouble(motionX);
		buffer.writeDouble(motionY);
		buffer.writeDouble(motionZ);
		buffer.writeDouble(world.getTotalWorldTime());
		buffer.writeFloat(addtick);
		buffer.writeInt(Shooter.getEntityId());
		PacketHandler.writeString(buffer, bulletData.ITEM_INFO.NAME_SHORT);
		PacketHandler.writeString(buffer, gunData.ITEM_INFO.NAME_SHORT);
		onUpdate(addtick);
	}

	/** サーバーからの情報を変数に書き込む */
	@Override
	public void readSpawnData(ByteBuf buf) {
		lastWorldTick = world.getTotalWorldTime();
		// System.out.println("form Server");
		rotationYaw = buf.readFloat();
		rotationPitch = buf.readFloat();
		motionX = buf.readDouble();
		motionY = buf.readDouble();
		motionZ = buf.readDouble();
		float tickt = (float) (world.getTotalWorldTime() - buf.readDouble());
		tickt = tickt < 0 ? 0 : tickt;
		tickt += buf.readFloat();
		Shooter = world.getEntityByID(buf.readInt());
		bulletData = ItemMagazine.getBulletData(PacketHandler.readString(buf));
		gunData = ItemGun.getGunData(PacketHandler.readString(buf));
		onUpdate(tickt);
		// オーナーならリコイルを追加
		RecoilHandler.addRecoil(gunData, Shooter);

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
	}

}
