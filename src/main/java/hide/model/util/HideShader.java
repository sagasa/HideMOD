package hide.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import hide.model.impl.IMaterial;
import hide.model.impl.ModelImpl;
import hide.opengl.ServerRenderContext;
import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HideShader {
	public final int ID;
	private final int WORLD_VIEW_PROJECTION_INDEX;
	private final int BASE_COLOR_TEXTURE;
	private final int HAS_NORMAL_TEXTURE;
	private final int NORMAL_TEXTURE;
	private final int HAS_EMISSIVE_TEXTURE;
	private final int EMISSIVE_TEXTURE;
	private final int HAS_OCCLUSION_TEXTURE;
	private final int OCCLUSION_TEXTURE;

	private HideShader(String vert_path, String frag_path) {
		ID = makeProgram(vert_path, frag_path);
		WORLD_VIEW_PROJECTION_INDEX = GL20.glGetUniformLocation(ID, "u_WorldViewProjectionMatrix");

		BASE_COLOR_TEXTURE = GL20.glGetUniformLocation(ID, "u_baseColorTexture");

		HAS_NORMAL_TEXTURE = GL20.glGetUniformLocation(ID, "u_hasNormalTexture");
		NORMAL_TEXTURE = GL20.glGetUniformLocation(ID, "u_normalTexture");

		HAS_EMISSIVE_TEXTURE = GL20.glGetUniformLocation(ID, "u_hasEmissiveTexture");
		EMISSIVE_TEXTURE = GL20.glGetUniformLocation(ID, "u_emissiveTexture");

		HAS_OCCLUSION_TEXTURE = GL20.glGetUniformLocation(ID, "u_hasOcclusionTexture");
		OCCLUSION_TEXTURE = GL20.glGetUniformLocation(ID, "u_occlusionTexture");
	}

	public void use() {
		GL20.glUseProgram(ID);
		FloatBuffer mat = BufferUtils.createFloatBuffer(32);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mat);
		mat.position(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, mat);
		TransformMatUtil.mul4x4(mat, 16, mat, 0, mat, 0);
		mat.position(0);
		mat.limit(16);

		GL20.glUniformMatrix4(WORLD_VIEW_PROJECTION_INDEX, false, mat);
	}

	public void material(IMaterial material) {
		if (material == null)
			return;
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.getBaseColorTexture());
		GL20.glUniform1i(BASE_COLOR_TEXTURE, 0);
		//
		//			GL20.glUniform1i(HAS_NORMAL_TEXTURE, material.normalTexture == -1 ? 0 : 1);
		//			if (material.normalTexture == -1) {
		//				GL13.glActiveTexture(GL13.GL_TEXTURE1);
		//				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.normalTexture);
		//				GL20.glUniform1i(NORMAL_TEXTURE, 1);
		//			}
		//
		//			GL20.glUniform1i(HAS_EMISSIVE_TEXTURE, material.emissiveTexture == -1 ? 0 : 1);
		//			if (material.normalTexture == -1) {
		//				GL13.glActiveTexture(GL13.GL_TEXTURE2);
		//				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.emissiveTexture);
		//				GL20.glUniform1i(EMISSIVE_TEXTURE, 2);
		//			}
		//
		//			GL20.glUniform1i(HAS_OCCLUSION_TEXTURE, material.occlusionTexture == -1 ? 0 : 1);
		//			if (material.normalTexture == -1) {
		//				GL13.glActiveTexture(GL13.GL_TEXTURE3);
		//				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.occlusionTexture);
		//				GL20.glUniform1i(OCCLUSION_TEXTURE, 3);
		//			}
		//			GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}

	public static final HideShader SKIN_SHADER;
	public static final int SKIN_BONE_MAT_INDEX;

	public static final HideShader BASE_SHADER;

	static {

		ModelImpl.profiler.profilingEnabled = true;
		if (ServerRenderContext.SUPPORT_CONTEXT) {
			SKIN_SHADER = new HideShader("assets/hidemod/shader/skinshader.vert", "assets/hidemod/shader/skinshader.frag");
			SKIN_BONE_MAT_INDEX = GL20.glGetUniformLocation(SKIN_SHADER.ID, "u_BoneMatrices");

			BASE_SHADER = new HideShader("assets/hidemod/shader/baseshader.vert", "assets/hidemod/shader/baseshader.frag");
		} else {
			SKIN_SHADER = null;
			SKIN_BONE_MAT_INDEX = 0;
			BASE_SHADER = null;
		}
	}

	@SideOnly(Side.CLIENT) //TODO ill be back
	private static ByteBuffer readResource(String path) {

		//ClassLoader.getSystemResourceAsStream(path);
		try (InputStream ins = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(HideMod.MOD_ID, path.replace("assets/hidemod/", ""))).getInputStream()) {
			byte[] data = IOUtils.toByteArray(ins);
			ByteBuffer bytebuffer = BufferUtils.createByteBuffer(data.length);
			bytebuffer.put(data);
			bytebuffer.rewind();
			return bytebuffer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static int makeProgram(String vert_path, String frag_path) {
		if (FMLCommonHandler.instance().getSide().isServer())
			return 0;

		//バーテックスシェーダのコンパイル
		int vShaderId = makeShader(GL20.GL_VERTEX_SHADER, readResource(vert_path));
		//フラグメントシェーダのコンパイル
		int fShaderId = makeShader(GL20.GL_FRAGMENT_SHADER, readResource(frag_path));

		//プログラムオブジェクトの作成
		int programId = GL20.glCreateProgram();
		GL20.glAttachShader(programId, vShaderId);
		GL20.glAttachShader(programId, fShaderId);

		// リンク
		GL20.glLinkProgram(programId);
		int length = GL20.glGetProgrami(programId, GL20.GL_INFO_LOG_LENGTH);
		if (0 < length) {
			System.out.println("\n" + GL20.glGetProgramInfoLog(programId, length));
		}

		GL20.glDeleteShader(vShaderId);
		GL20.glDeleteShader(fShaderId);
		return programId;
	}

	private static int makeShader(int type, ByteBuffer source) {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println("compile error in " + (type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader");
			int length = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
			System.out.println("\n" + GL20.glGetShaderInfoLog(shader, length));
		}
		return shader;
	}
}
