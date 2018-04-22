package helper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import entity.EntityBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class RayTracer {
	/** 比較用の数値とエンティティセットのクラス */
	class EntitySort implements Comparable {
		double range;
		Entity entity;

		EntitySort(Entity e, double range) {
			this.range = range;
			this.entity = e;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof EntitySort) {
				return (int) Math.ceil(((EntitySort) o).range - this.range);
			}
			return 0;
		}
	}

	/** 部位ダメージ判定 */
	public float getPartDamage(Entity tirget, Vec3 lv0, Vec3 lvt, float damage) {
		// 頭の判定
		AxisAlignedBB head = new AxisAlignedBB(tirget.posX - 0.3, tirget.posY + 1.2, tirget.posZ - 0.3,
				tirget.posX + 0.3, tirget.posY + 1.8, tirget.posZ + 0.3);
		if (head.calculateIntercept(lv0, lvt) != null) {
			System.out.println("HEADSHOT");
			damage *= 2;
		}
		return damage;

	}

	/** ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外 */
	public List<Entity> getHitEntity(Entity owner, World w, Vec3 lv0, Vec3 lvt) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.xCoord, lv0.yCoord, lv0.zCoord, lvt.xCoord, lvt.yCoord, lvt.zCoord)
				.expand(1, 1, 1);
		List<EntitySort> allInterceptEntity = new ArrayList<EntitySort>();
		for (Object e : w.getEntitiesWithinAABBExcludingEntity(owner, aabb)) {
			Entity entity = (Entity) e;
			// 例外なら戻る
			if (entity instanceof EntityBullet || entity instanceof EntityArrow || entity instanceof EntityThrowable
					|| entity.isDead || entity.getEntityBoundingBox() == null||entity==owner) {
				continue;
			}
		//	System.out.println(entity+" "+((EntityBullet)owner).Shooter);
			// ヒットボックスを取得して

			MovingObjectPosition lmop1 = entity.getEntityBoundingBox().calculateIntercept(lv0, lvt);
			//System.out.println(lmop1+entity.getEntityBoundingBox().expand(0.2, 0.2, 0.2).toString());
			if (lmop1 != null) {
				allInterceptEntity.add(new EntitySort(entity, lv0.distanceTo(lmop1.hitVec)));
			}
		}
		Collections.sort(allInterceptEntity);
		List<Entity> result = new ArrayList<Entity>();
		for (EntitySort value : allInterceptEntity) {
			result.add(value.entity);
		}
		return result;
	}
}
