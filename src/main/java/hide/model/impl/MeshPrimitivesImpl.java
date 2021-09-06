package hide.model.impl;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import hide.model.impl.MeshImpl.Attribute;
import hide.model.impl.MeshImpl.Mode;
import hide.model.util.HideShader;
import hide.model.util.TransformMatUtil;
import hide.opengl.ServerRenderContext;

public abstract class MeshPrimitivesImpl implements IDisposable {

	private static boolean GL30Supported = ServerRenderContext.SUPPORT_CONTEXT && GLContext.getCapabilities().OpenGL30;

	private Mode mode = Mode.TRIANGLES;

	transient private int vao = -1;
	transient private HideShader shader;
	transient protected IMaterial material;
	transient protected AccessorImpl indices;
	transient protected Map<Attribute, AccessorImpl> attributes = new EnumMap<>(Attribute.class);;
	transient protected List<Map<Attribute, AccessorImpl>> targets = Collections.EMPTY_LIST;
	/**モーフィングがある場合の頂点データ*///TODO シェーダーでやりたい
	transient protected Map<Attribute, AccessorImpl> target = Collections.EMPTY_MAP;

	void postInit() {
		if (!targets.isEmpty())
			for (Entry<Attribute, AccessorImpl> entry : attributes.entrySet()) {
				target.put(entry.getKey(), entry.getValue().copy());
			}
		indices.setTarget(GL15.GL_ELEMENT_ARRAY_BUFFER);
	}

	public void setShader(HideShader shader) {
		this.shader = shader;
	}

	public void setMaterial(IMaterial mat) {
		material = mat;
		System.out.println("setMaterial " + mat);
	}

	public void calcWeight(float[] weight) {
		for (Entry<Attribute, AccessorImpl> entry : target.entrySet()) {
			Attribute attribute = entry.getKey();
			AccessorImpl outAccessor = entry.getValue();
			ByteBuffer out = outAccessor.getBuffer();
			for (int j = 0; j < out.limit(); j += outAccessor.getComponentType().size) {
				//float f = weight[j];
				float sum = attributes.get(attribute).getBuffer().getFloat(j);
				for (int i = 0; i < weight.length; i++) {
					//Model.profiler.endStartSection("hide.updateWeight.sum");
					Map<Attribute, AccessorImpl> map = targets.get(i);
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

		//TransformMatUtil.checkGLError("pre bind");
		//バッファバインド
		for (Attribute attribute : attributes.keySet()) {
			AccessorImpl vbo;
			if (!target.isEmpty()) {
				vbo = target.get(attribute);
			} else {
				vbo = attributes.get(attribute);
			}
			int index = attribute.index;
			vbo.bindAttribPointer(index);
			//System.out.println(attribute + " " + index);
			//vbo.writeAsFloat();
		}

		indices.bind();
		//System.out.println("indices");
		//indices.writeAsFloat();
		if (GL30Supported) {
			GL30.glBindVertexArray(0);
		}
	}

	public void render() {
		TransformMatUtil.checkGLError("pre bind VAO");
		if (GL30Supported) {
			if (vao == -1)
				bind();
			GL30.glBindVertexArray(vao);
			TransformMatUtil.checkGLError("bind VAO");
		} else {
			bind();
		}

		shader.material(material);

		TransformMatUtil.checkGLError("bind material");

		//System.out.println("render "+vertexCount);
		GL11.glDrawElements(mode.gl, indices.getCount(), indices.getComponentType().gl, indices.getByteOffset());

		TransformMatUtil.checkGLError("drawElements");

		if (GL30Supported) {
			GL30.glBindVertexArray(0);
		}
	}

	@Override
	public void dispose() {
		if (vao != -1)
			GL30.glDeleteVertexArrays(vao);
		indices.dispose();
		for (AccessorImpl accessor : target.values()) {
			accessor.dispose();
		}
		for (AccessorImpl accessor : attributes.values()) {
			accessor.dispose();
		}
		for (Map<Attribute, AccessorImpl> map : targets) {
			for (AccessorImpl accessor : map.values()) {
				accessor.dispose();
			}
		}
	}

}
