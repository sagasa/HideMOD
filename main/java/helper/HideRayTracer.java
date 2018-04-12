package helper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import entity.EntityBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class HideRayTracer {
	/**比較用の数値とエンティティセットのクラス*/
	class EntitySort implements Comparable{
		double range;
		Entity entity;
		EntitySort(Entity e,double range){
			this.range = range;
			this.entity = e;
		}
		@Override
		public int compareTo(Object o) {
			if (o instanceof EntitySort){
				return (int) Math.ceil(((EntitySort)o).range - this.range);
			}
			return 0;
		}
	}
	/**ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外*/
	public Entity[] getHitEntity(World w, Vec3 lv0, Vec3 lvt) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.xCoord, lv0.yCoord, lv0.zCoord, lvt.xCoord, lvt.yCoord, lvt.zCoord)
				.expand(1, 1, 1);
		Object[] elist = w.getEntitiesWithinAABB(null, aabb).toArray();
		List<EntitySort> allInterceptEntity = new ArrayList<EntitySort>();
		for (Object e : elist) {
			Entity entity = (Entity) e;
			// 例外なら戻る
			if (entity instanceof EntityBullet||entity instanceof EntityArrow||entity instanceof EntityThrowable) {
				continue;
			}
			//ヒットボックスを取得して
			MovingObjectPosition lmop1 = entity.getEntityBoundingBox().calculateIntercept(lv0, lvt);
			if (lmop1 != null) {
				allInterceptEntity.add(new EntitySort(entity,lv0.distanceTo(lmop1.hitVec)));
			}
		}
		EntitySort[] sortVaue = (EntitySort[])allInterceptEntity.toArray();
		Arrays.sort(sortVaue);
		Entity[] result = new Entity[sortVaue.length];
		for(int i =0;i<sortVaue.length;i++){
			result[i] = sortVaue[i].entity;
		}
		return result;
	}
}
