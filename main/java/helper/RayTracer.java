package helper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import entity.EntityBullet;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class RayTracer {
	/** 比較用の数値とエンティティセットのクラス */
	public class HitEntity implements Comparable {
		double range;
		public Vec3 hitVec;
		public Entity entity;

		HitEntity(Entity e, Vec3 hitvec, double range) {
			this.range = range;
			this.entity = e;
			this.hitVec = hitvec;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof HitEntity) {
				return (int) Math.ceil(this.range-((HitEntity) o).range);
			}
			return 0;
		}
	}

	/** BlockPosのみを比較するクラス */
	public class HitBlock {
		public Vec3 hitVec;
		public BlockPos blockPos;

		HitBlock(BlockPos block, Vec3 hetvec) {
			this.hitVec = hetvec;
			this.blockPos = block;
		}

		@Override
		public boolean equals(Object obj) {
			return blockPos.equals(((HitBlock)obj).blockPos);
		}
	}

	/** 当たったブロックを取得*/
	public HitBlock[] getHitBlock(Entity owner, World w, Vec3 lv0, Vec3 lvt){
		ArrayList<HitBlock> hitBlocks = new ArrayList<HitBlock>();

		Vec3 vm = lvt.subtract(lv0).normalize();
		MovingObjectPosition lmop1 = w.rayTraceBlocks(lv0, lvt);
		Vec3 lvm = lmop1!=null?lmop1.hitVec:lv0;
		for (;lmop1!=null&&lvm.distanceTo(lv0)<lvt.distanceTo(lv0);lmop1 = w.rayTraceBlocks(lvm, lvt)){
			//hitBlocks.add(arg0)
			HitBlock Hit = new HitBlock(lmop1.getBlockPos(),lmop1.hitVec);
			if(!hitBlocks.contains(Hit)){
				hitBlocks.add(Hit);
			}
			lvm = lvm.add(vm);
		}
		return hitBlocks.toArray(new HitBlock[hitBlocks.size()]);
	}

	/** 部位ダメージ判定 */
	public boolean isHeadShot(Entity tirget, Vec3 lv0, Vec3 lvt) {
		// 頭の判定
		AxisAlignedBB head = new AxisAlignedBB(tirget.posX - 0.3, tirget.posY + 1.2, tirget.posZ - 0.3,
				tirget.posX + 0.3, tirget.posY + 1.8, tirget.posZ + 0.3);
		return head.calculateIntercept(lv0, lvt) != null;
	}

	/** ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外 */
	public List<HitEntity> getHitEntity(Entity owner, World w, Vec3 lv0, Vec3 lvt) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.xCoord, lv0.yCoord, lv0.zCoord, lvt.xCoord, lvt.yCoord, lvt.zCoord)
				.expand(1, 1, 1);
		List<HitEntity> allInterceptEntity = new ArrayList<HitEntity>();
		for (Object e : w.getEntitiesWithinAABBExcludingEntity(owner, aabb)) {
			Entity entity = (Entity) e;
			// 例外なら戻る
			if (entity instanceof EntityBullet || entity instanceof EntityArrow || entity instanceof EntityThrowable
					|| entity.isDead || entity.getEntityBoundingBox() == null||entity==owner) {
				continue;
			}
			// ヒットボックスを取得して

			MovingObjectPosition lmop1 = entity.getEntityBoundingBox().calculateIntercept(lv0, lvt);
			//System.out.println(lmop1+entity.getEntityBoundingBox().expand(0.2, 0.2, 0.2).toString());
			if (lmop1 != null) {
				allInterceptEntity.add(new HitEntity(entity,lmop1.hitVec, lv0.distanceTo(lmop1.hitVec)));
			}
		}
		Collections.sort(allInterceptEntity);
		return allInterceptEntity;
	}
	/**エンティティがベクトルを中線とした範囲の中にいるかどうか*/
	public boolean isInRange(Entity e, Vec3 lv0,Vec3 lvt,float range){

		return false;
	}
}
