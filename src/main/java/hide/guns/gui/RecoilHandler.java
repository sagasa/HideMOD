package hide.guns.gui;

import java.util.EnumMap;

import handler.client.HideViewHandler;
import helper.HideMath;
import hide.types.effects.Recoil;
import hide.types.guns.ProjectileData;
import hide.types.util.DataView;
import hide.types.util.DataView.ViewCache;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RecoilHandler {
	private static float recoilPower = 0;

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

		private ViewCache<ProjectileData> nowGun = null;

		private void clearRecoil() {
			yawShakeTo = pitchShakeTo = 0;
			nowGun = null;
		}

		/**
		 * 反動を与える
		 *
		 * @param shooter
		 */
		private void addRecoil(ViewCache<ProjectileData> viewCache) {
			nowGun = viewCache;
			ViewCache<Recoil> recoil = getRecoil(viewCache);
			float yawrecoil = getHorizontalRecoil(recoil);
			float pitchrecoil = getVerticalRecoil(recoil);

			// リコイル戻し
			yawReturnTo = getHorizontalReturn(recoil, yawrecoil);
			pitchReturnTo = getVerticalReturn(recoil, pitchrecoil);

			//System.out.println("recoil " + yawrecoil + " " + pitchrecoil);

			// リコイル
			yawShakeTo += yawrecoil;
			yawShakeTick = recoil.get(Recoil.HorizontalRecoilTick).get(recoilPower);

			pitchShakeTo += pitchrecoil;
			pitchShakeTick = recoil.get(Recoil.VerticalRecoilTick).get(recoilPower);

			pitchReturnTick = yawReturnTick = -1;
			// リコイルパワー加算
			recoilPower += recoil.get(Recoil.PowerShoot);
			if (1f < recoilPower)
				recoilPower = 1f;
		}

		/** Tick毎の変化 */
		private void updateRecoil(float tick) {
			// 撃ってなければ戻る
			if (nowGun == null || Minecraft.getMinecraft().player == null) {
				return;
			}
			//
			ViewCache<Recoil> recoil = getRecoil(nowGun);
			if (yawShakeTick >= 0) {
				float coe = yawShakeTo * tick / (yawShakeTick + 1);
				yawShakeTo -= coe;
				Minecraft.getMinecraft().player.rotationYaw += coe;
				yawShakeTick -= tick;
				if (yawShakeTick < 0) {
					yawReturnTick = recoil.get(Recoil.HorizontalReturnTick).get(recoilPower);
				}
			}
			if (pitchShakeTick >= 0) {
				float coe = pitchShakeTo * tick / (pitchShakeTick + 1);
				pitchShakeTo -= coe;
				Minecraft.getMinecraft().player.rotationPitch -= coe;
				pitchShakeTick -= tick;
				if (pitchShakeTick < 0) {
					pitchReturnTick = recoil.get(Recoil.VerticalRecoilTick).get(recoilPower);
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
				recoilPower -= recoil.get(Recoil.PowerTick) * tick;
				if (recoilPower < 0)
					recoilPower = 0;
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

	public static void addRecoil(ViewCache<ProjectileData> viewCache, EnumHand hand) {
		recoilcash.get(hand).addRecoil(viewCache);
	}

	public static void clearRecoil(EnumHand hand) {
		recoilcash.get(hand).clearRecoil();
	}

	/** 現在のリコイルパワー(0-1)を取得 */
	public static float getRecoilPower() {
		return recoilPower;
	}

	/** プレイヤーの状態から使用するリコイルを取得 */
	private static ViewCache<Recoil> getRecoil(ViewCache<ProjectileData> viewCache) {
		boolean sneak = Minecraft.getMinecraft().player != null ? Minecraft.getMinecraft().player.isSneaking() : false;
		return getRecoil(viewCache, sneak, HideViewHandler.isADS);
	}

	private static DataView<Recoil> recoilView = new DataView<>(Recoil.class, 2);

	// 状態から取得 使えなかった場合前を参照
	private static ViewCache<Recoil> getRecoil(ViewCache<ProjectileData> viewCache, boolean isSneak, boolean isADS) {
		recoilView.setBase(viewCache.getData(ProjectileData.Recoil));
		recoilView.setModifier(0, isADS ? viewCache.getData(ProjectileData.RecoilADS) : null);
		recoilView.setModifier(1, isSneak ? viewCache.getData(ProjectileData.RecoilSneak) : null);
		return recoilView.getView();
	}

	/** 横の戻る先を取得 */
	static private float getHorizontalReturn(ViewCache<Recoil> data, float base) {
		return base * data.get(Recoil.HorizontalReturn).get(recoilPower);
	}

	/** 横のぶれる先を取得 */
	static private float getHorizontalRecoil(ViewCache<Recoil> recoil) {
		float base = recoil.get(Recoil.HorizontalBase).get(recoilPower);
		float spread = recoil.get(Recoil.HorizontalSpread).get(recoilPower);
		return (float) HideMath.normal(base, spread);
	}

	/** 縦の戻る先を取得 */
	static private float getVerticalReturn(ViewCache<Recoil> data, float base) {
		return base * data.get(Recoil.VerticalReturn).get(recoilPower);
	}

	/** 縦のぶれる先を取得 */
	static private float getVerticalRecoil(ViewCache<Recoil> data) {
		float base = data.get(Recoil.VerticalBase).get(recoilPower);
		float spread = data.get(Recoil.VerticalSpread).get(recoilPower);
		return (float) HideMath.normal(base, spread);
	}
}
