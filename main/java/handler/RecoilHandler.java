package handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.guns.GunData;
import types.guns.GunData.GunDataList;
import types.guns.GunRecoil;

@SideOnly(Side.CLIENT)
public class RecoilHandler {

	/**プレイヤーの状態から使用するリコイルを取得*/
	private static GunRecoil getRecoil(EntityPlayer player,GunData data){
		if (player.isSneaking()){
			if(PlayerHandler.isADS){
				return (GunRecoil) data.getDataObject(GunDataList.RECOIL_ADS);
			}else{
				return (GunRecoil) data.getDataObject(GunDataList.RECOIL_DEFAULT);
			}
		}else{
			if(PlayerHandler.isADS){
				return (GunRecoil) data.getDataObject(GunDataList.RECOIL_SNEAK_ADS);
			}else{
				return (GunRecoil) data.getDataObject(GunDataList.RECOIL_SNEAK);
			}
		}
	}

	/**リコイルパワーの増加値を取得*/
	static int getRecoilPowerAdd(EntityPlayer player,GunData data){
		GunRecoil recoil = getRecoil(player, data);
		return recoil.recoilPower_shoot;
	}
	/**リコイルパワーの減少値を取得*/
	static int getRecoilPowerRemove(EntityPlayer player,GunData data){
		GunRecoil recoil = getRecoil(player, data);
		return recoil.recoilPower_tick;
	}
	/**反動を与える*/
	static void MakeRecoil (EntityPlayer player, GunData data,int RecoilPower){
		GunRecoil recoil = getRecoil(player, data);

		player.rotationYaw += getRecoilYaw(recoil,RecoilPower);
		player.rotationPitch -= getRecoilPitch(recoil,RecoilPower);
	}

	static private float getRecoilYaw(GunRecoil data, int RecoilPower){
		float base = data.yaw_base_min+(data.yaw_base_max-data.yaw_base_min/100*RecoilPower);
		float spread = data.yaw_spread_min+((data.yaw_spread_max-data.yaw_spread_min)/100*RecoilPower);
		return (float) normal(base,  spread);
	}

	static private float getRecoilPitch(GunRecoil data, int RecoilPower){
		float base = data.pitch_base_min+(data.pitch_base_max-data.pitch_base_min/100*RecoilPower);
		float spread = data.pitch_spread_min+((data.pitch_spread_max-data.pitch_spread_min)/100*RecoilPower);
		return (float) normal(base,  spread);
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
		//System.out.println("calue : "+ x);
		return (x);
	}
}
