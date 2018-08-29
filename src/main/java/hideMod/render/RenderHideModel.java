package hideMod.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.model.DisplayPart;
import types.model.Polygon;
import types.model.VertexUV;

@SideOnly(Side.CLIENT)
public class RenderHideModel {
	/** パーツを描画 */
	// 命令リストにコンパイル
	private void compileDisplayList(DisplayPart part) {
		part.displayList = GLAllocation.generateDisplayLists(1);
		GL11.glNewList(part.displayList, GL11.GL_COMPILE);
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		// 面を全部呼ぶ
		for (Polygon surface : part.Polygon) {
			if (surface.Vertex.length == 3) {
				bb.begin(GL11.GL_QUADS, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
				bb.pos(x, y, z)

				worldrenderer.startDrawingQuads();
				addVertexWithUV(worldrenderer, surface.Vertex[0]);
				addVertexWithUV(worldrenderer, surface.Vertex[1]);
				addVertexWithUV(worldrenderer, surface.Vertex[2]);
				addVertexWithUV(worldrenderer, surface.Vertex[2]);
				Tessellator.getInstance().draw();
			} else if (surface.Vertex.length == 4) {
				worldrenderer.startDrawingQuads();
				addVertexWithUV(worldrenderer, surface.Vertex[0]);
				addVertexWithUV(worldrenderer, surface.Vertex[1]);
				addVertexWithUV(worldrenderer, surface.Vertex[2]);
				addVertexWithUV(worldrenderer, surface.Vertex[3]);
				Tessellator.getInstance().draw();
			}
		}
		GL11.glEndList();
		part.compiled = true;
	}

	// 長かったから分けただけ
	private void addVertexWithUV(WorldRenderer worldrenderer, VertexUV vertex) {
		worldrenderer.addVertexWithUV(vertex.X, vertex.Y, vertex.Z, vertex.U, vertex.V);
	}

	void dorender(DisplayPart part) {
		if (!part.compiled) {
			compileDisplayList(part);
		}
		GL11.glPushMatrix();
		GL11.glCallList(part.displayList);
		GL11.glPopMatrix();
	}
}
