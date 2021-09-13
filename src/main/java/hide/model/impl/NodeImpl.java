package hide.model.impl;

import static hide.model.util.TransformMatUtil.*;

import java.nio.FloatBuffer;
import java.util.Arrays;

import handler.client.HideGunRender;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import hide.model.util.BufferUtil;
import hide.model.util.HideShader;
import hide.model.util.TransformMatUtil;
import net.minecraft.client.renderer.GlStateManager;

public class NodeImpl implements IDisposable {
	protected String name;

	transient protected MeshImpl mesh;
	transient protected SkinImpl skin;

	transient private boolean hasSkin, hasMesh, hasMorphing, isJoint;
	transient private HideShader shader;

	void postInit() {
		hasSkin = skin != null;
		hasMesh = mesh != null;
		hasMorphing = hasMesh && mesh.hasWeights();

		if (matrix == null) {
			matrix = new float[16];
		} else {
			isValidLocalMat = true;
		}
		if (hasMesh) {
			shader = hasSkin ? HideShader.SKIN_SHADER : HideShader.BASE_SHADER;

			System.out.println("post init in " + shader);
			mesh.setShader(shader);
			mesh.postInit();
		}
		if (hasSkin) {

			for (NodeImpl nodeImpl : skin.getJoints()) {
				nodeImpl.isJoint = true;
			}
		}
	}

	public boolean isJoint() {
		return isJoint;
	}

	public boolean hasMorphing() {
		return hasMorphing;
	}

	public boolean hasSkin() {
		return hasSkin;
	}

	public String getName() {
		return name;
	}

	/**子の中にmeshがある場合もtrue*/
	public boolean hasMesh() {
		if (hasMesh)
			return true;
		for (NodeImpl node : children) {
			if (node.hasMesh())
				return true;
		}
		return false;
	}

	public SkinImpl getSkin() {
		return skin;
	}

	transient protected NodeImpl[] children = new NodeImpl[0];
	transient protected NodeImpl parent = null;

	public NodeImpl getParent() {
		return parent;
	}

	public NodeImpl[] getChildren() {
		return children;
	}

	private float[] matrix;
	private float[] rotation = new float[] { 0.707107f, 0, 0, 0.707107f};
	private float[] scale = new float[] { 1, 1, 1 };
	private float[] translation = new float[3];
	private float[] weights;
	transient private float[] globalMatrix = new float[16];
	transient private boolean isValidLocalMat = false;
	transient private boolean isValidGlobalMat = false;
	transient private boolean isValidWeight = false;

	public void mulRotation(float[] value) {
		System.out.println("Rotate pre" + ArrayUtils.toString(rotation));
		TransformMatUtil.quatMul(rotation, value, rotation);
		isValidLocalMat = false;
		markInvalidGlobalMat();
		System.out.println("Rotate " + ArrayUtils.toString(rotation));
	}

	public void setRotation(float[] value) {
		if (!Arrays.equals(rotation, value)) {
			rotation = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
			//System.out.println(name+" setRotation "+ArrayUtils.toString(value));
		}
	}

	public void setScale(float[] value) {
		if (!Arrays.equals(scale, value)) {
			scale = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
			//System.out.println(name+" setScale "+ArrayUtils.toString(value));
		}
	}

	public void setTranslation(float[] value) {
		if (!Arrays.equals(translation, value)) {
			translation = value;
			isValidLocalMat = false;
			markInvalidGlobalMat();
			//System.out.println(name+" setTranslation "+ArrayUtils.toString(value));
		}
	}

	public void setWeights(float[] value) {
		if (!Arrays.equals(weights, value)) {
			weights = value;
			isValidWeight = false;
		}
	}

	private void markInvalidGlobalMat() {
		isValidGlobalMat = false;
		for (NodeImpl child : children) {
			child.markInvalidGlobalMat();
		}
	}

	private static final ThreadLocal<float[]> TMP_MAT4x4 = ThreadLocal.withInitial(() -> new float[16]);

	public float[] getLocalMat() {
		if (!isValidLocalMat) {
			setIdentity4x4(matrix);

			float[] s;

			s = this.translation;
			this.matrix[12] = s[0];
			this.matrix[13] = s[1];
			this.matrix[14] = s[2];

			float[] m;
			s = this.rotation;
			m = TMP_MAT4x4.get();
			quaternionToMatrix4x4(s, m);
			mul4x4(matrix, m, matrix);

			s = this.scale;
			setIdentity4x4(m);
			m[0] = s[0];
			m[5] = s[1];
			m[10] = s[2];
			m[15] = 1.0F;
			mul4x4(matrix, m, matrix);
			isValidLocalMat = true;
		}
		return matrix;
	}

	public float[] getGlobalMat() {
		if (!isValidGlobalMat) {
			NodeImpl currentNode = this;
			setIdentity4x4(globalMatrix);
			while (currentNode != null) {
				mul4x4(currentNode.getLocalMat(), globalMatrix, globalMatrix);
				currentNode = currentNode.getParent();
			}
			isValidGlobalMat = true;
		}
		return globalMatrix;
	}

	public void renderSkin() {
		if (hasSkin) {
			shader.use();

			GL20.glUniformMatrix4(HideShader.SKIN_BONE_MAT_INDEX, false, skin.computeJointMatrix(this));

			mesh.render();

			GL20.glUseProgram(0);
		}
	}

	private static final ThreadLocal<FloatBuffer> TMP_FB16 = ThreadLocal.withInitial(() -> BufferUtil.createFloatBuffer(16));

	public void render(boolean debug) {
		ModelImpl.profiler.startSection("hide.render");

		ModelImpl.profiler.endStartSection("hide.render.calcBone");
		//モーフィング
		if (hasMorphing() && !isValidWeight) {
			mesh.calcWeight(weights);
		}

		GL11.glPushMatrix();

		FloatBuffer fb = TMP_FB16.get();
		fb.rewind();
		fb.put(getLocalMat());
		fb.rewind();
		GL11.glMultMatrix(fb);

		if (!hasMesh && debug) {
			ModelImpl.profiler.endStartSection("hide.render.debug");
			HideGunRender.debugDot();
		}

		ModelImpl.profiler.endStartSection("hide.render.draw");
		if (hasMesh && !hasSkin) {
			shader.use();
			mesh.render();
		}
		if (hasMesh) {
			renderSkin();
		}

		GL20.glUseProgram(0);
		ModelImpl.profiler.endSection();
		for (NodeImpl node : children) {
			node.render(debug);
		}

		GL11.glPopMatrix();

	}

	@Override
	public String toString() {
		return String.format("NodeImpl[Name=%s]", name);
	}

	@Override
	public void dispose() {
		mesh.dispose();
	}

}
