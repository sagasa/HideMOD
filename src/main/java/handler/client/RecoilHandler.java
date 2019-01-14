package handler.client;

import handler.PlayerHandler;
import helper.HideMath;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.effect.Recoil;
import types.items.GunData;

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

	private static GunData nowGun = null;

	/**現在のリコイルパワー(0-100)を取得*/
	public static int getRecoilPower(){
		return recoilPower;
	}
	/** プレイヤーの状態から使用するリコイルを取得 */

	private static Recoil getRecoil(GunData data) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		return getRecoil(data, player.isSneaking(), PlayerHandler.isADS);
	}

	// 状態から取得 使えなかった場合前を参照
	private static Recoil getRecoil(GunData data, boolean isSneak, boolean isADS) {
		if (!isSneak) {
			if (isADS) {
				Recoil recoil = data.RECOIL_ADS;
				if (recoil.USE) {
					return recoil;
				}
				return getRecoil(data, false, false);
			} else {
				Recoil recoil = data.RECOIL_DEFAULT;
				if (recoil.USE) {
					return recoil;
				}
				return new Recoil();
			}
		} else {
			if (isADS) {
				Recoil recoil = data.RECOIL_SNEAK_ADS;
				if (recoil.USE) {
					return recoil;
				}
			} else {
				Recoil recoil = data.RECOIL_SNEAK;
				if (recoil.USE) {
					return recoil;
				}
			}
			return getRecoil(data, false, isADS);
		}
	}

	/** 反動を与える
	 * @param shooter */
	public static void addRecoil(GunData data) {
		// 銃が変わったならリコイルの適応を解除
		if (!data.equals(nowGun)) {
			yawShakeTo = pitchShakeTo = 0;
			nowGun = data;
		}
		Recoil recoil = getRecoil(data);
		float yawrecoil = getYawRecoil(recoil);
		float pitchrecoil = getPitchRecoil(recoil);

		// リコイル戻し
		yawReturnTo = getYawReturn(recoil, yawrecoil);
		pitchReturnTo = getPitchReturn(recoil, pitchrecoil);

		// リコイル
		yawShakeTo += yawrecoil;
		yawShakeTick = recoil.YAW_RECOIL_TICK;

		pitchShakeTo += pitchrecoil;
		pitchShakeTick = recoil.PITCH_RECOIL_TICK;
		// リコイルパワー加算
		recoilPower = recoilPower + getRecoil(data).POWER_SHOOT > 100 ? 100 : recoilPower + getRecoil(data).POWER_SHOOT;
	}

	/** Tick毎の変化 */
	static public void updateRecoil() {
		// 撃ってなければ戻る
		if (nowGun == null) {
			return;
		}
		Recoil recoil = getRecoil(nowGun);
		if (yawShakeTick >= 0) {
			float coe = yawShakeTo / (yawShakeTick + 1);
			yawShakeTo -= coe;
			Minecraft.getMinecraft().player.rotationYaw += coe;
			yawShakeTick -= 1;
			if (yawShakeTick == -1) {
				yawReturnTick = recoil.YAW_RETURN_TICK;
			}
		}
		if (pitchShakeTick >= 0) {
			float coe = pitchShakeTo / (pitchShakeTick + 1);
			pitchShakeTo -= coe;
			Minecraft.getMinecraft().player.rotationPitch -= coe;
			pitchShakeTick -= 1;
			if (pitchShakeTick == -1) {
				pitchReturnTick = recoil.PITCH_RETURN_TICK;
			}
		}

		if (yawReturnTick >= 0) {
			float coe = yawReturnTo / (yawReturnTick + 1);
			yawReturnTo -= coe;
			Minecraft.getMinecraft().player.rotationYaw -= coe;
			yawReturnTick -= 1;
		}
		if (pitchReturnTick >= 0) {
			float coe = pitchReturnTo / (pitchReturnTick + 1);
			pitchReturnTo -= coe;
			Minecraft.getMinecraft().player.rotationPitch += coe;
			pitchReturnTick -= 1;
		}
		if (recoilPower > 0) {
			recoilPower = recoilPower - recoil.POWER_TICK < 0 ? 0 : recoilPower - recoil.POWER_TICK;
		}
		// 適応が終わったら止める
		if (pitchReturnTick == -1 && yawReturnTick == -1) {
			nowGun = null;
		}
	}

	/** yaw軸の戻る先を取得 */
	static private float getYawReturn(Recoil data, float base) {
		float shake = data.MIN_YAW_RETURN + ((data.MAX_YAW_RETURN - data.MIN_YAW_RETURN) / 100 * recoilPower);
		return base * shake;
	}

	/** yaw軸のぶれる先を取得 */
	static private float getYawRecoil(Recoil data) {
		float base = data.MIN_YAW_BASE + (data.MAX_YAW_BASE - data.MIN_YAW_BASE / 100 * recoilPower);
		float spread = data.MIN_YAW_SPREAD + ((data.MAX_YAW_SPREAD - data.MIN_YAW_SPREAD) / 100 * recoilPower);
		return (float) HideMath.normal(base, spread);
	}

	/** pitch軸の戻る先を取得 */
	static private float getPitchReturn(Recoil data, float base) {
		float shake = data.MIN_PITCH_RETURN + ((data.MAX_PITCH_RETURN - data.MIN_PITCH_RETURN) / 100 * recoilPower);
		return base * shake;
	}

	/** pitch軸のぶれる先を取得 */
	static private float getPitchRecoil(Recoil data) {
		float base = data.MIN_PITCH_BASE + (data.MAX_PITCH_BASE - data.MIN_PITCH_BASE / 100 * recoilPower);
		float spread = data.MIN_PITCH_SPREAD + ((data.MAX_PITCH_SPREAD - data.MIN_PITCH_SPREAD) / 100 * recoilPower);
		return (float) HideMath.normal(base, spread);
	}
}
