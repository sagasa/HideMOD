package model;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.Info;
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

		@Override
		public String toString() {
			return "[" + posX + "," + posY + "," + posZ + " : " + texU + "," + texV + "]";
		}
	}

	// エディタサイドのみ
	transient public Map<String, HideVertex[]> modelParts;

	// 共通
	@Info(isResourceName=true)
	public String texture = "";
	public Bone rootBone = new Bone();

	public float scaleX;//TODO

	public HideModel setModel(Map<String, HideVertex[]> faces) {
		modelParts = faces;
		return this;
	}

	transient private ResourceLocation Textur = new ResourceLocation(HideMod.MOD_ID, "skin/default_skinstg44.png");

	@SideOnly(Side.CLIENT)
	public void render() {
		if (modelParts != null) {
			//System.out.println("render");
			Minecraft mc = Minecraft.getMinecraft();

			Tessellator tessellator = Tessellator.getInstance();
			GL11.glPushMatrix();

			GlStateManager.disableCull();
			GlStateManager.scale(0.15, 0.15, 0.15);
			GlStateManager.scale(scaleX, scaleX, scaleX);

			Minecraft.getMinecraft().renderEngine.bindTexture(Textur);
			BufferBuilder buf = tessellator.getBuffer();
			for (HideVertex[] part : modelParts.values()) {
				int i = 3;
				for (int j = 0; j < part.length; j++) {
					HideVertex vert = part[j];
					if (i % 3 == 0) {
						buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
					}
					buf.pos(vert.posX, vert.posY, vert.posZ).tex(vert.texU, 1f - vert.texV).endVertex();
					if (i % 3 == 2) {
						tessellator.draw();
					}
					i++;
				}
			}
			GlStateManager.enableCull();
			GL11.glPopMatrix();
		}
	}
}
