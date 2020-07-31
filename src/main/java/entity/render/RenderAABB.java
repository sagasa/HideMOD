package entity.render;

import org.lwjgl.opengl.GL11;

import entity.EntityDebugAABB;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderAABB extends Render {

	public RenderAABB(RenderManager renderManager) {
		super(renderManager);
	}

	public void render(EntityDebugAABB aabb, double d, double d1, double d2, float f, float f1) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float) d, (float) d1, (float) d2);

		GlStateManager.disableDepth();
		GlStateManager.disableTexture2D();
		//GlStateManager.enableBlend();

		// GL11.glColor4ub(1,0, 0, 0.2F);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		GL11.glLineWidth(3);
		//RenderGlobal.drawBoundingBox(0, 0, 0, aabb.aabb.maxX - aabb.aabb.minX, aabb.aabb.maxY - aabb.aabb.minY, aabb.aabb.maxZ - aabb.aabb.minZ, aabb.r, aabb.g, aabb.b, 0.5f);

		buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
		RenderGlobal.addChainedFilledBoxVertices(buf, 0, 0, 0, aabb.aabb.maxX - aabb.aabb.minX, aabb.aabb.maxY - aabb.aabb.minY, aabb.aabb.maxZ - aabb.aabb.minZ, aabb.r, aabb.g, aabb.b, 0.3f);
		tessellator.draw();

		//GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();

		GlStateManager.enableDepth();
		GL11.glPopMatrix();
	}

	@Override
	public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
		render((EntityDebugAABB) entity, d, d1, d2, f, f1);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}
}
