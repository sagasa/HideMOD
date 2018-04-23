package handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.GunData;
import types.GunData.GunDataList;

@SideOnly(Side.CLIENT)
public class RecoilHandler {

	private static final int STAND = 0;
	private static final int STAND_ADS = 1;
	private static final int SNEAK = 2;
	private static final int SNEAK_ADS = 3;

	/**リコイルパワーの増加値を取得*/
	static int getRecoilPowerAdd(EntityPlayer player,GunData data){
		int state = 0;
		/*プレイヤーの状態を取得*/
		if (player.isSneaking()){
			state +=2;
		}

		switch (state) {
		case STAND:
			return data.getDataInt(GunDataList.DEFAULT_RECOILPOWER_SHOOT);
		case STAND_ADS:
			return data.getDataInt(GunDataList.ADS_RECOILPOWER_SHOOT);
		case SNEAK:
			return data.getDataInt(GunDataList.SNEAK_RECOILPOWER_SHOOT);
		case SNEAK_ADS:
			return data.getDataInt(GunDataList.ADS_SNEAK_RECOILPOWER_SHOOT);
		}
		return 0;
	}
	/**リコイルパワーの減少値を取得*/
	static int getRecoilPowerRemove(EntityPlayer player,GunData data){
		int state = 0;
		/*プレイヤーの状態を取得*/
		if (player.isSneaking()){
			state +=2;
		}

		switch (state) {
		case STAND:
			return data.getDataInt(GunDataList.DEFAULT_RECOILPOWER_TICK);
		case STAND_ADS:
			return data.getDataInt(GunDataList.ADS_RECOILPOWER_TICK);
		case SNEAK:
			return data.getDataInt(GunDataList.SNEAK_RECOILPOWER_TICK);
		case SNEAK_ADS:
			return data.getDataInt(GunDataList.ADS_SNEAK_RECOILPOWER_TICK);
		}
		return 0;
	}
	/**反動を与える*/
	static void MakeRecoil (EntityPlayer player, GunData data,int RecoilPower){
		int state = 0;
		/*プレイヤーの状態を取得*/
		if (player.isSneaking()){
			state +=2;
		}

		player.rotationYaw += getRecoilYaw(data,RecoilPower,state);
		player.rotationPitch -= getRecoilPitch(data,RecoilPower,state);
	}
	static private float getRecoilYaw(GunData data, int RecoilPower ,int state){
		float max_base = 0;
		float max_spread = 0;
		float min_base = 0;
		float min_spread = 0;
		switch (state) {
		case STAND:
			max_base = data.getDataFloat(GunDataList.MAX_YAW_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_YAW_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_YAW_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_YAW_RECOIL_SPREAD);
			break;
		case STAND_ADS:
			max_base = data.getDataFloat(GunDataList.MAX_ADS_YAW_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_ADS_YAW_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_ADS_YAW_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_ADS_YAW_RECOIL_SPREAD);
			break;
		case SNEAK:
			max_base = data.getDataFloat(GunDataList.MAX_SNEAK_YAW_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_SNEAK_YAW_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_SNEAK_YAW_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_SNEAK_YAW_RECOIL_SPREAD);
			break;
		case SNEAK_ADS:
			max_base = data.getDataFloat(GunDataList.MAX_ADS_SNEAK_YAW_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_ADS_SNEAK_YAW_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_ADS_SNEAK_YAW_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_ADS_SNEAK_YAW_RECOIL_SPREAD);
			break;
		}
		float base = min_base+(max_base-min_base/100*RecoilPower);
		float spread = min_spread+((max_spread-min_spread)/100*RecoilPower);
		return (float) normal(base,  spread);
	}
	static private float getRecoilPitch(GunData data, int RecoilPower,int state){
		float max_base = 0;
		float max_spread = 0;
		float min_base = 0;
		float min_spread = 0;
		switch (state) {
		case STAND:
			max_base = data.getDataFloat(GunDataList.MAX_PITCH_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_PITCH_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_PITCH_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_PITCH_RECOIL_SPREAD);
			break;
		case STAND_ADS:
			max_base = data.getDataFloat(GunDataList.MAX_ADS_PITCH_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_ADS_PITCH_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_ADS_PITCH_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_ADS_PITCH_RECOIL_SPREAD);
			break;
		case SNEAK:
			max_base = data.getDataFloat(GunDataList.MAX_SNEAK_PITCH_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_SNEAK_PITCH_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_SNEAK_PITCH_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_SNEAK_PITCH_RECOIL_SPREAD);
			break;
		case SNEAK_ADS:
			max_base = data.getDataFloat(GunDataList.MAX_ADS_SNEAK_PITCH_RECOIL_BASE);
			max_spread = data.getDataFloat(GunDataList.MAX_ADS_SNEAK_PITCH_RECOIL_SPREAD);
			min_base = data.getDataFloat(GunDataList.MIN_ADS_SNEAK_PITCH_RECOIL_BASE);
			min_spread = data.getDataFloat(GunDataList.MIN_ADS_SNEAK_PITCH_RECOIL_SPREAD);
			break;
		}
		float base = min_base+(max_base-min_base/100*RecoilPower);
		float spread = min_spread+((max_spread-min_spread)/100*RecoilPower);
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
