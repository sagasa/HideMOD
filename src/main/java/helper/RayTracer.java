package helper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
import types.model.HideCollision;
import types.model.HideCollision.HideCollisionPoly;

public class RayTracer {
	/** 比較用の数値とベクトルのクラス */
	public class Hit implements Comparable {
		public double range;
		public Vec3 hitVec;
		public Entity entity;
		public BlockPos blockPos;

		public Hit(Entity e, Vec3 hitvec, double range) {
			this.range = range;
			this.entity = e;
			this.hitVec = hitvec;
		}
		public Hit(Vec3 hitvec, double range) {
			this.range = range;
			this.hitVec = hitvec;
		}
		public Hit(BlockPos block, Vec3 hetvec) {
			this.hitVec = hetvec;
			this.blockPos = block;
		}

		@Override
		public String toString() {
			return super.toString()+"[HitVec:"+hitVec+",Range:"+range+"]";
		}
		@Override
		public int compareTo(Object o) {
			if (o instanceof Hit) {
				return (int) Math.ceil(this.range-((Hit) o).range);
			}
			return 0;
		}
	}

	/** 当たったブロックを取得*/
	public Hit[] getHitBlock(Entity owner, World w, Vec3 lv0, Vec3 lvt){
		ArrayList<Hit> hitBlocks = new ArrayList<Hit>();

		Vec3 vm = lvt.subtract(lv0).normalize();
		MovingObjectPosition lmop1 = w.rayTraceBlocks(lv0, lvt);
		Vec3 lvm = lmop1!=null?lmop1.hitVec:lv0;
		for (;lmop1!=null&&lvm.distanceTo(lv0)<lvt.distanceTo(lv0);lmop1 = w.rayTraceBlocks(lvm, lvt)){
			//hitBlocks.add(arg0)
			Hit Hit = new Hit(lmop1.getBlockPos(),lmop1.hitVec);
			if(!hitBlocks.contains(Hit)){
				hitBlocks.add(Hit);
			}
			lvm = lvm.add(vm);
		}
		return hitBlocks.toArray(new Hit[hitBlocks.size()]);
	}

	/** 部位ダメージ判定 */
	public boolean isHeadShot(Entity tirget, Vec3 lv0, Vec3 lvt) {
		// 頭の判定
		AxisAlignedBB head = new AxisAlignedBB(tirget.posX - 0.3, tirget.posY + 1.2, tirget.posZ - 0.3,
				tirget.posX + 0.3, tirget.posY + 1.8, tirget.posZ + 0.3);
		return head.calculateIntercept(lv0, lvt) != null;
	}

	/** ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外 */
	public List<Hit> getHitEntity(Entity owner, World w, Vec3 lv0, Vec3 lvt) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.xCoord, lv0.yCoord, lv0.zCoord, lvt.xCoord, lvt.yCoord, lvt.zCoord)
				.expand(1, 1, 1);
		List<Hit> allInterceptEntity = new ArrayList<Hit>();
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
				allInterceptEntity.add(new Hit(entity,lmop1.hitVec, lv0.distanceTo(lmop1.hitVec)));
			}
		}
		Collections.sort(allInterceptEntity);
		return allInterceptEntity;
	}

	/**エンティティがベクトルを中線とした範囲の中にいるかどうか*/
	public boolean isInRange(Entity e, Vec3 lv0,Vec3 lvt,float range){

		return false;
	}

	/**コリジョンとベクトルが接触するか*/
	public Hit getHit(HideCollision collision,Vec3 lv0,Vec3 lvt){
		List<Hit> hits = new ArrayList<Hit>();
		for(HideCollisionPoly poly:collision.Collision){
			hits.add(getHit(poly, lv0, lvt));
		}
		Collections.sort(hits);

		return hits.iterator().next();
	}
	private Hit getHit(HideCollisionPoly collision,Vec3 lv0,Vec3 lvt){
		if(collision.vertex.length <3){
			return null;
		}
		for (int i = 0; i < collision.vertex.length-2; i++) {
			Hit hit = getHit(collision.vertex[0], collision.vertex[i+1], collision.vertex[i+2], lv0, lvt);
			if(hit!=null){
				return hit;
			}
		}
		return null;
	}
	private Hit getHit(Vec3 v0,Vec3 v1,Vec3 v2,Vec3 lv0,Vec3 lvt){
		Vec3 nomal = lvt.normalize();
		Vec3 invRay = VecHelper.multiplyScalar(nomal, -1);
		Vec3 edge1 = v1.subtract(v0);
		Vec3 edge2 = v2.subtract(v0);

		float det = getDet(edge1, edge2,invRay);
		if (det <= 0) {
			return null;
		}

		Vec3 d = lv0.subtract(v0);

		float u = getDet(d, edge2, invRay) / det;
		if ((u >= 0) && (u <= 1)) {
			float v = getDet(edge1, d, invRay) / det;
			if ((v >= 0) && (v <= 1)) {
				float t = getDet(edge1, edge2, d) / det;

				// 距離がマイナスの場合は交差していない
	            if (t < 0||lvt.distanceTo(lv0)<t) {
	                return null;
	            }
	            return new Hit(VecHelper.multiplyScalar(nomal,t).add(lv0), t);
			}
		}
		return null;
	}

	/**ベクトル3つのdetを取得*/
	private static float getDet(Vec3 vec0, Vec3 vec1, Vec3 vec2) {
		return (float) ((vec0.xCoord * vec1.yCoord * vec2.zCoord) + (vec0.yCoord * vec1.zCoord * vec2.xCoord)
				+ (vec0.zCoord * vec1.xCoord * vec2.yCoord) - (vec0.xCoord * vec1.zCoord * vec2.yCoord)
				- (vec0.yCoord * vec1.xCoord * vec2.zCoord) - (vec0.zCoord * vec1.yCoord * vec2.xCoord));
	}
}
