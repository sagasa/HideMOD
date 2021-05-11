package hide.gltf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

public class Model {

	private GlTF model;

	public Model(GltfModel model) {
		for (NodeModel node : model.getSceneModels().get(0).getNodeModels()) {
			rootNodes.add(new HideNode(node));
			System.out.println(ArrayUtils.toString(node.getScale()));
		}
		for (AnimationModel animation : model.getAnimationModels()) {
			System.out.println(animation.getName());
			animations.put(animation.getName(), new HideAnimation(animation));
		}

		for (HideAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(0.5f);
		}
		//render();
		for (HideAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(0.1f);
		}
		//render();

		Vector4f pos = new Vector4f(1, 0, 0, 1);
		Matrix4f mat = new Matrix4f();
		mat.scale(new Vector3f(2, 2, 2));

		System.out.println(mul(mat, pos));
	}

	List<HideNode> rootNodes = new ArrayList<>();
	Map<String, HideAnimation> animations = new HashMap<>();

	float anim = 0;
	private Matrix4f projectionMatrix;

	public void render() {

		for (HideAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(anim);
		}
		anim += 0.001f;
		anim %= 1;

		GlStateManager.disableDepth();
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.disableCull();


		//GlStateManager.translate(0, 2, 0);

		for (HideNode node : rootNodes) {

			node.render();
			//	System.out.println(node.getMesh());

		}

		GL20.glDisableVertexAttribArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.enableDepth();
	}

	static Vector4f mul(Matrix4f mat, Vector4f pos) {
		Vector4f res = new Vector4f();
		res.x = mat.m00 * pos.x + mat.m01 * pos.y + mat.m02 * pos.z + mat.m03 * pos.w;
		res.y = mat.m10 * pos.x + mat.m11 * pos.y + mat.m12 * pos.z + mat.m13 * pos.w;
		res.z = mat.m20 * pos.x + mat.m21 * pos.y + mat.m22 * pos.z + mat.m23 * pos.w;
		res.w = mat.m30 * pos.x + mat.m31 * pos.y + mat.m32 * pos.z + mat.m33 * pos.w;
		return res;
	}

	private static final String SKIN_VERT = "assets/hidemod/shader/skinshader.vert";
	private static final String SKIN_FRAG = "assets/hidemod/shader/skinshader.frag";
	public static final int BONE_MAT_INDEX;
	public static final int WORLD_VIEW_PROJECTION_INDEX;
	public static final int TEST_MAT_0;
	public static final int SKIN_SHADER;

	private static final String TEST_VERT = "assets/hidemod/shader/test.vert";
	private static final String TEST_FRAG = "assets/hidemod/shader/test.frag";
	public static final int TEST_SHADER;
	public static final int TEST_MVP_MAT;
	public static final int TEST_MAT_1;
	public static final int TEST_MAT_2;

	static {
		SKIN_SHADER = makeProgram(SKIN_VERT, SKIN_FRAG);
		BONE_MAT_INDEX = GL20.glGetUniformLocation(SKIN_SHADER, "gs_BoneMatrices");
		WORLD_VIEW_PROJECTION_INDEX = GL20.glGetUniformLocation(SKIN_SHADER, "u_WorldViewProjectionMatrix");
		TEST_MAT_0 = GL20.glGetUniformLocation(SKIN_SHADER, "u_Test");

		TEST_SHADER = makeProgram(TEST_VERT, TEST_FRAG);
		TEST_MVP_MAT = GL20.glGetUniformLocation(TEST_SHADER, "u_WorldViewProjectionMat");
		TEST_MAT_1 = GL20.glGetUniformLocation(TEST_SHADER, "u_ModelViewMatrix");
		TEST_MAT_2 = GL20.glGetUniformLocation(TEST_SHADER, "u_ProjectionMatrix");

		System.out.println("\n" + GL20.glGetProgramInfoLog(TEST_SHADER, GL20.glGetProgrami(TEST_SHADER, GL20.GL_INFO_LOG_LENGTH)));

		System.out.println(SKIN_SHADER + " " + BONE_MAT_INDEX + " " + WORLD_VIEW_PROJECTION_INDEX);

		System.out.println(TEST_SHADER + " " + TEST_MVP_MAT + " " + TEST_MAT_1 + " " + TEST_MAT_2);
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
		if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			System.out.println("\n" + GL20.glGetProgramInfoLog(programId, GL20.glGetProgrami(programId, GL20.GL_INFO_LOG_LENGTH)));
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
}
