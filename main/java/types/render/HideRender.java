package types.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;

/**リスト作成機能付き*/
public class HideRender {
	PolygonUV[] Polygons;
	int TextureX;
	int TextureY;
	ResourceLocation Textur;

	boolean compiled = false;
	int GLList;

	public HideRender(PolygonUV[] polygons,int textureX,int textureY,ResourceLocation texture){
		Polygons = polygons;
		TextureX = textureX;
		TextureY = textureY;
		Textur = texture;
	}

	private void compile(){
		Tessellator t = Tessellator.getInstance();
		WorldRenderer render =t.getWorldRenderer();
		GLList = GL11.glGenLists(1);
		GL11.glNewList(GLList, GL11.GL_COMPILE);

		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();

		GlStateManager.color(1f, 1f, 1f, 0.2f);
		//ポリゴンリスト
		for (PolygonUV polygonUV : Polygons) {
			VertexUV[] VertexList = polygonUV.VertexList;
			render.startDrawingQuads();
			for (int i = 0; i < 4; i++) {
				render.addVertexWithUV(VertexList[i].X, VertexList[i].Y, VertexList[i].Z, VertexList[i].U/TextureX, VertexList[i].V/TextureY);
			}
			t.draw();
		}

		GlStateManager.disableAlpha();
		GlStateManager.disableBlend();

        GL11.glEndList();
        compiled = true;
	}

	public void dorender(){
		if(!compiled){
			compile();
		}
		Minecraft.getMinecraft().renderEngine.bindTexture(Textur);
		GlStateManager.pushMatrix();
		GL11.glCallList(GLList);
		GlStateManager.popMatrix();
	}
}
