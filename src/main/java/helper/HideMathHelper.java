package helper;

import net.minecraft.util.math.Vec3d;

public class HideMathHelper {
    public static Vec3d cross3dProduct(Vec3d a, Vec3d b){
        Vec3d vec = new Vec3d(a.y*b.z-a.z*b.y, a.z*b.x-a.x*b.z, a.x*b.y-a.y*b.x);
        return vec;
    }
    public static Vec3d normalize(Vec3d a){
        float scale = (float) a.lengthVector();
        Vec3d vec = new Vec3d(a.x/scale, a.y/scale, a.z/scale);
        return vec;
    }
    public static float getDistance(Vec3d a, Vec3d b){
        return (float) Math.sqrt(Math.pow(a.x-b.x, 2) + Math.pow(a.y-b.y, 2) + Math.pow(a.z-b.z, 2));
    }
    public static float innerProduct3d(Vec3d a, Vec3d b) {
        return (float) (a.x*b.x+a.y*b.y+a.z*b.z);
    }
}
