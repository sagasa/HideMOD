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

public class HideScope_back {
	public Framebuffer framebufferOut = new Framebuffer(0, 0, false);
	private float Zoom = 1;
	private float Scale = 0.7f;
	private ResourceLocation Mask;

	/** 1<=zoom */
	public void setZoom(float zoom) {
		if (1 > zoom) {
			return;
		}
		Zoom = zoom;
	}

	public void setMask(ResourceLocation mask) {
		Mask = mask;
	}

	/** フレームバッファからイメージを更新 */
	public void updateImage(Framebuffer fb) {
		preRender();
		updateSize(fb);
		fb.unbindFramebuffer();
		fb.bindFramebufferTexture();
		float x = (float) this.framebufferOut.framebufferTextureWidth;
		float y = (float) this.framebufferOut.framebufferTextureHeight;

		framebufferOut.framebufferClear();
		framebufferOut.bindFramebuffer(false);

		GlStateManager.viewport(0, 0, (int) x, (int) y);
		float zoom = 0.5f - (0.5f / Zoom);
		GlStateManager.depthMask(false);
		GlStateManager.colorMask(true, true, true, true);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
		bufferbuilder.pos(0.0D, (double) y, 500.0D).tex(0, 0).endVertex();
		bufferbuilder.pos((double) x, (double) y, 500.0D).tex(1, 0).endVertex();
		bufferbuilder.pos((double) x, 0.0D, 500.0D).tex(1, 1).endVertex();
		bufferbuilder.pos(0.0D, 0.0D, 500.0D).tex(0, 1).endVertex();
		tessellator.draw();

		GlStateManager.enableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("hidemod", "gui/SARScope.png"));
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bufferbuilder.pos(0, y, 0d).tex(0, 0).endVertex();
		bufferbuilder.pos(x, y, 0d).tex(1, 0).endVertex();
		bufferbuilder.pos(x, 0, 0d).tex(1, 1).endVertex();
		bufferbuilder.pos(0, 0, 0d).tex(0, 1).endVertex();
		Tessellator.getInstance().draw();

		GlStateManager.glBlendEquation(GL14.GL_FUNC_ADD);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();

		GlStateManager.depthMask(true);
		GlStateManager.colorMask(true, true, true, true);
		this.framebufferOut.unbindFramebuffer();
		fb.unbindFramebufferTexture();
		fb.bindFramebuffer(true);
		// Minecraft.getMinecraft().getTextureManager().bindTexture(Mask);
	}

	private void updateSize(Framebuffer fb) {
		if (fb.framebufferWidth != framebufferOut.framebufferWidth
				|| fb.framebufferHeight != framebufferOut.framebufferHeight) {
			framebufferOut.createFramebuffer(fb.framebufferWidth, fb.framebufferHeight);
		}
	}

	public void renderOnGUI() {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution scaledresolution = new ScaledResolution(mc);
		int y = scaledresolution.getScaledHeight();
		int x = scaledresolution.getScaledWidth();

		float r = (float) Math.min(y, x) * Scale / 2;

		//framebufferOut.bindFramebufferTexture();
		Minecraft.getMinecraft().getFramebuffer().bindFramebufferTexture();
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bb.pos(x / 2 - r, y / 2 + r, 0d).tex(0, 0).endVertex();
		bb.pos(x / 2 + r, y / 2 + r, 0d).tex(1, 0).endVertex();
		bb.pos(x / 2 + r, y / 2 - r, 0d).tex(1, 1).endVertex();
		bb.pos(x / 2 - r, y / 2 - r, 0d).tex(0, 1).endVertex();
		Tessellator.getInstance().draw();
	}

	private void preRender() {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableBlend();
		GlStateManager.disableDepth();
		GlStateManager.disableAlpha();
		GlStateManager.disableFog();
		GlStateManager.disableLighting();
		GlStateManager.disableColorMaterial();
		GlStateManager.enableTexture2D();
		GlStateManager.bindTexture(0);
	}

	public static class ScopeMask {

	}
}
