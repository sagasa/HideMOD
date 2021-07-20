package hide.model.gltf.base;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.google.gson.annotations.SerializedName;

import hide.model.gltf.GltfLoader;
import hide.model.gltf.Model;
import hide.model.gltf.Model.HideShader;

public class Mesh implements IDisposable {
	private static boolean GL30Supported = GLContext.getCapabilities().OpenGL30;

	private MeshPrimitives[] primitives;
	private float[] weights = ArrayUtils.EMPTY_FLOAT_ARRAY;

	public boolean hasWeights() {
		return weights.length != 0;
	}

	public void calcWeight(float[] weight) {
		this.weights = weight;
		for (MeshPrimitives meshPrimitives : primitives) {
			meshPrimitives.calcWeight(weight);
		}
	}

	public Mesh register(GltfLoader loader) {
		for (MeshPrimitives meshPrimitives : primitives) {
			meshPrimitives.register(loader);
		}
		return this;
	}

	public void render() {
		for (MeshPrimitives meshPrimitives : primitives) {
			meshPrimitives.render();
		}

	}

	public static class MeshPrimitives implements IDisposable {
		@SerializedName("attributes")
		private Map<String, Integer> attributeIndex;
		@SerializedName("indices")
		private int indicesIndex;
		@SerializedName("material")
		private int materialIndex = -1;
		private Mode mode = Mode.TRIANGLES;
		@SerializedName("targets")
		transient private Map<Attribute, Integer>[] targetsIndex;

		transient private int vao = -1;
		transient private HideShader shader;
		transient private Material material;
		transient private Accessor indices;
		transient private Map<Attribute, Accessor> attributes;
		transient private List<Map<Attribute, Accessor>> targets;
		/**モーフィングがある場合の頂点データ*///TODO シェーダーでやりたい
		transient private Map<Attribute, Accessor> target = Collections.emptyMap();
		transient private int vertexCount;
		transient private int offset;
		transient private int componentType;

		transient private boolean hasTarget;

		private void register(GltfLoader loader) {

			material = loader.getMaterial(materialIndex);
			indices = loader.getAccessor(indicesIndex);
			attributes = new EnumMap<>(Attribute.class);
			attributeIndex.forEach((att, index) -> {
				Attribute attribute = Attribute.valueOf(att);
				if (attribute != null&&attribute == Attribute.POSITION)
					attributes.put(attribute, loader.getAccessor(index));
			});

			vertexCount = indices.getCount();
			componentType = indices.getComponentType().gl;
			offset = indices.getByteOffset();

			hasTarget = targetsIndex != null && targetsIndex.length != 0;

			//モーフィングがあるなら
			if (hasTarget) {
				targets = new ArrayList<>();
				target = new EnumMap<>(Attribute.class);
				for (Map<Attribute, Integer> src : targetsIndex) {
					Map<Attribute, Accessor> map = new EnumMap<>(Attribute.class);
					for (Entry<Attribute, Integer> entry : src.entrySet()) {
						map.put(entry.getKey(), loader.getAccessor(entry.getValue()));
					}
					targets.add(map);
				}
				for (Entry<Attribute, Accessor> entry : attributes.entrySet()) {
					target.put(entry.getKey(), entry.getValue().copy());
				}
			}

			indices.setTarget(GL15.GL_ELEMENT_ARRAY_BUFFER);
		}

		void calcWeight(float[] weight) {
			for (Entry<Attribute, Accessor> entry : target.entrySet()) {
				Attribute attribute = entry.getKey();
				Accessor outAccessor = entry.getValue();
				ByteBuffer out = outAccessor.getBuffer();
				for (int j = 0; j < out.limit(); j += outAccessor.getComponentType().size) {
					//float f = weight[j];
					float sum = attributes.get(attribute).getBuffer().getFloat(j);
					for (int i = 0; i < weight.length; i++) {
						//Model.profiler.endStartSection("hide.updateWeight.sum");
						Map<Attribute, Accessor> map = targets.get(i);
						if (map.containsKey(attribute))
							sum += map.get(attribute).getBuffer().getFloat(j) * weight[i];
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
			for (Attribute attribute : attributes.keySet()) {
				Accessor vbo;
				if (hasTarget) {
					vbo = target.get(attribute);
				} else {
					vbo = attributes.get(attribute);
				}
				int index = attribute.index;
				vbo.bindAttribPointer(index);
				System.out.println(attribute+ " "+index);
				vbo.writeAsFloat();
			}

			indices.bind();
			System.out.println("indices");
			indices.writeAsFloat();
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

			Model.BASE_SHADER.material(material);
			//System.out.println("render "+vertexCount);
			GL11.glDrawElements(mode.gl, vertexCount, componentType, offset);

			if (GL30Supported) {
				GL30.glBindVertexArray(0);
			}
		}

		@Override
		public void dispose() {
			if (vao != -1)
				GL30.glDeleteVertexArrays(vao);
			indices.dispose();
			for (Accessor accessor : target.values()) {
				accessor.dispose();
			}
			for (Accessor accessor : attributes.values()) {
				accessor.dispose();
			}
			for (Map<Attribute, Accessor> map : targets) {
				for (Accessor accessor : map.values()) {
					accessor.dispose();
				}
			}
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

	@Override
	public void dispose() {
		for (MeshPrimitives p : primitives) {
			p.dispose();
		}

	}
}
