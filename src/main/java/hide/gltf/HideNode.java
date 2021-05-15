package hide.gltf;

import static hide.gltf.TransformMatUtil.*;

import java.nio.ByteBuffer;
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
import hide.gltf.Model.HideMaterial;
import hide.gltf.Model.HideShader;
import hide.gltf.base.IDisposable;
import net.minecraft.client.renderer.GlStateManager;

public class HideNode implements IDisposable {


	private static boolean GL30Supported = GLContext.getCapabilities().OpenGL30;

	List<HideNode> children = new ArrayList<>();

	List<MeshPrimitiveRender> renders = new ArrayList<>();

	public final NodeModel nodeModel;
	private final Model model;
	private final HideShader shader;

	//skin render
	private final boolean useSkin;
	SkinModel skinModel;
	private VBOModel inverseMatrices;
	private FloatBuffer boneMat;

	public HideNode(NodeModel node, Model model) {
		nodeModel = node;
		this.model = model;
		for (NodeModel child : node.getChildren()) {
			children.add(new HideNode(child, model));
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
				Model.profiler.startSection("hide.updateWeight");
				for (MeshPrimitiveRender meshPrimitiveRender : renders) {
					meshPrimitiveRender.calcWeight(node.getWeights());
				}
				Model.profiler.endSection();
			});
		}

		useSkin = node.getSkinModel() != null;
		shader = useSkin ? Model.SKIN_SHADER : Model.BASE_SHADER;
		if (useSkin) {
			skinModel = node.getSkinModel();
			inverseMatrices = getVBO(skinModel.getInverseBindMatrices());
			boneMat = BufferUtils.createFloatBuffer(inverseMatrices.numComponents * inverseMatrices.count);
			node.setOnMatChange(() -> {

				System.out.println("CalcBoneMat");
			});
		}

	}

	public void render() {
		Model.profiler.startSection("hide.render");
		GL11.glPushMatrix();

		FloatBuffer fb = BufferUtils.createFloatBuffer(16);
		fb.put(getLocalTransform(nodeModel));
		fb.rewind();
		GL11.glMultMatrix(fb);

		Model.profiler.endStartSection("hide.render.debug");
		GlStateManager.disableTexture2D();
		GlStateManager.color(1f, 0.5f, 0.5f, 1);
		GL11.glPointSize(5);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3f(0, 0, 0);
		GL11.glEnd();
		GlStateManager.enableTexture2D();
		Model.profiler.endStartSection("hide.render.calcBone");

		shader.use();
		if (useSkin) {
			boneMat.rewind();
			computeJointMatrix(nodeModel, boneMat);
			boneMat.rewind();
			GL20.glUniformMatrix4(Model.SKIN_BONE_MAT_INDEX, false, boneMat);
		}
		Model.profiler.endStartSection("hide.render.draw");
		for (MeshPrimitiveRender meshPrimitiveRender : renders) {
			meshPrimitiveRender.render();
		}

		GL20.glUseProgram(0);
		Model.profiler.endSection();
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



	class MeshPrimitiveRender implements IDisposable {

		private int vao = -1;
		private final boolean hasTargets;
		private final Map<Attribute, VBOModel> attributeMap = new EnumMap<>(Attribute.class);
		private final VBOModel indicesVBO;
		private final HideMaterial material;
		private final int drawMode;
		private final int vertexCount;
		private final int componentType;
		private final long offset;
		//モーフィング
		private List<Map<Attribute, VBOModel>> targets = new ArrayList<>();
		private final Map<Attribute, VBOModel> target = new EnumMap<>(Attribute.class);

		public MeshPrimitiveRender(MeshPrimitiveModel primitive) {
			material = model.matMap.get(primitive.getMaterialModel());
			drawMode = primitive.getMode();
			AccessorModel indices = primitive.getIndices();
			vertexCount = indices.getCount();
			componentType = indices.getComponentType();
			offset = indices.getByteOffset();

			hasTargets = !primitive.getTargets().isEmpty();

			for (Entry<String, AccessorModel> entry : primitive.getAttributes().entrySet()) {
				Attribute attribute = Attribute.valueOf(entry.getKey());
				if (attribute != null) {
					attributeMap.put(attribute, getVBO(entry.getValue()));
				}
			}

			//モーフィングがあるなら
			if (hasTargets) {
				for (Map<String, AccessorModel> src : primitive.getTargets()) {
					Map<Attribute, VBOModel> map = new EnumMap<>(Attribute.class);
					for (Entry<String, AccessorModel> entry : src.entrySet()) {
						Attribute attribute = Attribute.valueOf(entry.getKey());
						if (attribute != null)
							map.put(attribute, getVBO(entry.getValue()));
					}
					targets.add(map);
				}
				for (String str : primitive.getTargets().get(0).keySet()) {
					Attribute attribute = Attribute.valueOf(str);
					if (attribute != null) {
						VBOModel vbo = new VBOModel(primitive.getAttributes().get(str));
						vbo.setData(BufferUtils.createByteBuffer(vbo.buffer.limit()));
						target.put(attribute, vbo);
						System.out.println(attribute);
					}
				}
			}

			indicesVBO = getVBO(indices);
			indicesVBO.target = GL15.GL_ELEMENT_ARRAY_BUFFER;

			System.out.println(primitive.getMaterialModel().getValues());

		}

		void calcWeight(float[] weight) {
			for (Attribute attribute : target.keySet()) {
				ByteBuffer out = target.get(attribute).buffer;
				for (int j = 0; j < out.limit(); j += 4) {
					//float f = weight[j];
					float sum = attributeMap.get(attribute).buffer.getFloat(j);
					for (int i = 0; i < weight.length; i++) {
						//Model.profiler.endStartSection("hide.updateWeight.sum");
						sum += targets.get(i).get(attribute).buffer.getFloat(j) * weight[i];

						//Model.profiler.endSection();
					}
					out.putFloat(j, sum);
				}
				target.get(attribute).uploadData();
			}
		}

		private void bind() {
			if (GL30Supported) {
				vao = GL30.glGenVertexArrays();
				GL30.glBindVertexArray(vao);
			}

			//バッファバインド
			for (Attribute attribute : attributeMap.keySet()) {
				VBOModel vbo;
				if (hasTargets && target.containsKey(attribute)) {
					vbo = target.get(attribute);
				} else {
					vbo = attributeMap.get(attribute);
				}
				int index = attribute.index;
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

			shader.material(material);

			GL11.glDrawElements(drawMode, vertexCount, componentType, offset);

			if (GL30Supported) {
				GL30.glBindVertexArray(0);
			}
		}

		@Override
		public void dispose() {
			if (vao != -1)
				GL30.glDeleteVertexArrays(vao);
			for (VBOModel vbo : target.values()) {
				vbo.dispose();
			}
		}
	}

	private class VBOModel implements IDisposable {
		private int target = GL15.GL_ARRAY_BUFFER;

		private int vbo = -1;

		private ByteBuffer buffer;

		private final int count;
		private final int numComponents;
		private final int componentType;
		private final int byteStride;
		private final long byteOffset;

		public VBOModel(AccessorModel accessor) {
			BufferViewModel buf = accessor.getBufferViewModel();
			if (buf.getTarget() != null)
				target = buf.getTarget();
			buffer = buf.getBufferViewData();
			count = accessor.getCount();
			numComponents = accessor.getElementType().getNumComponents();
			componentType = accessor.getComponentType();
			byteStride = accessor.getByteStride();
			byteOffset = accessor.getByteOffset();
		}

		/**要注意*/
		void setData(ByteBuffer buf) {
			buffer = buf;
			uploadData();
		}

		void uploadData() {
			if (vbo != -1) {
				GL15.glBindBuffer(target, vbo);
				GL15.glBufferData(target, buffer, GL15.GL_STATIC_DRAW);
			}
		}

		void bind() {
			if (vbo == -1) {
				vbo = GL15.glGenBuffers();
				GL15.glBindBuffer(target, vbo);
				buffer.rewind();
				GL15.glBufferData(target, buffer, GL15.GL_STATIC_DRAW);
			}
			GL15.glBindBuffer(target, vbo);
		}

		void bindAttribPointer(int index) {
			GL20.glEnableVertexAttribArray(index);
			GL20.glVertexAttribPointer(index, numComponents, componentType, false, byteStride, byteOffset);
		}

		@Override
		public void dispose() {
			if (vbo != -1) {
				GL15.glDeleteBuffers(vbo);
			}
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
