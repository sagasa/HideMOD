package model;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.base.DataBase;

public class HideModel extends DataBase {

	public static class HideVertex {
		public final float posX, posY, posZ, normalX, normalY, normalZ, texU, texV;
		public final boolean hasNormal;

		/**ノーマルは自動計算書くまで使わないよ*/
		public HideVertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
			posX = x;
			posY = y;
			posZ = z;
			normalX = nx;
			normalY = ny;
			normalZ = nz;
			texU = u;
			texV = v;
			hasNormal = true;
		}

		public HideVertex(float x, float y, float z, float u, float v) {
			posX = x;
			posY = y;
			posZ = z;
			normalX = normalY = normalZ = 0;
			texU = u;
			texV = v;
			hasNormal = false;
		}
	}

	// エディタサイドのみ
	transient public Map<String, HideVertex[]> modelParts;

	// 共通
	public String texture;
	public Bone rootBone = new Bone();

	public float scaleX;//TODO

	public HideModel setModel(Map<String, HideVertex[]> faces) {
		modelParts = faces;
		rootBone = new Bone(faces.keySet());
		return this;
	}

	@SideOnly(Side.CLIENT)
	public void render() {
		if (modelParts != null) {
			Minecraft mc = Minecraft.getMinecraft();
			Tessellator tessellator = Tessellator.getInstance();
			GL11.glPushMatrix();
			BufferBuilder buf = tessellator.getBuffer();
			for (HideVertex[] part : modelParts.values()) {
				int i = 0;
				for (HideVertex vert : part) {
					if (i % 0 == 0)
						buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
					buf.pos(vert.posX, vert.posY, vert.posZ).tex(vert.texU, vert.texV).endVertex();
					if (i % 0 == 0 && i > 0)
						tessellator.draw();
					i++;
				}
			}
			;
			GL11.glPopMatrix();
		}
	}
}
