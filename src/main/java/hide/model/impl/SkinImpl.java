package hide.model.impl;

import static hide.model.util.TransformMatUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import hide.model.util.BufferUtil;

public class SkinImpl {

	transient protected AccessorImpl inverseBindMatrices;
	transient protected NodeImpl[] joints;

	transient private FloatBuffer boneMat;

	public void postInit() {
		boneMat = BufferUtil.createFloatBuffer(inverseBindMatrices.getElementType().size * inverseBindMatrices.getCount());
	}

	private static final ThreadLocal<float[]> TMP_MAT4x4_0 = ThreadLocal.withInitial(() -> new float[16]);
	private static final ThreadLocal<float[]> TMP_MAT4x4_1 = ThreadLocal.withInitial(() -> new float[16]);
	private static final ThreadLocal<float[]> TMP_MAT4x4_2 = ThreadLocal.withInitial(() -> new float[16]);

	public FloatBuffer computeJointMatrix(NodeImpl root) {

		boneMat.rewind();

		float[] base = TMP_MAT4x4_0.get();
		setIdentity4x4(base);
		invert4x4(root.getGlobalMat(), base);

		//float[] bindShapeMatrix = skin.getBindShapeMatrix(null);

		float[] jointMat = TMP_MAT4x4_1.get();

		for (int i = 0; i < joints.length; i++) {
			NodeImpl joint = joints[i];

			mul4x4(base, joint.getGlobalMat(), jointMat);

			float[] inverseBindMatrix = getInverseBindMatrix(i, TMP_MAT4x4_2.get());
			mul4x4(jointMat, inverseBindMatrix, jointMat);

			//mul4x4(jointMat, bindShapeMatrix, jointMat);
			boneMat.put(jointMat);
		}

		boneMat.rewind();
		return boneMat;
	}

	public AccessorImpl getInverseBindMatrices() {
		return inverseBindMatrices;
	}

	public NodeImpl[] getJoints() {
		return joints;
	}

	public float[] getInverseBindMatrix(int index, float[] res) {

		if (res != null)
			res = new float[16];
		ByteBuffer buf = getInverseBindMatrices().getBuffer();
		for (int i = 0; i < 16; i++) {
			res[i] = buf.getFloat(getInverseBindMatrices().getByteIndex(index, i));
		}
		return res;
	}

	protected static class Bone {
		protected Bone parent;
		protected NodeImpl node;
	}
}
