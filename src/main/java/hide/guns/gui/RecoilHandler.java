package hide.guns.gui;

import java.util.EnumMap;

import handler.client.HideViewHandler;
import helper.HideMath;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.effect.Recoil;
import types.items.GunData;

@SideOnly(Side.CLIENT)
public class RecoilHandler {
	private static int recoilPower = 0;

	private static EnumMap<EnumHand, RecoilCash> recoilcash = new EnumMap<>(EnumHand.class);

	static {
		recoilcash.put(EnumHand.MAIN_HAND, new RecoilCash());
		recoilcash.put(EnumHand.OFF_HAND, new RecoilCash());
	}

	private static class RecoilCash {
		private float yawReturnTo = 0;
		private float pitchReturnTo = 0;
		private float yawReturnTick = -1;
		private float pitchReturnTick = -1;

		private float yawShakeTo = 0;
		private float pitchShakeTo = 0;
		private float yawShakeTick = -1;
		private float pitchShakeTick = -1;

		private GunData nowGun = null;

		private void clearRecoil() {
			yawShakeTo = pitchShakeTo = 0;
			nowGun = null;
		}

		/**
		 * 反動を与える
		 *
		 * @param shooter
		 */
		private void addRecoil(GunData data) {
			nowGun = data;
			Recoil recoil = getRecoil(data);
			float yawrecoil = getYawRecoil(recoil);
			float pitchrecoil = getPitchRecoil(recoil);

			// リコイル戻し
			yawReturnTo = yawrecoil;
			pitchReturnTo = pitchrecoil;

			// リコイル
			yawShakeTo += yawrecoil;
			yawShakeTick = recoil.YAW_RECOIL_TICK;

			pitchShakeTo += pitchrecoil;
			pitchShakeTick = recoil.PITCH_RECOIL_TICK;

			pitchReturnTick = yawReturnTick = -1;
			// リコイルパワー加算
			recoilPower = recoilPower + getRecoil(data).POWER_SHOOT > 100 ? 100
					: recoilPower + getRecoil(data).POWER_SHOOT;
		}

		/** Tick毎の変化 */
		private void updateRecoil(float tick) {
			// 撃ってなければ戻る
			if (nowGun == null || Minecraft.getMinecraft().player == null) {
				return;
			}
			//
			Recoil recoil = getRecoil(nowGun);
			if (yawShakeTick >= 0) {
				float coe = yawShakeTo * tick / (yawShakeTick + 1);
				yawShakeTo -= coe;
				Minecraft.getMinecraft().player.rotationYaw += coe;
				yawShakeTick -= tick;
				if (yawShakeTick < 0) {
					yawReturnTick = recoil.YAW_RETURN_TICK;
				}
			}
			if (pitchShakeTick >= 0) {
				float coe = pitchShakeTo * tick / (pitchShakeTick + 1);
				pitchShakeTo -= coe;
				Minecraft.getMinecraft().player.rotationPitch -= coe;
				pitchShakeTick -= tick;
				if (pitchShakeTick < 0) {
					pitchReturnTick = recoil.PITCH_RETURN_TICK;
				}
			}

			if (yawReturnTick >= 0) {
				float coe = yawReturnTo * tick / (yawReturnTick + 1);
				yawReturnTo -= coe;
				Minecraft.getMinecraft().player.rotationYaw -= coe;
				yawReturnTick -= tick;
			}
			if (pitchReturnTick >= 0) {
				float coe = pitchReturnTo * tick / (pitchReturnTick + 1);
				pitchReturnTo -= coe;
				Minecraft.getMinecraft().player.rotationPitch += coe;
				pitchReturnTick -= tick;
			}
			if (recoilPower > 0) {
				recoilPower = recoilPower - recoil.POWER_TICK < 0 ? 0 : recoilPower - recoil.POWER_TICK;
			}
			// 適応が終わったら止める
			if (pitchReturnTick == -1 && yawReturnTick == -1 && pitchShakeTick == -1 && yawShakeTick == -1) {
				nowGun = null;
			}
			//*/
		}
	}

	static long lastTime = -1;

	/**tick update TODO レンダー側のTickでやりたい
	 * @param renderTickTime */
	public static void updateRecoil(float renderTickTime) {
		if (lastTime < 0) {
			lastTime = System.currentTimeMillis();
			return;
		}
		long now = System.currentTimeMillis();
		recoilcash.values().forEach(recoil -> recoil.updateRecoil((now - lastTime) / 50f));
		lastTime = now;
	}

	public static void addRecoil(GunData modifyData, EnumHand hand) {
		recoilcash.get(hand).addRecoil(modifyData);
	}

	public static void clearRecoil(EnumHand hand) {
		recoilcash.get(hand).clearRecoil();
	}

	/** 現在のリコイルパワー(0-100)を取得 */
	public static int getRecoilPower() {
		return recoilPower;
	}

	/** プレイヤーの状態から使用するリコイルを取得 */
	private static Recoil getRecoil(GunData data) {
		boolean sneak = Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.isSneaking() : false;
		return getRecoil(data, sneak, HideViewHandler.isADS);
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
