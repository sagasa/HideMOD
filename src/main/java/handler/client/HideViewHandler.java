package handler.client;

import org.lwjgl.opengl.GL11;

import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 銃 兵器 装備品 GUIの描画系 */
@SideOnly(Side.CLIENT)
public class HideViewHandler {
	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static boolean HitMarker_H = false;

	public static boolean isScope = false;
	public static boolean isADS = false;
	private static float defaultFOV;
	private static float defaultMS;

	private static ResourceLocation scopeImage;
	private static float scopeSize;

	/** ADSの切り替え クライアント側 */
	public static void setADS(String scope, float dia, float size) {

		if (dia <= 0) {
			clearADS();
			return;
		}
		//
		isScope = !Strings.isNullOrEmpty(scope);

		scopeImage = isScope ? new ResourceLocation(scope) : null;

		scopeSize = size;
		GameSettings setting = Minecraft.getMinecraft().gameSettings;
		// FOV
		if (!isADS) {
			defaultFOV = setting.fovSetting;
			defaultMS = setting.mouseSensitivity;
		}
		setting.fovSetting = defaultFOV / dia;
		setting.mouseSensitivity = defaultMS / dia;
		// マウス感度

		isADS = true;
	}

	/** ADS解除 クライアント側 */
	public static void clearADS() {
		if (isADS) {
			scopeImage = null;
			GameSettings setting = Minecraft.getMinecraft().gameSettings;
			// FOV
			setting.fovSetting = defaultFOV;
			// マウス感度
			setting.mouseSensitivity = defaultMS;
			isADS = false;
			isScope = false;
		}
	}

	/** 入力処理 */
	@SideOnly(Side.CLIENT)
	public static void ClientTick(EntityPlayerSP player) {
		// アップデート
		//RecoilHandler.updateRecoil(0f);
		if (HitMarkerTime > 0) {
			HitMarkerTime--;
		}
	}

	static Minecraft mc = FMLClientHandler.instance().getClient();

	public static boolean writeScope() {
		if (scopeImage == null)
			return false;
		ScaledResolution scaledresolution = new ScaledResolution(mc);
		int x = scaledresolution.getScaledWidth();
		int y = scaledresolution.getScaledHeight();
		int Zlevel = -200;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();

		GL11.glEnable(GL11.GL_STENCIL_TEST);
		GlStateManager.clear(GL11.GL_STENCIL_BUFFER_BIT);
		// GL11.glStencilMask(0xFF);
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
		GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
		GL11.glStencilMask(0xFF);
		GlStateManager.colorMask(false, false, false, false);

		int size = (int) (Math.min(x, y) * scopeSize * 0.5f);
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		buf.begin(7, DefaultVertexFormats.POSITION);
		buf.pos(x / 2 - size, y / 2 - size, Zlevel).endVertex();
		buf.pos(x / 2 - size, y / 2 + size, Zlevel).endVertex();
		buf.pos(x / 2 + size, y / 2 + size, Zlevel).endVertex();
		buf.pos(x / 2 + size, y / 2 - size, Zlevel).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();

		GlStateManager.color(0, 0, 0);
		GlStateManager.colorMask(true, true, true, true);
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		GL11.glStencilMask(0x00);

		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		buf.begin(7, DefaultVertexFormats.POSITION);
		buf.pos(0, 0, Zlevel).endVertex();
		buf.pos(0, y, Zlevel).endVertex();
		buf.pos(x, y, Zlevel).endVertex();
		buf.pos(x, 0, Zlevel).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();

		GlStateManager.color(1, 1, 1);
		GL11.glDisable(GL11.GL_STENCIL_TEST);

		//隙間防止
		size++;

		GlStateManager.enableAlpha();
		mc.renderEngine.bindTexture(scopeImage);
		buf.begin(7, DefaultVertexFormats.POSITION_TEX);
		buf.pos(x / 2 - size, y / 2 + size, Zlevel).tex(0, 1).endVertex();
		buf.pos(x / 2 + size, y / 2 + size, Zlevel).tex(1, 1).endVertex();
		buf.pos(x / 2 + size, y / 2 - size, Zlevel).tex(1, 0).endVertex();
		buf.pos(x / 2 - size, y / 2 - size, Zlevel).tex(0, 0).endVertex();
		tessellator.draw();
		GlStateManager.disableAlpha();
		GlStateManager.disableBlend();

		return true;
	}

}
