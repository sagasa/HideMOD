package helper;

import net.minecraft.util.Vec3;

public class VecHelper {
	public static Vec3 multiplyScalar(Vec3 vec,float d){
		return new Vec3(vec.xCoord*d, vec.yCoord*d, vec.zCoord*d);
	}
}
