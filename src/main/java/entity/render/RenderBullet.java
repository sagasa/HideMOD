package entity.render;

import org.lwjgl.opengl.GL11;

import entity.EntityBullet;
import entity.model.ModelBullet;
import hidemod.HideMod;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderBullet extends Render {

	public static final ResourceLocation texture = new ResourceLocation(HideMod.MOD_ID, "skins/defaultBullet.png");

	public RenderBullet(RenderManager renderManager)
	{
		super(renderManager);
	}

	public void render(EntityBullet bullet, double d, double d1, double d2, float f, float f1)
	{
		bindEntityTexture(bullet);
		GL11.glPushMatrix();
		GL11.glTranslatef((float) d, (float) d1, (float) d2);
		GL11.glRotatef(f, 0.0F, -1.0F, 0.0F);
		GL11.glRotatef(90F -bullet.prevRotationPitch - (bullet.rotationPitch - bullet.prevRotationPitch) * f1, -1.0F, 0.0F, 0.0F);
		ModelBase model = new ModelBullet();
		if(model != null)
			model.render(bullet, 0.0F, 0.0F, f1, 0.0F, 0.0F, 0.05F);
		GL11.glPopMatrix();
	}

	@Override
	public void doRender(Entity entity, double d, double d1, double d2, float f, float f1)
	{
		render((EntityBullet) entity, d, d1, d2, f, f1);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return texture;
	}
}
