package helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import entity.EntityBullet;
import entity.EntityDebugAABB;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import model.ModelPart;
import model.ModelPart.HidePolygon;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RayTracer {
	/** 比較用の数値とベクトルのクラス */
	public static class Hit extends RayTraceResult implements Comparable {
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
			return super.toString() + "[HitVec:" + hitVec + ",Range:" + range + "]";
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof Hit) {
				return (int) Math.ceil(this.range - ((Hit) o).range);
			}
			return 0;
		}
	}

	/**補完*/
	public static float Comp = 2;
	public static boolean debug = false;

	/** 当たったブロックを取得
	 * 欠陥 距離0の時にブロックが取得できない */
	public RayTraceResult[] getHitBlocks(World world, Vec3d lv0, Vec3d lvt) {
		// 計算不能なら空リストを返す
		if (Double.isNaN(lv0.x) || Double.isNaN(lv0.y) || Double.isNaN(lv0.z) || Double.isNaN(lvt.x)
				|| Double.isNaN(lvt.y) || Double.isNaN(lvt.z)) {
			return new RayTraceResult[0];
		}
		ArrayList<RayTraceResult> hitBlocks = new ArrayList<>();
		int i = MathHelper.floor(lvt.x);
		int j = MathHelper.floor(lvt.y);
		int k = MathHelper.floor(lvt.z);
		int l = MathHelper.floor(lv0.x);
		int i1 = MathHelper.floor(lv0.y);
		int j1 = MathHelper.floor(lv0.z);
		BlockPos blockpos = new BlockPos(l, i1, j1);

		int k1 = 200;

		while (k1-- >= 0) {
			if (Double.isNaN(lv0.x) || Double.isNaN(lv0.y) || Double.isNaN(lv0.z)) {
				return hitBlocks.toArray(new RayTraceResult[hitBlocks.size()]);
			}

			if (l == i && i1 == j && j1 == k) {
				return hitBlocks.toArray(new RayTraceResult[hitBlocks.size()]);
			}

			boolean flag2 = true;
			boolean flag = true;
			boolean flag1 = true;
			double d0 = 999.0D;
			double d1 = 999.0D;
			double d2 = 999.0D;

			if (i > l) {
				d0 = l + 1.0D;
			} else if (i < l) {
				d0 = l + 0.0D;
			} else {
				flag2 = false;
			}

			if (j > i1) {
				d1 = i1 + 1.0D;
			} else if (j < i1) {
				d1 = i1 + 0.0D;
			} else {
				flag = false;
			}

			if (k > j1) {
				d2 = j1 + 1.0D;
			} else if (k < j1) {
				d2 = j1 + 0.0D;
			} else {
				flag1 = false;
			}

			double d3 = 999.0D;
			double d4 = 999.0D;
			double d5 = 999.0D;
			double d6 = lvt.x - lv0.x;
			double d7 = lvt.y - lv0.y;
			double d8 = lvt.z - lv0.z;

			if (flag2) {
				d3 = (d0 - lv0.x) / d6;
			}

			if (flag) {
				d4 = (d1 - lv0.y) / d7;
			}

			if (flag1) {
				d5 = (d2 - lv0.z) / d8;
			}

			if (d3 == -0.0D) {
				d3 = -1.0E-4D;
			}

			if (d4 == -0.0D) {
				d4 = -1.0E-4D;
			}

			if (d5 == -0.0D) {
				d5 = -1.0E-4D;
			}

			EnumFacing enumfacing;

			if (d3 < d4 && d3 < d5) {
				enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
				lv0 = new Vec3d(d0, lv0.y + d7 * d3, lv0.z + d8 * d3);
			} else if (d4 < d5) {
				enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
				lv0 = new Vec3d(lv0.x + d6 * d4, d1, lv0.z + d8 * d4);
			} else {
				enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
				lv0 = new Vec3d(lv0.x + d6 * d5, lv0.y + d7 * d5, d2);
			}

			l = MathHelper.floor(lv0.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
			i1 = MathHelper.floor(lv0.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
			j1 = MathHelper.floor(lv0.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
			blockpos = new BlockPos(l, i1, j1);
			IBlockState iblockstate1 = world.getBlockState(blockpos);
			Block block1 = iblockstate1.getBlock();

			if (block1.canCollideCheck(iblockstate1, false)) {
				RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(world, blockpos, lv0, lvt);
				if (raytraceresult1 != null) {
					hitBlocks.add(raytraceresult1);
				}
			}
		}
		return hitBlocks.toArray(new RayTraceResult[hitBlocks.size()]);
	}

	/** 部位ダメージ判定 */
	public boolean isHeadShot(Entity tirget, Vec3d lv0, Vec3d lvt, float offset) {
		// 頭の判定
		AxisAlignedBB head = new AxisAlignedBB(tirget.posX - 0.3, tirget.posY + 1.2, tirget.posZ - 0.3,
				tirget.posX + 0.3, tirget.posY + 1.8, tirget.posZ + 0.3).offset(getOffsetVec(tirget, offset));
		return head.calculateIntercept(lv0, lvt) != null;
	}

	/**補完用位置*/
	private Vec3d getOffsetVec(Entity entity, float offsetTick) {
		// 補完用速度
		double x, y, z;
		if (entity instanceof EntityPlayerMP) {
			ServerPlayerData data = HidePlayerData.getServerData((EntityPlayerMP) entity);
			System.out.println("offset " + offsetTick + " comp = " + data.Comp.getCompVec(offsetTick).subtract(entity.getPositionVector()));
			return data.Comp.getCompVec(offsetTick).subtract(entity.getPositionVector());
		}
		x = (entity.posX - entity.lastTickPosX) * -offsetTick;
		y = (entity.posY - entity.lastTickPosY) * -offsetTick;
		z = (entity.posZ - entity.lastTickPosZ) * -offsetTick;
		return new Vec3d(x, y, z);
	}

	/**エンティティの最大補完距離*/
	private static final double ExpandSize = 3;

	/** ベクトルに触れたエンティティを返す EntityBulletと雪玉と矢は例外 */
	public List<Hit> getHitEntity(Entity owner, World w, Vec3d lv0, Vec3d lvt, final float offset) {
		AxisAlignedBB aabb = new AxisAlignedBB(lv0.x, lv0.y, lv0.z, lvt.x, lvt.y, lvt.z).expand(ExpandSize, ExpandSize, ExpandSize).expand(-ExpandSize, -ExpandSize, -ExpandSize);
		List<Hit> allInterceptEntity = new ArrayList<>();
		for (Object e : w.getEntitiesWithinAABBExcludingEntity(owner, aabb)) {
			Entity entity = (Entity) e;
			// 例外なら戻る
			if (entity instanceof EntityBullet || entity instanceof EntityDebugAABB || entity instanceof EntityArrow || entity instanceof EntityThrowable
					|| entity.isDead || entity.getEntityBoundingBox() == null || entity == owner) {
				continue;
			}
			// ヒットボックスを取得して
			AxisAlignedBB entityAABB = entity.getEntityBoundingBox();
			if (offset != 0) {
				if (debug)
					w.spawnEntity(new EntityDebugAABB(w, entityAABB, 0.2f, 1, 0.2f));

				//w.spawnEntity(new EntityDebugAABB(w, entityAABB.offset(new Vec3d(-x, -y, -z)), 0.2f, 0.2f, 1));
				Vec3d off = getOffsetVec(entity, offset);
				entityAABB = entityAABB.offset(off);
				if (debug) {
					System.out.println(offset + " " + off);
					w.spawnEntity(new EntityDebugAABB(w, entityAABB, 0.2f, 0.2f, 1));
				}
			}
			RayTraceResult lmop1 = entityAABB.calculateIntercept(lv0, lvt);
			// System.out.println(lmop1+entity.getEntityBoundingBox().expand(0.2, 0.2,
			// 0.2).toString());
			if (lmop1 != null) {
				allInterceptEntity.add(new Hit(entity, lmop1.hitVec, lv0.distanceTo(lmop1.hitVec)));
			}
		}
		Collections.sort(allInterceptEntity);
		return allInterceptEntity;
	}

	/** エンティティがベクトルを中線とした範囲の中にいるかどうか */
	public boolean isInRange(Entity e, Vec3d lv0, Vec3d lvt, float range) {

		return false;
	}

	/** コリジョンとベクトルが接触するか */
	public Hit getHit(ModelPart collision, Vec3d lv0, Vec3d lvt) {
		List<Hit> hits = new ArrayList<>();
		for (HidePolygon poly : collision.Polygon) {
			hits.add(getHit(poly, lv0, lvt));
		}
		Collections.sort(hits);

		return hits.iterator().next();
	}

	private Hit getHit(HidePolygon collision, Vec3d lv0, Vec3d lvt) {
		if (collision.Vertex.length < 3) {
			return null;
		}
		for (int i = 0; i < collision.Vertex.length - 2; i++) {
			Hit hit = getHit(collision.Vertex[0], collision.Vertex[i + 1], collision.Vertex[i + 2], lv0, lvt);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private Hit getHit(Vec3d v0, Vec3d v1, Vec3d v2, Vec3d lv0, Vec3d lvt) {
		Vec3d nomal = lvt.normalize();
		Vec3d invRay = nomal.scale(-1);
		Vec3d edge1 = v1.subtract(v0);
		Vec3d edge2 = v2.subtract(v0);

		float det = getDet(edge1, edge2, invRay);
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
				if (t < 0 || lvt.distanceTo(lv0) < t) {
					return null;
				}
				return new Hit(nomal.scale(t).add(lv0), t);
			}
		}
		return null;
	}

	/** ベクトル3つのdetを取得 */
	private static float getDet(Vec3d vec0, Vec3d vec1, Vec3d vec2) {
		return (float) ((vec0.x * vec1.y * vec2.z) + (vec0.y * vec1.z * vec2.x) + (vec0.z * vec1.x * vec2.y)
				- (vec0.x * vec1.z * vec2.y) - (vec0.y * vec1.x * vec2.z) - (vec0.z * vec1.y * vec2.x));
	}
}
