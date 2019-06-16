package handler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 銃 兵器 装備品 GUIの描画系 */
public class HideViewHandler {
	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static boolean HitMarker_H = false;

	public static boolean isADS = false;
	private static float defaultFOV;
	private static float defaultMS;
	public static String scopeName;

	/** ADSの切り替え クライアント側 */
	public static void setADS(String scope, float dia) {
		if (isADS) {
			clearADS();
		}
		//
		scopeName = scope;
		GameSettings setting = Minecraft.getMinecraft().gameSettings;
		// FOV
		defaultFOV = setting.fovSetting;
		setting.fovSetting = defaultFOV / dia;
		// マウス感度
		defaultMS = setting.mouseSensitivity;
		setting.mouseSensitivity = defaultMS / dia;
		isADS = true;
	}

	/** ADS解除 クライアント側 */
	public static void clearADS() {
		if (isADS) {
			GameSettings setting = Minecraft.getMinecraft().gameSettings;
			// FOV
			setting.fovSetting = defaultFOV;
			// マウス感度
			setting.mouseSensitivity = defaultMS;
			isADS = false;
		}
	}

	/** 入力処理 */
	@SideOnly(Side.CLIENT)
	public static void ClientTick(EntityPlayerSP player) {
		// アップデート
		RecoilHandler.updateRecoil();
		if (HitMarkerTime > 0) {
			HitMarkerTime--;
		}
	}
}
