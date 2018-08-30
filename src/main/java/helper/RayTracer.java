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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import types.model.HideCollision;
import types.model.HideCollision.HideCollisionPoly;

public class RayTracer {
	/** 比較用の数値とベクトルのクラス */
	public class Hit extends RayTraceResult implements Comparable {
		public double range;

		public Hit(Entity e, Vec3d hitvec, double range) {
			super(e, hitvec);
			this.range = range;
		}
		public Hit(Vec3d hitvec, double range) {
			super(null, hitvec);
			this.typeOfHit = Type.MISS;
			this.range = range;
		}
		public Hit(BlockPos block, Vec3d hetvec) {
			super(hetvec, null, block);
			this.hitVec = hetvec;
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
	public Hit[] getHitBlock(Entity owner, World w, Vec3d lv0, Vec3d lvt){
		ArrayList<Hit> hitBlocks = new ArrayList<Hit>();

		Vec3d vm = lvt.subtract(lv0).normalize();
		RayTraceResult lmop1 = w.rayTraceBlocks(lv0, lvt);
		Vec3d lvm = lmop1!=null?lmop1.hitVec:lv0;
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
	public boolean isHeadShot(Entity tirget, Vec3d lv0, Vec3d lvt) {
		// 頭の判定
		AxisAlignedBB head = new AxisAlignedBB(tirget.posX - 0.3, tirget.posY + 1.2, tirget.posZ - 0.3,
				tirget.posX + 0.3, tirget.posY + 1.8, tirget.posZ + 0.3);
		return head.calculateIntercept(lv0, lvt) != null;
	}

	/** ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外 */
	public List<Hit> getHitEntity(Entity owner, World w, Vec3d lv0, Vec3d lvt) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.x, lv0.y, lv0.z, lvt.x, lvt.y, lvt.z)
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

			RayTraceResult lmop1 = entity.getEntityBoundingBox().calculateIntercept(lv0, lvt);
			//System.out.println(lmop1+entity.getEntityBoundingBox().expand(0.2, 0.2, 0.2).toString());
			if (lmop1 != null) {
				allInterceptEntity.add(new Hit(entity,lmop1.hitVec, lv0.distanceTo(lmop1.hitVec)));
			}
		}
		Collections.sort(allInterceptEntity);
		return allInterceptEntity;
	}

	/**エンティティがベクトルを中線とした範囲の中にいるかどうか*/
	public boolean isInRange(Entity e, Vec3d lv0,Vec3d lvt,float range){

		return false;
	}

	/**コリジョンとベクトルが接触するか*/
	public Hit getHit(HideCollision collision,Vec3d lv0,Vec3d lvt){
		List<Hit> hits = new ArrayList<Hit>();
		for(HideCollisionPoly poly:collision.Collision){
			hits.add(getHit(poly, lv0, lvt));
		}
		Collections.sort(hits);

		return hits.iterator().next();
	}
	private Hit getHit(HideCollisionPoly collision,Vec3d lv0,Vec3d lvt){
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
	private Hit getHit(Vec3d v0,Vec3d v1,Vec3d v2,Vec3d lv0,Vec3d lvt){
		Vec3d nomal = lvt.normalize();
		Vec3d invRay = nomal.scale(-1);
		Vec3d edge1 = v1.subtract(v0);
		Vec3d edge2 = v2.subtract(v0);

		float det = getDet(edge1, edge2,invRay);
		if (det <= 0) {
			return null;
		}

		Vec3d d = lv0.subtract(v0);

		float u = getDet(d, edge2, invRay) / det;
		if ((u >= 0) && (u <= 1)) {
			float v = getDet(edge1, d, invRay) / det;
			if ((v >= 0) && (v <= 1)) {
				float t = getDet(edge1, edge2, d) / det;

				// 距離がマイナスの場合は交差していない
	            if (t < 0||lvt.distanceTo(lv0)<t) {
	                return null;
	            }
	            return new Hit(nomal.scale(t).add(lv0), t);
			}
		}
		return null;
	}

	/**ベクトル3つのdetを取得*/
	private static float getDet(Vec3d vec0, Vec3d vec1, Vec3d vec2) {
		return (float) ((vec0.x * vec1.y * vec2.z) + (vec0.y * vec1.z * vec2.x)
				+ (vec0.z * vec1.x * vec2.y) - (vec0.x * vec1.z * vec2.y)
				- (vec0.y * vec1.x * vec2.z) - (vec0.z * vec1.y * vec2.x));
	}
}
