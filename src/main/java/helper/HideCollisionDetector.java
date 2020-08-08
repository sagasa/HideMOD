package helper;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import org.la4j.LinearAlgebra;
import org.la4j.Matrix;
import org.la4j.Vector;

public class HideCollisionDetector {

    float soutaiItiX = 0, soutaiItiY = 0, soutaiItiZ = 0, soutaiYaw = 0, soutaiPitch = 0;

    List<Vec3d> collisionVec = new ArrayList<>();

    HideCollisionDetector(List<Vec3d> model){
        collisionVec = model;
    }
    public void isHit(List<RayTracer.Hit> list, Vec3d startv, Vec3d endv){
        for(int n=0; n<collisionVec.size()/3; n++){
            //rayのベクトルを取得
            Vec3d ray = endv.subtract(startv);
            //コリジョンの2辺のベクトルを取得
            Vec3d vecA = new Vec3d(collisionVec.get(3*n+1).x - collisionVec.get(3*n).x, collisionVec.get(3*n+1).y - collisionVec.get(3*n).y, collisionVec.get(3*n+1).z - collisionVec.get(3*n).z);
            Vec3d vecB = new Vec3d(collisionVec.get(3*n+2).x - collisionVec.get(3*n).x, collisionVec.get(3*n+2).y - collisionVec.get(3*n).y, collisionVec.get(3*n+2).z - collisionVec.get(3*n).z);
            //AとBの外積を取得
            Vec3d normalVec = HideMathHelper.cross3dProduct(vecA, vecB);
            //単位ベクトル化
            Vec3d unitVec = HideMathHelper.normalize(normalVec);
            Vec3d origin = new Vec3d(collisionVec.get(3*n).x,collisionVec.get(3*n).y,collisionVec.get(3*n).z);
            if(HideMathHelper.innerProduct3d(ray,unitVec) != 0 && canIntersect(startv, endv, normalVec, origin)){
                //コリジョンに届くrayを生成
                float t = (float) (-HideMathHelper.innerProduct3d(startv,unitVec)/HideMathHelper.innerProduct3d(ray,unitVec));
                if(t>=0 && t<=1) {
                    //交点の座標を取得
                    Vec3d crossingVec = ray.scale(t).add(startv);
                    Matrix rayPosVec;
                    Matrix ABi;
                    if(Matrix.from2DArray(new double[][] {{vecA.x,vecB.x},{vecA.y,vecB.y}}).determinant() != 0){
                        rayPosVec = Matrix.from2DArray(new double[][] {{crossingVec.x-collisionVec.get(3*n).x, crossingVec.y-collisionVec.get(3*n).y}}).transpose();
                        ABi = Matrix.from2DArray(new double[][] {{vecA.x,vecB.x},{vecA.y,vecB.y}}).withInverter(LinearAlgebra.INVERTER).inverse();
                    }else if(Matrix.from2DArray(new double[][] {{vecA.y,vecB.y},{vecA.z,vecB.z}}).determinant() != 0){
                        rayPosVec = Matrix.from2DArray(new double[][] {{crossingVec.y-collisionVec.get(3*n).y, crossingVec.z-collisionVec.get(3*n).z}}).transpose();
                        ABi = Matrix.from2DArray(new double[][] {{vecA.y,vecB.y},{vecA.z,vecB.z}}).withInverter(LinearAlgebra.INVERTER).inverse();
                    }else if(Matrix.from2DArray(new double[][] {{vecA.z,vecB.z},{vecA.x,vecB.x}}).determinant() != 0){
                        rayPosVec = Matrix.from2DArray(new double[][] {{crossingVec.z-collisionVec.get(3*n).z, crossingVec.x-collisionVec.get(3*n).x}}).transpose();
                        ABi = Matrix.from2DArray(new double[][] {{vecA.z,vecB.z},{vecA.x,vecB.x}}).withInverter(LinearAlgebra.INVERTER).inverse();
                    }else{ continue; }
                    //vecAとvecBを基底としたときの成分が正かつ足して1以下なら衝突
                    //Vec3d R = ABi.multiply(rayPosVec).getColumn(0);
                    Vector R = ABi.multiply(rayPosVec).getColumn(0);
                    if(R.get(0) >= 0 && R.get(1) >= 0 && R.get(0)+R.get(1) < 1){
                        System.out.println("GetCrossingVector! "+crossingVec);
                        break;
                    }
                }
            }
        }
    }
    
    public void isProximity(List<RayTracer.Hit> list, Vec3d startv, Vec3d endv, Vec3d center, float radius){
        if(HideMathHelper.getDistance(startv,center) <= radius && HideMathHelper.getDistance(endv, center) <= radius){
            //do something
        }
    }

    private boolean canIntersect(Vec3d startv, Vec3d endv, Vec3d normal, Vec3d origin){
        return (HideMathHelper.innerProduct3d(normal,startv)-HideMathHelper.innerProduct3d(normal,origin))*(HideMathHelper.innerProduct3d(normal,endv)-HideMathHelper.innerProduct3d(normal,origin))<= 0;
    }
}
