package hideMod.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

public class HideScope {
	public Framebuffer framebufferOut = new Framebuffer(0, 0, false);
	private float Zoom = 1;
	private float Scale = 0.7f;
	private ScopeMask Mask = new ScopeMask();

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

	public void renderOnGUI() {

		Minecraft mc = Minecraft.getMinecraft();

		int y = mc.displayHeight;
		int x = mc.displayWidth;

		float r = (float) Math.min(y, x) * Scale / 2;

		float scopeCenterX = 0.5f;
		float scopeCenterY = 0.5f;

		float textureX = (x * scopeCenterX) - r;
		float textureY = (y * scopeCenterY) - r;

		textureX += 2 * r * Mask.X;
		textureY += 2 * r * Mask.Y;

		updateImage(mc.getFramebuffer());

		// framebufferOut.bindFramebufferTexture();


		// バッファに投げ込む
		Framebuffer fb = mc.getFramebuffer();
		fb.unbindFramebuffer();
		fb.bindFramebufferTexture();
		updateSize(x, y);
		framebufferOut.framebufferClear();
		framebufferOut.bindFramebuffer(false);
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bb.pos(0, y, 0d).tex((textureX - r * Zoom) / x, (textureY - r * Zoom) / y).endVertex();
		bb.pos(x, y, 0d).tex((textureX + r * Zoom) / x, (textureY - r * Zoom) / y).endVertex();
		bb.pos(x, 0, 0d).tex((textureX + r * Zoom) / x, (textureY + r * Zoom) / y).endVertex();
		bb.pos(0, 0, 0d).tex((textureX - r * Zoom) / x, (textureY + r * Zoom) / y).endVertex();
		Tessellator.getInstance().draw();
		framebufferOut.unbindFramebuffer();
		framebufferOut.bindFramebufferTexture();
		fb.bindFramebuffer(true);
		//座標系をスケーリングする
		ScaledResolution sl = new ScaledResolution(mc);
		x /= sl.getScaleFactor();
		y /= sl.getScaleFactor();
		r /= sl.getScaleFactor();
		bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bb.pos(x * scopeCenterX - r, y * scopeCenterY + r, 0d).tex(0, 0).endVertex();
		bb.pos(x * scopeCenterX + r, y * scopeCenterY + r, 0d).tex(1, 0).endVertex();
		bb.pos(x * scopeCenterX + r, y * scopeCenterY - r, 0d).tex(1, 1).endVertex();
		bb.pos(x * scopeCenterX - r, y * scopeCenterY - r, 0d).tex(0, 1).endVertex();
		Tessellator.getInstance().draw();

	}

	/** フレームバッファからイメージを更新 */
	public void updateImage(Framebuffer fb) {




	}

	private void updateSize(int x, int y) {
		if (x != framebufferOut.framebufferWidth || y != framebufferOut.framebufferHeight) {
			framebufferOut.createFramebuffer(x, y);
			System.out.println(x+" "+y);
		}
	}

	public static class ScopeMask {
		public ResourceLocation Mask;
		public float X = 0.5f;
		public float Y = 0.5f;
	}
}
