package hide.common.util;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DebugDraw {
	@SideOnly(Side.CLIENT)
	public static void drawString(String str, double x, double y, double z, float scale, int color) {
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();

		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(180, 1, 0, 0);
		GlStateManager.rotate(mc.player.rotationYaw + 180, 0, 1, 0);
		GlStateManager.rotate(mc.player.rotationPitch, -1, 0, 0);

		GlStateManager.scale(scale, scale, 1);

		GlStateManager.translate(mc.fontRenderer.getStringWidth(str) / -2, mc.fontRenderer.FONT_HEIGHT / -2, 0);

		drawRect(-2, -2, mc.fontRenderer.getStringWidth(str) + 1, mc.fontRenderer.FONT_HEIGHT, 0.5f, 0.5f,
				0.5f, 0.4f);
		mc.fontRenderer.drawString(str, 0, 0, color);

		GlStateManager.popMatrix();
	}

	@SideOnly(Side.CLIENT)
	public static void drawRect(double x, double y, double x1, double y1, float red, float green, float blue,
			float alpha) {
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableCull();

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
		buf.pos(x, y, 0).color(red, green, blue, alpha).endVertex();
		buf.pos(x1, y, 0).color(red, green, blue, alpha).endVertex();
		buf.pos(x, y1, 0).color(red, green, blue, alpha).endVertex();
		buf.pos(x1, y1, 0).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.enableCull();
	}
}
