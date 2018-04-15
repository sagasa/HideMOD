package handler;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.client.event.RenderPlayerEvent.Pre;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderHandler {
	public static void RenderPlayerEvent(Pre event) {
		GL11.glPushMatrix();
		GL11.glScalef(20f, 20f, 20f);
		GL11.glTranslated(event.x, event.y, event.z);
		GL11.glRotatef(0, 0F, 1F, 0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		//GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(0F, 0F, 1F, 0.3F);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1F, 1F, 1F, 1F);
		GL11.glPopMatrix();

	}
}
