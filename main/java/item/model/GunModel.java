package item.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import types.render.HideRender;
import types.render.PolygonUV;
import types.render.VertexUV;

public class GunModel{

	HideRender GunRender;

	public GunModel() {
		PolygonUV[] GunModel = new PolygonUV[1];
		VertexUV ver1 = new VertexUV(0, 0, 0, 0, 0);
		VertexUV ver2 = new VertexUV(0, 1, 0, 64, 0);
		VertexUV ver3 = new VertexUV(0, 0, 1, 64, 64);
		GunModel[0] = new PolygonUV(ver1, ver2, ver3);
		GunRender = new HideRender(GunModel, 64, 64, new ResourceLocation("hidemod", "dummy.png"));
	}

	public void render(float partialTicks,EntityPlayerSP player){
		GlStateManager.pushMatrix();
		GlStateManager.disableCull();

		//高さを合わせる
		GlStateManager.translate(0, player.getEyeHeight(), 0);
		//角度を視線に合わせる
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        GlStateManager.rotate(pitch, (float) Math.cos(Math.toRadians(yaw)), 0.0F, (float) Math.sin(Math.toRadians(yaw)));
        GlStateManager.rotate(yaw+90, 0.0F, -1.0F, 0.0F);

        //GlStateManager.rotate((player.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
        //GlStateManager.rotate((player.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

		GunRender.dorender();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
}
