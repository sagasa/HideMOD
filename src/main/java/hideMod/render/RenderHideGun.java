package hideMod.render;

import org.lwjgl.opengl.GL11;

import hideMod.model.ModelGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import types.model.Polygon;

public class RenderHideGun extends RenderHideModel {
	public RenderHideGun(ModelGun model) {
		Model = model;
	}

	private ResourceLocation Textur;
	private ModelGun Model;

	/** テクスチャの画像を指定 */
	public void setTexture(ResourceLocation texture) {
		Textur = texture;
	}

	/** モデルを描画 */
	public void render(float partialTicks, EntityPlayerSP player) {
		Minecraft.getMinecraft().renderEngine.bindTexture(Textur);

		GlStateManager.pushMatrix();
		// GlStateManager.disableCull();

		// 高さを合わせる
		GlStateManager.translate(0, player.getEyeHeight(), 0);
		// 角度を視線に合わせる
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
		GlStateManager.rotate(pitch, (float) Math.cos(Math.toRadians(yaw)), 0.0F,
				(float) Math.sin(Math.toRadians(yaw)));
		GlStateManager.rotate(yaw + 90, 0.0F, -1.0F, 0.0F);

		// GlStateManager.rotate((player.rotationPitch - f5) * 0.1F, 1.0F, 0.0F,
		// 0.0F);
		// GlStateManager.rotate((player.rotationYaw - f6) * 0.1F, 0.0F, 1.0F,
		// 0.0F);
		dorender(Model.Body);
		// GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
}
