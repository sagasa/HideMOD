package hide.gltf;

import static hide.gltf.TransformMatUtil.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;
import hide.gltf.base.IDisposable;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Matrix4f;

public class HideNode implements IDisposable {

	private static boolean GL30Supported = GLContext.getCapabilities().OpenGL30;

	List<HideNode> children = new ArrayList<>();

	List<MeshPrimitiveRender> renders = new ArrayList<>();

	public final NodeModel nodeModel;

	//skin render
	private final boolean useSkin;
	SkinModel skinModel;
	private VBOModel inverseMatrices;
	private FloatBuffer boneMat;

	public HideNode(NodeModel node) {
		nodeModel = node;
		for (NodeModel child : node.getChildren()) {
			children.add(new HideNode(child));
		}

		boolean hasWeight = node.getMeshModels().size() != 0;
		for (MeshModel mesh : node.getMeshModels()) {
			for (MeshPrimitiveModel primitive : mesh.getMeshPrimitiveModels()) {
				hasWeight = hasWeight && !primitive.getTargets().isEmpty();
				renders.add(new MeshPrimitiveRender(primitive));
			}
		}

		if (hasWeight) {
			node.setOnWeightChange(() -> {
				node.getWeights();



			});
		}
		System.out.println("hasWeight " + hasWeight + " " + node.getName());

		useSkin = node.getSkinModel() != null;
		if (useSkin) {
			skinModel = node.getSkinModel();
			inverseMatrices = getVBO(skinModel.getInverseBindMatrices());
			boneMat = BufferUtils.createFloatBuffer(inverseMatrices.numComponents * inverseMatrices.count);
			node.setOnMatChange(() -> {
				boneMat.rewind();
				computeJointMatrix(nodeModel, boneMat);
				System.out.println("CalcBoneMat");
			});
		}

	}

	static void read(FloatBuffer fb, boolean transpose) {
		float[] data = new float[16];
		fb.get(data);
		if (transpose) {
			Matrix4f mat = new Matrix4f(data);
			mat.transpose();
			data[0] = mat.m00;
			data[1] = mat.m01;
			data[2] = mat.m02;
			data[3] = mat.m03;
			data[4] = mat.m10;
			data[5] = mat.m11;
			data[6] = mat.m12;
			data[7] = mat.m13;
			data[8] = mat.m20;
			data[9] = mat.m21;
			data[10] = mat.m22;
			data[11] = mat.m23;
			data[12] = mat.m30;
			data[13] = mat.m31;
			data[14] = mat.m32;
			data[15] = mat.m33;
		}
		System.out.println(String.format("pos[%.2f,%.2f,%.2f]", data[12], data[13], data[14]));
		System.out.println(String.format("scale[%.2f,%.2f,%.2f]",
				Math.sqrt(data[0] * data[0] + data[4] * data[4] + data[8] * data[8]), //X軸
				Math.sqrt(data[1] * data[1] + data[5] * data[5] + data[9] * data[9]), //Y軸
				Math.sqrt(data[2] * data[2] + data[6] * data[6] + data[10] * data[10])//Z軸
		));

	}

	public void render() {
		GL11.glPushMatrix();

		FloatBuffer fb = BufferUtils.createFloatBuffer(16);

		fb.put(getLocalTransform(nodeModel));
		fb.rewind();

		GL11.glMultMatrix(fb);

		GlStateManager.color(1f, 0.5f, 0.5f, 1);
		GL11.glPointSize(5);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3f(0, 0, 0);
		GL11.glEnd();
		GlStateManager.color(0.5f, 1.0f, 0.5f, 0.5f);

		if (useSkin) {

			GL20.glUseProgram(Model.SKIN_SHADER);

			FloatBuffer mat = BufferUtils.createFloatBuffer(32);
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mat);
			mat.position(16);
			GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, mat);
			HideNode.mul(mat, 16, mat, 0, mat, 0);
			mat.position(0);
			mat.limit(16);

			GL20.glUniformMatrix4(Model.WORLD_VIEW_PROJECTION_INDEX, false, mat);

			boneMat.rewind();
			GL20.glUniformMatrix4(Model.BONE_MAT_INDEX, false, boneMat);
		}

		for (MeshPrimitiveRender meshPrimitiveRender : renders) {
			meshPrimitiveRender.render();
		}

		if (useSkin) {
			GL20.glUseProgram(0);
		}

		for (HideNode node : children) {
			node.render();
		}

		GL11.glPopMatrix();
	}

	private Map<AccessorModel, VBOModel> vboMap = new HashMap<>();

	private VBOModel getVBO(AccessorModel accessor) {
		if (!vboMap.containsKey(accessor)) {
			vboMap.put(accessor, new VBOModel(accessor));
		}
		return vboMap.get(accessor);
	}

	@Override
	public void dispose() {
		for (VBOModel vbo : vboMap.values()) {
			vbo.dispose();
		}

		for (MeshPrimitiveRender render : renders) {
			render.dispose();
		}

		for (HideNode node : children) {
			node.dispose();
		}
	}

	static void mul(FloatBuffer left, int left_i, FloatBuffer right, int right_i, FloatBuffer dest, int dest_i) {

		float m00 = left.get(left_i + 0) * right.get(right_i + 0) + left.get(left_i + 4) * right.get(right_i + 1) + left.get(left_i + 8) * right.get(right_i + 2) + left.get(left_i + 12) * right.get(right_i + 3);
		float m01 = left.get(left_i + 1) * right.get(right_i + 0) + left.get(left_i + 5) * right.get(right_i + 1) + left.get(left_i + 9) * right.get(right_i + 2) + left.get(left_i + 13) * right.get(right_i + 3);
		float m02 = left.get(left_i + 2) * right.get(right_i + 0) + left.get(left_i + 6) * right.get(right_i + 1) + left.get(left_i + 10) * right.get(right_i + 2) + left.get(left_i + 14) * right.get(right_i + 3);
		float m03 = left.get(left_i + 3) * right.get(right_i + 0) + left.get(left_i + 7) * right.get(right_i + 1) + left.get(left_i + 11) * right.get(right_i + 2) + left.get(left_i + 15) * right.get(right_i + 3);
		float m10 = left.get(left_i + 0) * right.get(right_i + 4) + left.get(left_i + 4) * right.get(right_i + 5) + left.get(left_i + 8) * right.get(right_i + 6) + left.get(left_i + 12) * right.get(right_i + 7);
		float m11 = left.get(left_i + 1) * right.get(right_i + 4) + left.get(left_i + 5) * right.get(right_i + 5) + left.get(left_i + 9) * right.get(right_i + 6) + left.get(left_i + 13) * right.get(right_i + 7);
		float m12 = left.get(left_i + 2) * right.get(right_i + 4) + left.get(left_i + 6) * right.get(right_i + 5) + left.get(left_i + 10) * right.get(right_i + 6) + left.get(left_i + 14) * right.get(right_i + 7);
		float m13 = left.get(left_i + 3) * right.get(right_i + 4) + left.get(left_i + 7) * right.get(right_i + 5) + left.get(left_i + 11) * right.get(right_i + 6) + left.get(left_i + 15) * right.get(right_i + 7);
		float m20 = left.get(left_i + 0) * right.get(right_i + 8) + left.get(left_i + 4) * right.get(right_i + 9) + left.get(left_i + 8) * right.get(right_i + 10) + left.get(left_i + 12) * right.get(right_i + 11);
		float m21 = left.get(left_i + 1) * right.get(right_i + 8) + left.get(left_i + 5) * right.get(right_i + 9) + left.get(left_i + 9) * right.get(right_i + 10) + left.get(left_i + 13) * right.get(right_i + 11);
		float m22 = left.get(left_i + 2) * right.get(right_i + 8) + left.get(left_i + 6) * right.get(right_i + 9) + left.get(left_i + 10) * right.get(right_i + 10) + left.get(left_i + 14) * right.get(right_i + 11);
		float m23 = left.get(left_i + 3) * right.get(right_i + 8) + left.get(left_i + 7) * right.get(right_i + 9) + left.get(left_i + 11) * right.get(right_i + 10) + left.get(left_i + 15) * right.get(right_i + 11);
		float m30 = left.get(left_i + 0) * right.get(right_i + 12) + left.get(left_i + 4) * right.get(right_i + 13) + left.get(left_i + 8) * right.get(right_i + 14) + left.get(left_i + 12) * right.get(right_i + 15);
		float m31 = left.get(left_i + 1) * right.get(right_i + 12) + left.get(left_i + 5) * right.get(right_i + 13) + left.get(left_i + 9) * right.get(right_i + 14) + left.get(left_i + 13) * right.get(right_i + 15);
		float m32 = left.get(left_i + 2) * right.get(right_i + 12) + left.get(left_i + 6) * right.get(right_i + 13) + left.get(left_i + 10) * right.get(right_i + 14) + left.get(left_i + 14) * right.get(right_i + 15);
		float m33 = left.get(left_i + 3) * right.get(right_i + 12) + left.get(left_i + 7) * right.get(right_i + 13) + left.get(left_i + 11) * right.get(right_i + 14) + left.get(left_i + 15) * right.get(right_i + 15);

		dest.position(dest_i);
		dest.put(m00);
		dest.put(m01);
		dest.put(m02);
		dest.put(m03);
		dest.put(m10);
		dest.put(m11);
		dest.put(m12);
		dest.put(m13);
		dest.put(m20);
		dest.put(m21);
		dest.put(m22);
		dest.put(m23);
		dest.put(m30);
		dest.put(m31);
		dest.put(m32);
		dest.put(m33);
	}

	class MeshPrimitiveRender implements IDisposable {

		private int vao = -1;
		private final Map<Attribute, VBOModel> attributeMap = new EnumMap<>(Attribute.class);
		private final VBOModel indicesVBO;
		private final int drawMode;
		private final int vertexCount;
		private final int componentType;
		private final long offset;

		public MeshPrimitiveRender(MeshPrimitiveModel primitive) {
			drawMode = primitive.getMode();
			AccessorModel indices = primitive.getIndices();
			vertexCount = indices.getCount();
			componentType = indices.getComponentType();
			offset = indices.getByteOffset();

			primitive.getTargets();

			for (Entry<String, AccessorModel> entry : primitive.getAttributes().entrySet()) {
				Attribute attribute = Attribute.valueOf(entry.getKey());
				if (attribute != null) {
					attributeMap.put(attribute, getVBO(entry.getValue()));
				}
			}

			indicesVBO = getVBO(indices);
			indicesVBO.target = GL15.GL_ELEMENT_ARRAY_BUFFER;

		}

		private void bind() {
			if (GL30Supported) {
				vao = GL30.glGenVertexArrays();
				GL30.glBindVertexArray(vao);
			}

			//バッファバインド
			for (Entry<Attribute, VBOModel> entry : attributeMap.entrySet()) {
				VBOModel vbo = entry.getValue();
				int index = entry.getKey().index;
				vbo.bind();
				vbo.bindAttribPointer(index);
			}

			indicesVBO.bind();

			if (GL30Supported) {
				GL30.glBindVertexArray(0);
			}
		}

		void render() {
			if (GL30Supported) {
				if (vao == -1)
					bind();
				GL30.glBindVertexArray(vao);
			} else {
				bind();
			}

			GL11.glDrawElements(drawMode, vertexCount, componentType, offset);

			if (GL30Supported) {
				GL30.glBindVertexArray(0);
			}
		}

		@Override
		public void dispose() {
			if (vao != -1)
				GL30.glDeleteVertexArrays(vao);
		}
	}

	private class VBOModel implements IDisposable {
		private int target = GL15.GL_ARRAY_BUFFER;
		private final int vbo;

		private final int count;
		private final int numComponents;
		private final int componentType;
		private final int byteStride;
		private final long byteOffset;

		public VBOModel(AccessorModel accessor) {
			BufferViewModel buf = accessor.getBufferViewModel();
			if (buf.getTarget() != null)
				target = buf.getTarget();
			vbo = GL15.glGenBuffers();
			GL15.glBindBuffer(target, vbo);
			buf.getBufferViewData().rewind();
			GL15.glBufferData(target, buf.getBufferViewData(), GL15.GL_STATIC_DRAW);

			count = accessor.getCount();
			numComponents = accessor.getElementType().getNumComponents();
			componentType = accessor.getComponentType();
			byteStride = accessor.getByteStride();
			byteOffset = accessor.getByteOffset();
		}

		void bind() {
			GL15.glBindBuffer(target, vbo);
		}

		void bindAttribPointer(int index) {
			GL20.glEnableVertexAttribArray(index);
			GL20.glVertexAttribPointer(index, numComponents, componentType, false, byteStride, byteOffset);
		}

		@Override
		public void dispose() {
			GL15.glDeleteBuffers(vbo);
		}
	}

	public enum Attribute {
		POSITION(0), NORMAL(1), TEXCOORD_0(2), TANGENT(3), COLOR_0(4), JOINTS_0(5), WEIGHTS_0(6);

		public final int index;

		Attribute(final int index) {
			this.index = index;
		}
	}

}
