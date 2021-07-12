package hide.model.gltf;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import com.google.gson.annotations.SerializedName;

import de.javagl.jgltf.model.AccessorModel;
import hide.model.gltf.base.Accessor;
import hide.model.gltf.base.Material;

public class Mesh {

	MeshPrimitives[] primitives;
	float[] weights;

	public void register(GltfLoader loader) {

	}

	public static class MeshPrimitives {
		private Map<Attribute, Integer> attributes;
		@SerializedName("indices")
		private int indicesIndex;
		@SerializedName("material")
		private int materialIndex;
		private Mode mode = Mode.TRIANGLES;
		private Map<Attribute, Integer>[] targets = (Map<Attribute, Integer>[]) new Object[0];

		transient private Material material;
		transient private int vertexCount;
		transient private int offset;
		transient private int componentType;
		transient private int drawMode;

		transient private boolean hasTarget;

		public void register(GltfLoader loader) {
			material = loader.getMaterial(materialIndex);
			Accessor indices = loader.getAccessor(indicesIndex);
			vertexCount = indices.getCount();
			componentType = indices.getComponentType().gl;
			offset = indices.getByteOffset();

			hasTarget = targets.length != 0;

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

		public enum Mode {
			@SerializedName("0")
			POINTS(0), @SerializedName("1")
			LINES(1), @SerializedName("2")
			LINE_LOOP(2), @SerializedName("3")
			LINE_STRIP(3), @SerializedName("4")
			TRIANGLES(4), @SerializedName("5")
			TRIANGLE_STRIP(5), @SerializedName("6")
			TRIANGLE_FAN(6);

			public final int gl;

			Mode(final int gl) {
				this.gl = gl;
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
}
