package entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import handler.PacketHandler;
import handler.SoundHandler;
import helper.HideDamage;
import helper.HideDamage.HideDamageCase;
import helper.HideMath;
import helper.RayTracer;
import helper.RayTracer.Hit;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockVine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import network.PacketHit;
import types.effect.Explosion;
import types.items.GunData;
import types.items.MagazineData;
import types.projectile.BulletData;

/** 銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加 */
public class EntityBullet extends Entity implements IEntityAdditionalSpawnData {

	/*
	 * ダメージ関係はすべてサーバーサイドで判別 弾が消失した場合DWで位置と通知 ブロックに当たった音はここで出す クライアントサイドで音関連を実行する
	 * 毎Tick通った範囲にプレイヤーがいないか見る…改善の余地あり
	 *
	 * 角度はモーションから計算
	 */
	private RayTracer RayTracer;

	public EntityBullet(World worldIn) {
		super(worldIn);
		this.setSize(0.5F, 0.5F);
		RayTracer = new RayTracer();
	}

	private float addtick;
	public Entity Shooter;
	private GunData gunData;
	private BulletData bulletData;
	private MagazineData magazineData;

	/** 当たったエンティティのリスト 多段ヒット防止用 */
	private List<Entity> AlreadyHit;
	/** あと何体に当たれるか */
	private int bulletPower;
	/** 飛距離 */
	private double FlyingDistance = 0;
	/** Tickスキップ保管用 */
	private long lastWorldTick = 0;

	/** 消えるまでの時間 */
	public float life = 0;

	public EntityBullet(GunData gun, MagazineData magazine, Entity shooter, boolean isADS, float offset, double x,
			double y, double z, float yaw, float pitch) {
		this(shooter.world);
		magazineData = magazine;
		bulletData = magazine.BULLETDATA;
		gunData = gun;
		Shooter = shooter;
		AlreadyHit = new ArrayList<>();
		addtick = offset;
		bulletPower = gunData.BULLET_POWER + bulletData.BULLET_POWER;
		life = bulletData.BULLET_LIFE;

		setLocationAndAngles(x, y, z, yaw, pitch);
		setPosition(posX, posY, posZ);
		// 精度の概念
		float accuracy = isADS ? gun.ACCURACY_ADS : gun.ACCURACY;
		double d = (float) Math.atan(accuracy / 50);
		rotationPitch = (float) HideMath.normal(rotationPitch, d);
		rotationYaw = (float) HideMath.normal(rotationYaw, d);
		// 向いている方向をモーションに
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionY = -Math.sin(Math.toRadians(rotationPitch));
		float f2 = MathHelper.sqrt(motionX * motionX + motionZ * motionZ + motionY * motionY);
		motionX /= f2;
		motionZ /= f2;
		motionY /= f2;

		setPosition(posX, posY, posZ);
	}

	@Override
	public void onUpdate() {
		if (lastWorldTick != 0) {
			onUpdate(world.getTotalWorldTime() - lastWorldTick);
			lastWorldTick = world.getTotalWorldTime();
		}
	}

	private void onUpdate(float tick) {
		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;

		this.prevPosX = this.posX + (this.motionX * tick);
		this.prevPosY = this.posY + (this.motionY * tick);
		this.prevPosZ = this.posZ + (this.motionZ * tick);
		if (!this.world.isRemote) {
			// 削除計算
			life -= tick;
			if (life <= 0) {
				setDead();
			}
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
		/** 前のtickの位置ベクトル */
		Vec3d lvo = new Vec3d(posX, posY, posZ);
		/** 今のtickの位置ベクトル */
		Vec3d lvt = new Vec3d(prevPosX, prevPosY, prevPosZ);

		/** 弾が消失した位置 */
		Vec3d endPos = lvt;

		/** ブロック衝突のフラグ */
		boolean isHittoBlock = false;
		/** レイトレーサーの終点の位置ベクトル */
		Vec3d lvend = lvt;

		// ブロックとの衝突
		for (RayTraceResult pos : RayTracer.getHitBlocks(world, lvo, lvt)) {
			Block block = world.getBlockState(pos.getBlockPos()).getBlock();
			// 透過するブロック
			if (block instanceof BlockBush || block instanceof BlockReed || block instanceof BlockSign
					|| block instanceof BlockVine) {
				continue;
			}
			boolean flag = false;
			for (String name : bulletData.THROUGH_BLOCK) {
				if (Block.isEqualTo(Block.getBlockFromName(name), block)) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				isHittoBlock = true;
				lvend = endPos = pos.hitVec;
				break;
			}
		}
		DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET, Shooter);
		if (bulletData.HIT_IGNORING_ARMOR)
			damagesource.setDamageBypassesArmor();

		// Entityとの衝突
		Iterator<Hit> HitEntitys = RayTracer.getHitEntity(this, world, lvo, lvend).iterator();
		while (HitEntitys.hasNext() && bulletPower > 0) {
			Hit hit = HitEntitys.next();
			Entity e = hit.entityHit;
			// 多段ヒット防止
			if (!AlreadyHit.contains(e)) {
				// ダメージが与えられる対象なら
				if (e instanceof EntityLivingBase && ((EntityLivingBase) e).deathTime == 0 && !(e.equals(Shooter))) {
					// System.out.println("Shooter "+Shooter+" HitEntity "+e);
					// ダメージを算出

					DamageTarget dt = DamageTarget.getTarget(e);
					// Missなら戻る
					if (dt == DamageTarget.Miss) {
						continue;
					}
					float damage = getFinalHitDamage(dt, FlyingDistance + lvo.distanceTo(hit.hitVec));

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
					explode(hit.hitVec, bulletData.EXP_ON_HIT_ENTITY);

					// ヒットマーク
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
		FlyingDistance += lvo.distanceTo(endPos);
		// 消去処理
		if (isHittoBlock || bulletPower <= 0 || life <= 0) {
			if (isHittoBlock) {
				explode(endPos, bulletData.EXP_ON_HIT_GROUND);
			}
			if (life <= 0) {
				explode(endPos, bulletData.EXP_ON_TIMEOUT);
			}
			setDead();
		}
	}

	private void ClientUpdate() {

	}

	@Override
	protected void entityInit() {

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
			// TODO エフェクト
		}
	}

	/** 直撃ダメージ算出 */
	private float getFinalHitDamage(DamageTarget target, double distance) {
		float damage = 0;
		float decayStart;
		float decayDistance;
		float decayAmount;
		switch (target) {
		case Living:
			damage = bulletData.HIT_DAMAGE_LIVING;
			// 銃による変化
			damage *= gunData.LIVING_DAMAGE_DIAMETER;
			damage += gunData.LIVING_DAMAGE_ADD;
			// 減衰開始からの距離を作成
			decayStart = bulletData.DECAY_DAMAGE_START_LIVING;
			decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);
			decayAmount = decayDistance * bulletData.DECAY_DAMAGE_COE_LIVING;
			// 最大変化量を超えていないか
			float maxAmount = bulletData.DECAY_DAMAGE_MAX_LIVING;
			decayAmount = Math.abs(decayAmount) < maxAmount ? decayAmount : maxAmount;
			damage += decayAmount;
			break;
		case Player:
			damage = bulletData.HIT_DAMAGE_PLAYER;
			// 銃による変化
			damage *= gunData.PLAYER_DAMAGE_DIAMETER;
			damage += gunData.PLAYER_DAMAGE_ADD;
			// 減衰開始からの距離を作成
			decayStart = bulletData.DECAY_DAMAGE_START_PLAYER;
			decayDistance = (float) (distance > decayStart ? distance - decayStart : 0);
			decayAmount = decayDistance * bulletData.DECAY_DAMAGE_COE_PLAYER;
			// 最大変化量を超えていないか
			maxAmount = bulletData.DECAY_DAMAGE_MAX_PLAYER;
			decayAmount = Math.abs(decayAmount) < maxAmount ? decayAmount : maxAmount;
			damage += decayAmount;
			break;
		case Vehicle:
			break;
		case Aircraft:
			break;
		default:
			break;
		}
		return damage;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {

	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeFloat(rotationYaw);
		buffer.writeFloat(rotationPitch);
		buffer.writeDouble(motionX);
		buffer.writeDouble(motionY);
		buffer.writeDouble(motionZ);
		lastWorldTick = world.getTotalWorldTime();
		buffer.writeDouble(lastWorldTick);
		buffer.writeFloat(addtick);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		rotationYaw = buffer.readFloat();
		rotationPitch = buffer.readFloat();
		motionX = buffer.readDouble();
		motionY = buffer.readDouble();
		motionZ = buffer.readDouble();
		float tickt = (float) (world.getTotalWorldTime() - buffer.readDouble());
		tickt = tickt < 0 ? 0 : tickt;
		tickt += buffer.readFloat();
		lastWorldTick = world.getTotalWorldTime();
		onUpdate(tickt);
	}

	public enum DamageTarget {
		Player, Living, Vehicle, Aircraft, Miss;
		public static DamageTarget getTarget(Entity entity) {
			if (entity instanceof EntityPlayer) {
				return Player;
			} else if (entity instanceof EntityLivingBase) {
				return Living;
			}
			// TODO 判別
			return Miss;
		}
	}
}
