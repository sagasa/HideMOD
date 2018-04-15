package handler;

import net.minecraft.entity.player.EntityPlayer;
import types.GunData;

public class RecoilHandler {
	static void MakeRecoil (EntityPlayer player, GunData data){
		player.rotationYaw += getRecoilYaw();
		player.rotationPitch -= getRecoilPitch();
	}
	static private float getRecoilYaw(){
		return (float) normal(0,  0.4);
	}
	static private float getRecoilPitch(){
		return (float) normal(1,  0.5);
	}

	/**標準偏差*/
	static double normal(double ex, double sd){
		double xw = 0.0;
		double x;
		int n;
		for (n = 1; n <= 12; n ++) {        /* 12個の一様乱数の合計 */
			xw = xw + Math.random();
		}
		x = sd * (xw - 6.0) + ex;
		System.out.println("calue : "+ x);
		return (x);
	}
}
