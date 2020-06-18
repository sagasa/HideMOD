package model;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.lwjgl.opengl.GL11;

import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.Info;
import types.base.DataBase;

public class HideModel extends DataBase {

	public static class Pos3f {
		public float X = 0, Y = 0, Z = 0;

		@Override
		public String toString() {
			return "[" + X + "," + Y + "," + Z + "]";
		}
	}

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

	// 共通
	@Info(isResourceName = true)
	public String texture = "";
	public Bone rootBone = new Bone();

	public float scaleX;//TODO

	public Pos3f offset = new Pos3f();
	public Pos3f offsetFirstPerson = new Pos3f();

	public HideModel setModel(Map<String, HideVertex[]> faces) {
		modelParts = faces;
		rootBone.init(this);
		return this;
	}

	transient private ResourceLocation TexResource = null;

	/**モデル 別ファイルから読み込み*/
	transient Map<String, HideVertex[]> modelParts;

	transient private boolean isInit = false;
	transient private Map<String, Integer> renderLists = new HashMap<>();

	public void render(String name) {
		if (!isInit) {
			for (String key : modelParts.keySet()) {
				int list;
				GlStateManager.glNewList(list = GLAllocation.generateDisplayLists(1), GL11.GL_COMPILE);
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder buf = tessellator.getBuffer();
				HideVertex[] part = modelParts.get(key);
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
				GlStateManager.glEndList();
				renderLists.put(key, list);
			}
			isInit = true;
		}
		if (renderLists.containsKey(name))
			GlStateManager.callList(renderLists.get(name));
	}

	@SideOnly(Side.CLIENT)
	public void render(boolean firstPerson, IRenderProperty prop) {
		if (modelParts != null) {

			if (TexResource == null) {
				if (Strings.isEmpty(texture)) {
					TexResource = new ResourceLocation(HideMod.MOD_ID , "sample.png");
				}else {
					String tex = texture.replace(":", ":skin/");
					TexResource = new ResourceLocation(tex);
				}
				System.out.println(texture);
			}

			//System.out.println("render");
			Minecraft mc = Minecraft.getMinecraft();

			GL11.glPushMatrix();

			GlStateManager.scale(0.15, 0.15, 0.15);

			if (firstPerson)
				GlStateManager.translate(offsetFirstPerson.X, offsetFirstPerson.Y, offsetFirstPerson.Z);
			GlStateManager.translate(offset.X, offset.Y, offset.Z);

			GlStateManager.scale(scaleX, scaleX, scaleX);

			Minecraft.getMinecraft().renderEngine.bindTexture(TexResource);

			rootBone.render(prop);

			GL11.glPopMatrix();
		}
	}
}
