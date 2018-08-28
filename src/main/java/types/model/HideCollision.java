package types.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Vec3;
import scala.actors.threadpool.Arrays;

public class HideCollision {
	public HideCollisionPoly[] Collision;
	public float rotatePointX = 0;
	public float rotatePointY = 0;
	public float rotatePointZ = 0;

	public HideCollision() {
		Collision = new HideCollisionPoly[]{new HideCollisionPoly(new Vec3[]{new Vec3(1, 0, 1),new Vec3(1, 0,-1),new Vec3(-1, 0, -1)})};
	}
	/**原点とサイズとスケールからボックスを追加*/
	public void addbox(float x0, float y0, float z0, int x1, int y1, int z1, double scale){
		x0 *= scale;
		y0 *= scale;
		z0 *= scale;
		x1 *= scale;
		y1 *= scale;
		z1 *= scale;
		
		Vec3 ver0 = new Vec3(x0, y0, z0);
		Vec3 ver1 = new Vec3(x0, y0+y1, z0);
		Vec3 ver2 = new Vec3(x0+x1, y0+y1, z0);
		Vec3 ver3 = new Vec3(x0+x1, y0, z0);
		Vec3 ver4 = new Vec3(x0, y0, z0+z1);
		Vec3 ver5 = new Vec3(x0, y0+y1, z0+z1);
		Vec3 ver6 = new Vec3(x0+x1, y0+y1, z0+z1);
		Vec3 ver7 = new Vec3(x0+x1, y0, z0+z1);
		
		List<HideCollisionPoly> list = new ArrayList<HideCollisionPoly>(Arrays.asList(Collision));
		list.add(new HideCollisionPoly(new Vec3[]{ver0,ver1,ver2,ver3}));
		list.add(new HideCollisionPoly(new Vec3[]{ver3,ver2,ver6,ver7}));
		list.add(new HideCollisionPoly(new Vec3[]{ver7,ver6,ver5,ver4}));
		list.add(new HideCollisionPoly(new Vec3[]{ver4,ver5,ver1,ver0}));
		list.add(new HideCollisionPoly(new Vec3[]{ver4,ver0,ver3,ver7}));
		list.add(new HideCollisionPoly(new Vec3[]{ver1,ver5,ver6,ver2}));
	
		Collision = (HideCollisionPoly[]) list.toArray();
	}
	/**回転の基準点を設定*/
	public void setRotatePoint(float x,float y ,float z){
		rotatePointX = x;
		rotatePointY = y;
		rotatePointZ = z;
	}
	
	public class HideCollisionPoly{
		public HideCollisionPoly(Vec3[] vertex) {
			this.vertex = vertex;
		}
		public Vec3[] vertex;
	}
}
