package handler.client;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HideScope {
	/** カレントフレームバッファ */
	private static Framebuffer framebufferOut = new Framebuffer(0, 0, false);
	/** カレントスコープ */
	public static HideScope Scope = null;

	/** ズーム倍率 */
	private float Zoom = 2;
	/** 画面上の占有範囲 短いほうの辺に掛けて算出 */
	private float Scale = 0.7f;
	/** サイトのマスク */
	private ScopeMask Mask = new ScopeMask();

	public static void setScope(float zoom, float scale, ScopeMask mask) {
		Scope = new HideScope();
		Scope.Zoom = zoom;
		Scope.Scale = scale;
		Scope.Mask = mask;
	}

	public static void clearScope() {
		Scope = null;
	}

	/** 1<=zoom */
	public void setZoom(float zoom) {
		if (1 > zoom) {
			return;
		}
		Zoom = zoom;
	}

	public void setMask(ScopeMask mask) {
		Mask = mask;
	}

	public static void renderOnGUI() {
		if (Scope == null)
			return;

		Minecraft mc = Minecraft.getMinecraft();

		float y = mc.displayHeight;
		float x = mc.displayWidth;

		float r = Math.min(y, x) * Scope.Scale / 2;

		float scopeCenterX = 0.5f;
		float scopeCenterY = 0.5f;

		float textureX = (x * scopeCenterX) - r;
		float textureY = (y * scopeCenterY) - r;

		textureX += 2 * r * Scope.Mask.X;
		textureY += 2 * r * Scope.Mask.Y;

		framebufferOut.bindFramebufferTexture();
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		// 座標系をスケーリングする
		ScaledResolution sl = new ScaledResolution(mc);
		float x2 = x / sl.getScaleFactor();
		float y2 = y / sl.getScaleFactor();
		float r2 = r / sl.getScaleFactor();
		bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bb.pos(x2 * scopeCenterX - r2, y2 * scopeCenterY + r2, 0d)
				.tex((textureX - r / Scope.Zoom) / x, (textureY - r / Scope.Zoom) / y).endVertex();
		bb.pos(x2 * scopeCenterX + r2, y2 * scopeCenterY + r2, 0d)
				.tex((textureX + r / Scope.Zoom) / x, (textureY - r / Scope.Zoom) / y).endVertex();
		bb.pos(x2 * scopeCenterX + r2, y2 * scopeCenterY - r2, 0d)
				.tex((textureX + r / Scope.Zoom) / x, (textureY + r / Scope.Zoom) / y).endVertex();
		bb.pos(x2 * scopeCenterX - r2, y2 * scopeCenterY - r2, 0d)
				.tex((textureX - r / Scope.Zoom) / x, (textureY + r / Scope.Zoom) / y).endVertex();
		Tessellator.getInstance().draw();
	}

	public static void updateImage() {
		updateImage(Minecraft.getMinecraft().getFramebuffer());
	}

	/** フレームバッファからイメージを更新 */
	public static void updateImage(Framebuffer fb) {
		if (Scope == null)
			return;
		int x = fb.framebufferWidth;
		int y = fb.framebufferHeight;
		fb.unbindFramebuffer();
		fb.bindFramebufferTexture();
		updateSize(x, y);
		framebufferOut.framebufferClear();
		framebufferOut.bindFramebuffer(true);
		fb.framebufferRender(x, y);
		framebufferOut.unbindFramebuffer();
		framebufferOut.bindFramebufferTexture();
		fb.bindFramebuffer(true);
	}

	private static void updateSize(int x, int y) {
		if (x != framebufferOut.framebufferWidth || y != framebufferOut.framebufferHeight) {
			framebufferOut.createFramebuffer(x, y);
		}
	}

	public static class ScopeMask {
		public ResourceLocation Mask;
		public float X = 0.5f;
		public float Y = 0.5f;
	}
}
