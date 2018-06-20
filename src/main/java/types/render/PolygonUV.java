package types.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;

/**3角か4角形を描画するだけ*/
public class PolygonUV {

	public VertexUV[] VertexList = new VertexUV[4];

	/**3角 内部的には頂点が被った4角*/
	public PolygonUV(VertexUV ver1,VertexUV ver2,VertexUV ver3){
		VertexList[0] = ver1;
		VertexList[1] = ver2;
		VertexList[2] = ver3;
		VertexList[3] = ver3;
	}

	/**4角*/
	public PolygonUV(VertexUV ver1,VertexUV ver2,VertexUV ver3,VertexUV ver4){
		VertexList[0] = ver1;
		VertexList[1] = ver2;
		VertexList[2] = ver3;
		VertexList[3] = ver4;
	}
}
