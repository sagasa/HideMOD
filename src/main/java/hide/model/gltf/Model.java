package hide.model.gltf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.TextureModel;
import hide.model.gltf.base.ByteBufferInputStream;
import hide.model.gltf.base.IDisposable;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Model implements IDisposable {


	public static Profiler profiler = new Profiler();


	private GlTF model;

	List<DynamicTexture> textures = new ArrayList<>();

	List<HideNode> rootNodes = new ArrayList<>();
	Map<String, HideAnimation> animations = new HashMap<>();
	Map<MaterialModel, HideMaterial> matMap = new HashMap<>();

	public Model(GltfModel model) {
		// Client
		for (TextureModel texture : model.getTextureModels()) {
			textures.add(makeTexture(texture.getImageModel().getImageData()));
		}

		for (MaterialModel material : model.getMaterialModels()) {
			matMap.put(material, new HideMaterial(material));
		}

		for (NodeModel node : model.getSceneModels().get(0).getNodeModels()) {
			rootNodes.add(new HideNode(node, this));
			System.out.println(ArrayUtils.toString(node.getScale()));
		}
		for (AnimationModel animation : model.getAnimationModels()) {
			System.out.println(animation.getName());
			animations.put(animation.getName(), new HideAnimation(animation));
		}

		for (HideAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(0.5f);
		}
	}

	/**Client側処理*/
	@SideOnly(Side.CLIENT)
	private static DynamicTexture makeTexture(ByteBuffer data) {
		try (ByteBufferInputStream is = new ByteBufferInputStream(data)) {
			return new DynamicTexture(TextureUtil.readBufferedImage(is));
		} catch (IOException e) {
			//Tails.LOGGER.error("Failed to load texture " + name, e);
		}
		return null;
	}

	float anim = 0;

	public void render() {

		for (HideAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(anim);
		}
		anim += 0.001f;
		anim %= 1;

		GlStateManager.enableBlend();

		//GlStateManager.translate(0, 2, 0);

		for (HideNode node : rootNodes) {
			node.render();
		}

		GL20.glDisableVertexAttribArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		//GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures.get(1).getGlTextureId());
		//Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 100, 100, 200, 200);

		GlStateManager.disableBlend();

	}

	public static final HideShader SKIN_SHADER;
	public static final int SKIN_BONE_MAT_INDEX;

	public static final HideShader BASE_SHADER;

	static {
		profiler.profilingEnabled = true;

		SKIN_SHADER = new HideShader("assets/hidemod/shader/skinshader.vert", "assets/hidemod/shader/skinshader.frag");
		SKIN_BONE_MAT_INDEX = GL20.glGetUniformLocation(SKIN_SHADER.ID, "u_BoneMatrices");

		BASE_SHADER = new HideShader("assets/hidemod/shader/baseshader.vert", "assets/hidemod/shader/baseshader.frag");
	}

	public static class HideShader {
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
			TransformMatUtil.mul(mat, 16, mat, 0, mat, 0);
			mat.position(0);
			mat.limit(16);

			GL20.glUniformMatrix4(WORLD_VIEW_PROJECTION_INDEX, false, mat);
		}

		public void material(HideMaterial material) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.baseColorTexture);
			GL20.glUniform1i(BASE_COLOR_TEXTURE, 0);

			GL20.glUniform1i(HAS_NORMAL_TEXTURE, material.normalTexture == -1 ? 0 : 1);
			if (material.normalTexture == -1) {
				GL13.glActiveTexture(GL13.GL_TEXTURE1);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.normalTexture);
				GL20.glUniform1i(NORMAL_TEXTURE, 1);
			}

			GL20.glUniform1i(HAS_EMISSIVE_TEXTURE, material.emissiveTexture == -1 ? 0 : 1);
			if (material.normalTexture == -1) {
				GL13.glActiveTexture(GL13.GL_TEXTURE2);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.emissiveTexture);
				GL20.glUniform1i(EMISSIVE_TEXTURE, 2);
			}

			GL20.glUniform1i(HAS_OCCLUSION_TEXTURE, material.occlusionTexture == -1 ? 0 : 1);
			if (material.normalTexture == -1) {
				GL13.glActiveTexture(GL13.GL_TEXTURE3);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, material.occlusionTexture);
				GL20.glUniform1i(OCCLUSION_TEXTURE, 3);
			}
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
		}
	}

	private static int makeProgram(String vert_path, String frag_path) {
		//バーテックスシェーダのコンパイル
		int vShaderId = makeShader(GL20.GL_VERTEX_SHADER, readResource(vert_path));
		//フラグメントシェーダのコンパイル
		int fShaderId = makeShader(GL20.GL_FRAGMENT_SHADER, readResource(frag_path));

		//プログラムオブジェクトの作成
		int programId = OpenGlHelper.glCreateProgram();
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

	private static ByteBuffer readResource(String path) {
		try (InputStream ins = ClassLoader.getSystemResourceAsStream(path)) {
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

	@Override
	public void dispose() {
		for (HideNode hideNode : rootNodes) {
			hideNode.dispose();
		}
		for (DynamicTexture texture : textures) {
			texture.deleteGlTexture();
		}
	}

	class HideMaterial {
		public HideMaterial(MaterialModel material) {
			Map<String, Object> map = material.getValues();
			if ((int) map.get("hasBaseColorTexture") == 1)
				baseColorTexture = textures.get((int) map.get("baseColorTexture")).getGlTextureId();
			if ((int) map.get("hasNormalTexture") == 1)
				normalTexture = textures.get((int) map.get("normalTexture")).getGlTextureId();
			if ((int) map.get("hasEmissiveTexture") == 1)
				emissiveTexture = textures.get((int) map.get("emissiveTexture")).getGlTextureId();
			if ((int) map.get("hasOcclusionTexture") == 1)
				occlusionTexture = textures.get((int) map.get("occlusionTexture")).getGlTextureId();

			metallicFactor = (float) map.get("metallicFactor");
			roughnessFactor = (float) map.get("roughnessFactor");
		}

		int baseColorTexture = -1;
		int normalTexture = -1;
		int emissiveTexture = -1;
		int occlusionTexture = -1;

		float metallicFactor;
		float roughnessFactor;
	}
}
