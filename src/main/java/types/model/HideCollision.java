package types.model;

import net.minecraft.util.math.Vec3d;

public class HideCollision {
	public HideCollisionPoly[] Collision;

	public HideCollision() {
		Collision = new HideCollisionPoly[]{new HideCollisionPoly(new Vec3d[]{new Vec3d(1, 0, 1),new Vec3d(1, 0,-1),new Vec3d(-1, 0, -1)})};
	}

	public class HideCollisionPoly{
		public HideCollisionPoly(Vec3d[] vertex3) {
			vertex = vertex3;
		}
		public Vec3d[] vertex;
	}
}
