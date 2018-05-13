package item.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;
import types.render.VertexUV;

/**3角か4角形を描画するだけ*/
public class ModelRender {

	VertexUV[] VertexList = new VertexUV[4];
	int TextureX;
	int TextureY;
	ResourceLocation Textur;

	/**3角 内部的には頂点が被った4角*/
	public ModelRender(VertexUV ver1,VertexUV ver2,VertexUV ver3,int textureX,int textureY,ResourceLocation texture){
		VertexList[0] = ver1;
		VertexList[1] = ver2;
		VertexList[2] = ver3;
		VertexList[3] = ver3;
		TextureX = textureX;
		TextureY = textureY;
		Textur = texture;
	}

	/**4角*/
	public ModelRender(VertexUV ver1,VertexUV ver2,VertexUV ver3,VertexUV ver4,int textureX,int textureY,ResourceLocation texture){
		VertexList[0] = ver1;
		VertexList[1] = ver2;
		VertexList[2] = ver3;
		VertexList[3] = ver4;
		TextureX = textureX;
		TextureY = textureY;
		Textur = texture;
	}

	public void render(Tessellator t){
		WorldRenderer render =t.getWorldRenderer();
		Minecraft.getMinecraft().renderEngine.bindTexture(Textur);
		render.startDrawingQuads();
		for (int i = 0; i < 4; i++) {
			render.addVertexWithUV(VertexList[i].X, VertexList[i].Y, VertexList[i].Z, VertexList[i].U/TextureX, VertexList[i].V/TextureY);
		}
		t.draw();
	}
}
