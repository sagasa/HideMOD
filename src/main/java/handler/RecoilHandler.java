package handler;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import helper.HideMath;
import types.guns.GunData;
import types.guns.GunData.GunDataList;
import types.guns.GunRecoil;

@SideOnly(Side.CLIENT)
public class RecoilHandler {
	private static int recoilPower = 0;

	private static float yawReturnTo = 0;
	private static float pitchReturnTo = 0;
	private static int yawReturnTick = -1;
	private static int pitchReturnTick = -1;

	private static float yawShakeTo = 0;
	private static float pitchShakeTo = 0;
	private static float yawShakeTick = -1;
	private static float pitchShakeTick = -1;

	/** プレイヤーの状態から使用するリコイルを取得 */
	private static GunRecoil getRecoil(GunData data) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		return getRecoil(data, player.isSneaking(), PlayerHandler.isADS);
	}
	//状態から取得 使えなかった場合前を参照
	private static GunRecoil getRecoil(GunData data,boolean isSneak,boolean isADS){
		if (!isSneak) {
			if (isADS) {
				GunRecoil recoil = (GunRecoil) data.getDataObject(GunDataList.RECOIL_ADS);
				if(recoil.use){
					return recoil;
				}
				return getRecoil(data, false, false);
			} else {
				GunRecoil recoil = (GunRecoil) data.getDataObject(GunDataList.RECOIL_DEFAULT);
				if(recoil.use){
					return recoil;
				}
				return new GunRecoil();
			}
		} else {
			if (isADS) {
				GunRecoil recoil = (GunRecoil) data.getDataObject(GunDataList.RECOIL_SNEAK_ADS);
				if(recoil.use){
					return recoil;
				}
			} else {
				GunRecoil recoil = (GunRecoil) data.getDataObject(GunDataList.RECOIL_SNEAK);
				if(recoil.use){
					return recoil;
				}
			}
			return getRecoil(data, false, isADS);
		}
	}

	/** 反動を与える */
	public static void addRecoil(GunData data) {
		GunRecoil recoil = getRecoil(data);
		//リコイル戻し
		yawReturnTo = getYawReturn(recoil);
		pitchReturnTo = getPitchReturn(recoil);

		//リコイル
		yawShakeTo += getYawShake(recoil,yawReturnTo);
		yawShakeTick = recoil.yaw_recoil_tick;

		pitchShakeTo += getPitchShake(recoil,pitchReturnTo);
		pitchShakeTick = recoil.pitch_recoil_tick;
		//リコイルパワー加算
		recoilPower = recoilPower+getRecoil(data).recoilPower_shoot>100 ? 100 :recoilPower+getRecoil(data).recoilPower_shoot;
	}

	/**Tick毎の変化*/
	static public void updateRecoil(GunData data){
		GunRecoil recoil = getRecoil(data);
		if(yawShakeTick>=0){
			float coe = yawShakeTo/(yawShakeTick+1);
			yawShakeTo -= coe;
			Minecraft.getMinecraft().thePlayer.rotationYaw+=coe;
			yawShakeTick -= 1;
			if(yawShakeTick==-1){
				yawReturnTick = recoil.yaw_return_tick;
			}
		}
		if(pitchShakeTick>=0){
			float coe = pitchShakeTo/(pitchShakeTick+1);
			pitchShakeTo -= coe;
			Minecraft.getMinecraft().thePlayer.rotationPitch-=coe;
			pitchShakeTick -= 1;
			if(pitchShakeTick==-1){
				pitchReturnTick = recoil.pitch_return_tick;
			}
		}

		if(yawReturnTick>=0){
			float coe = yawReturnTo/(yawReturnTick+1);
			yawReturnTo -= coe;
			Minecraft.getMinecraft().thePlayer.rotationYaw-=coe;
			yawReturnTick -= 1;
		}
		if(pitchReturnTick>=0){
			float coe = pitchReturnTo/(pitchReturnTick+1);
			pitchReturnTo -= coe;
			Minecraft.getMinecraft().thePlayer.rotationPitch+=coe;
			pitchReturnTick -= 1;
		}
		if(recoilPower > 0){
			recoilPower = recoilPower-getRecoil(data).recoilPower_tick<0 ? 0 :recoilPower-getRecoil(data).recoilPower_tick;
		}
	}

	/**yaw軸の戻る先を取得*/
	static private float getYawReturn(GunRecoil data) {
		float base = data.yaw_base_min + (data.yaw_base_max - data.yaw_base_min / 100 * recoilPower);
		float spread = data.yaw_spread_min + ((data.yaw_spread_max - data.yaw_spread_min) / 100 * recoilPower);
		return (float) HideMath.normal(base, spread);
	}
	/**yaw軸のぶれる先を取得*/
	static private float getYawShake(GunRecoil data, float base) {
		float shake = data.yaw_shake_min + ((data.yaw_shake_max - data.yaw_shake_min) / 100 * recoilPower);
		return base*shake;
	}
	/**pitch軸の戻る先を取得*/
	static private float getPitchReturn(GunRecoil data) {
		float base = data.pitch_base_min + (data.pitch_base_max - data.pitch_base_min / 100 * recoilPower);
		float spread = data.pitch_spread_min + ((data.pitch_spread_max - data.pitch_spread_min) / 100 * recoilPower);
		return (float) HideMath.normal(base, spread);
	}
	/**pitch軸のぶれる先を取得*/
	static private float getPitchShake(GunRecoil data, float base) {
		float shake = data.pitch_shake_min + ((data.pitch_shake_max - data.pitch_shake_min) / 100 * recoilPower);
		return base*shake;
	}
}
