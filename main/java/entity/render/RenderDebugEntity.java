package entity.render;

import org.lwjgl.opengl.GL11;

import entity.EntityBullet;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderDebugEntity extends Render {

	public RenderDebugEntity(RenderManager renderManager)
	{
		super(renderManager);
	}


	@Override
	public void doRender(Entity entity, double d, double d1, double d2, float f, float f1)
	{
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glPushMatrix();
		GL11.glColor3f(0, 0,20);
		GL11.glTranslatef((float)d, (float)d1, (float)d2);
		GL11.glPointSize(5F);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3f(0F, 0F, 0F);
		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}
}
