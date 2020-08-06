package helper;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

public class HideCollisionDetector {

    float soutaiItiX = 0, soutaiItiY = 0, soutaiItiZ = 0, soutaiYaw = 0, soutaiPitch = 0;
    Vec3d centerPos;

    List<Vec3d> collisionVec = new ArrayList<>();

    float modelRange = 100F;

    public void HideCollisionDetector(List<Vec3d> model, Vec3d center){
        collisionVec = model;
        centerPos = center;
    }
    public void isHit(List<RayTracer.Hit> list, Vec3d startv, Vec3d endv){
        for(int n=0; n<list.size()/3; n++){
            //float range = (float) collisionVec.get(n).distanceTo(center);
            if(isInCenter(n)){
                //rayのベクトルを取得
                Vector ray = HideMathHelper.transformVec3d(endv.subtract(startv));
                //コリジョンの2辺のベクトルを取得
                Vector vecA = Vector.fromArray(new double[] {collisionVec.get(3*(n+1)).x - collisionVec.get(3*n).x, collisionVec.get(3*(n+1)).y - collisionVec.get(3*n).y, collisionVec.get(3*(n+1)).z - collisionVec.get(3*n).z});
                Vector vecB = Vector.fromArray(new double[] {collisionVec.get(3*(n+2)).x - collisionVec.get(3*n).x, collisionVec.get(3*(n+2)).y - collisionVec.get(3*n).y, collisionVec.get(3*(n+2)).z - collisionVec.get(3*n).z});
                //これはray
                Matrix rayPosVec = Matrix.from2DArray(new double[][] {{ray.get(0), ray.get(1), ray.get(2)}}).transpose();
                //vecAとvecBを行列化したやつの逆行列取得
                Matrix ABi = Matrix.from2DArray(new double[][] {{vecA.get(0),vecB.get(0)},{vecA.get(1),vecB.get(1)},{vecA.get(2),vecB.get(2)}}).withInverter(LinearAlgebra.INVERTER).inverse();
                //vecAとvecBを基底としたときの成分が正かつ足して1以下なら衝突
                Matrix R = ABi.multiply(rayPosVec);
                if(R.get(0,0) > 0 && R.get(0,1) > 0 && R.get(0,0)+R.get(0,1) < 1){
                    //AとBの内積を取得
                    Vector normalVec = HideMathHelper.cross3dProduct(vecA, vecB);
                    //単位ベクトル化
                    Vector unitVec = HideMathHelper.normalize(normalVec);
                    //始点の位置ベクトルを取得
                    Vector startVec = HideMathHelper.transformVec3d(startv);
                    if(ray.innerProduct(unitVec) != 0){
                        //コリジョンに届くrayを生成
                        float t = (float) (-startVec.innerProduct(unitVec)/ray.innerProduct(unitVec));
                        //交点の座標を取得
                        Vector crossingVec = HideMathHelper.scaler(ray, t).add(HideMathHelper.transformVec3d(startv));
                    }
                }
            }
        }
    }
    private boolean isInCenter(int m){
        return collisionVec.get(3 * m).distanceTo(centerPos) < modelRange && collisionVec.get(3 * (m + 1)).distanceTo(centerPos) < modelRange && collisionVec.get(3 * (m + 2)).distanceTo(centerPos) < modelRange;
    }
}
