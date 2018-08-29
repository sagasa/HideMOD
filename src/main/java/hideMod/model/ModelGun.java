package hideMod.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.model.DisplayPart;
import types.model.Polygon;
import types.model.VertexUV;

@SideOnly(Side.CLIENT)
public class ModelGun extends ModelBase{

	public DisplayPart Body;
	public DisplayPart Magazine;
	public DisplayPart Barrel;
	public DisplayPart Leaver;

	public ModelGun() {
		int TextureX = 64;
		int TextureY = 64;

		Polygon GunModel = new Polygon();
		VertexUV ver1 = new VertexUV(0, 0, 0, 0/TextureX, 0/TextureY);
		VertexUV ver2 = new VertexUV(0, 1, 0, 64/TextureX, 0/TextureY);
		VertexUV ver3 = new VertexUV(0, 1, 1, 64/TextureX, 64/TextureY);
		VertexUV ver4 = new VertexUV(0, 0, 1, 0/TextureX, 64/TextureY);
		GunModel.Vertex = new VertexUV[]{ver1,ver2,ver3,ver4};
		Body = new DisplayPart();
		Body.Polygon = new Polygon[]{GunModel};
	}

	/**モデル内容
	 * 本体
	 * マガジン
	 * スライド
	 * バレル
	 * 弾
	 * */
}
