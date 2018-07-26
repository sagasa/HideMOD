package hideMod.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GunModel extends ModelBase{

	private ResourceLocation Textur;
	private int TextureX;
	private int TextureY;

	DisplayPart GunBody;

	public GunModel() {

	}


	/**テクスチャの画像を指定*/
	public void setTexture(ResourceLocation texture){
		Textur = texture;
	}

	/**テクスチャの画像サイズを指定*/
	public void setTextureSize(int x,int y){
		TextureX = x;
		TextureY = y;
	}

	/**モデルを読み込み テクスチャとサイズ指定後に*/
	public void setModel(String model){
		Surface GunModel = new Surface();
		VertexUV ver1 = new VertexUV(0, 0, 0, 0/TextureX, 0/TextureY);
		VertexUV ver2 = new VertexUV(0, 1, 0, 64/TextureX, 0/TextureY);
		VertexUV ver3 = new VertexUV(0, 1, 1, 64/TextureX, 64/TextureY);
		VertexUV ver4 = new VertexUV(0, 0, 1, 0/TextureX, 64/TextureY);
		GunModel.Vertex = new VertexUV[]{ver1,ver2,ver3,ver4};
		GunBody = new DisplayPart();
		GunBody.Surface = new Surface[]{GunModel};
	}


	/**モデル内容
	 * 本体
	 * マガジン
	 * スライド
	 * バレル
	 * 弾
	 * */

	public void render(float partialTicks,EntityPlayerSP player){
		Minecraft.getMinecraft().renderEngine.bindTexture(Textur);

		GlStateManager.pushMatrix();
		//GlStateManager.disableCull();

		//高さを合わせる
		GlStateManager.translate(0, player.getEyeHeight(), 0);
		//角度を視線に合わせる
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        GlStateManager.rotate(pitch, (float) Math.cos(Math.toRadians(yaw)), 0.0F, (float) Math.sin(Math.toRadians(yaw)));
        GlStateManager.rotate(yaw+90, 0.0F, -1.0F, 0.0F);

        //GlStateManager.rotate((player.rotationPitch - f5) * 0.1F, 1.0F, 0.0F, 0.0F);
        //GlStateManager.rotate((player.rotationYaw - f6) * 0.1F, 0.0F, 1.0F, 0.0F);

		GunBody.render();
		//GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
}
