package hideMod.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** モデルの原型 */
@SideOnly(Side.CLIENT)
public abstract class ModelBase {
	String Texture;

	float ScaleX = 1;
	float ScaleY = 1;
	float ScaleZ = 1;

	public class VertexUV {
		float U;
		float V;

		float X;
		float Y;
		float Z;

		public VertexUV(float x,float y,float z,float u,float v) {
			X = x;
			Y = y;
			Z = z;
			U = u;
			V = v;
		}
	}

	public class Surface {
		VertexUV[] Vertex;
	}

	// クライアントサイド 表示用
	@SideOnly(Side.CLIENT)
	public class DisplayPart {
		Surface[] Surface;

		/** 回転の基準 */
		float X;
		/** 回転の基準 */
		float Y;
		/** 回転の基準 */
		float Z;

		private int displayList;
		private boolean compiled;

		// 命令リストにコンパイル
		private void compileDisplayList() {
			this.displayList = GLAllocation.generateDisplayLists(1);
			GL11.glNewList(this.displayList, GL11.GL_COMPILE);
			WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
			// 面を全部呼ぶ
			for (Surface surface : Surface) {
				if (surface.Vertex.length == 3) {
					worldrenderer.startDrawingQuads();
					addVertexWithUV(worldrenderer,surface.Vertex[0]);
					addVertexWithUV(worldrenderer,surface.Vertex[1]);
					addVertexWithUV(worldrenderer,surface.Vertex[2]);
					addVertexWithUV(worldrenderer,surface.Vertex[2]);
					Tessellator.getInstance().draw();
				}else if (surface.Vertex.length == 4) {
					worldrenderer.startDrawingQuads();
					addVertexWithUV(worldrenderer,surface.Vertex[0]);
					addVertexWithUV(worldrenderer,surface.Vertex[1]);
					addVertexWithUV(worldrenderer,surface.Vertex[2]);
					addVertexWithUV(worldrenderer,surface.Vertex[3]);
					Tessellator.getInstance().draw();
				}
			}
			GL11.glEndList();
			this.compiled = true;
		}

		// 長かったから分けただけ
		private void addVertexWithUV(WorldRenderer worldrenderer, VertexUV vertex) {
			worldrenderer.addVertexWithUV(vertex.X, vertex.Y, vertex.Z, vertex.U, vertex.V);
		}

		void render() {
			if (!this.compiled) {
				this.compileDisplayList();
			}
			GL11.glPushMatrix();
			GL11.glCallList(this.displayList);
            GL11.glPopMatrix();
		}

		/**回転の基準を*/
		void setPoint(float x, float y, float z) {
			X = x;
			Y = y;
			Z = z;
		}

		void setRotate(float yaw, float pitch) {

		}
	}

}
