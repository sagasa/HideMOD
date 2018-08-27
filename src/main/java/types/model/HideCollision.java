package types.model;

import net.minecraft.util.Vec3;

public class HideCollision {
	public HideCollisionPoly[] Collision;

	public HideCollision() {
		Collision = new HideCollisionPoly[]{new HideCollisionPoly(new Vec3[]{new Vec3(1, 0, 1),new Vec3(1, 0,-1),new Vec3(-1, 0, -1)})};
	}

	public class HideCollisionPoly{
		public HideCollisionPoly(Vec3[] vertex3) {
			vertex = vertex3;
		}
		public Vec3[] vertex;
	}
}
