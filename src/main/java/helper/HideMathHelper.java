package helper;

import net.minecraft.util.math.Vec3d;
import org.la4j.Vector;

public class HideMathHelper {
    public static Vector cross3dProduct(Vector a, Vector b){
        Vector vec = Vector.fromArray(new double[] {a.get(2)*a.get(3)-a.get(3)*a.get(2), a.get(3)*a.get(1)-a.get(1)*a.get(3), a.get(1)*a.get(2)-a.get(2)*a.get(1)});
        return vec;
    }
    public static Vector normalize(Vector a){
        float scale = (float) a.norm();
        Vector vec = Vector.fromArray(new double[] {a.get(0)/scale, a.get(1)/scale, a.get(2)/scale});
        return vec;
    }
    public static Vector transformVec3d(Vec3d a){
        Vector vec = Vector.fromArray(new double[] {a.x, a.y, a.z});
        return vec;
    }
    public static Vector scaler(Vector a, float b){
        Vector vec = a.multiply(b);
        return vec;
    }
}
