package hide.model.gltf;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import hide.model.gltf.animation.Skin;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TransformMatUtil {

	private static float[] cash = new float[16];
	private static final ThreadLocal<float[]> TMP_MAT4x4_0 = ThreadLocal.withInitial(() -> new float[16]);
	private static final ThreadLocal<float[]> TMP_MAT4x4_1 = ThreadLocal.withInitial(() -> new float[16]);
	private static final ThreadLocal<float[]> TMP_MAT4x4_2 = ThreadLocal.withInitial(() -> new float[16]);

	static void computeJointMatrix(final HideNode nodeModel, FloatBuffer fb) {
		final Skin skin = nodeModel.getSkin();

		float[] base = computeGlobalTransform(nodeModel, TMP_MAT4x4_0.get());
		invert4x4(base, base);

		//float[] bindShapeMatrix = skin.getBindShapeMatrix(null);

		float[] jointMat = TMP_MAT4x4_1.get();

		HideNode[] jointList = skin.getJoints();
		for (int i = 0; i < jointList.length; i++) {
			HideNode joint = jointList[i];

			computeGlobalTransform(joint, jointMat);
			mul4x4(base, jointMat, jointMat);

			float[] inverseBindMatrix = skin.getInverseBindMatrix(i, TMP_MAT4x4_2.get());
			mul4x4(jointMat, inverseBindMatrix, jointMat);

			//mul4x4(jointMat, bindShapeMatrix, jointMat);
			fb.put(jointMat);
		}
	}

	static float[] computeGlobalTransform(HideNode nodeModel, float[] result) {
		float[] localResult = result;
		HideNode currentNode = nodeModel;
		setIdentity4x4(localResult);
		while (currentNode != null && currentNode.getParent() != null) {
			mul4x4(currentNode.getLocalMat(), localResult, localResult);
			currentNode = currentNode.getParent();
		}
		return localResult;
	}

	static Vector4f mul(Matrix4f mat, Vector4f pos) {
		Vector4f res = new Vector4f();
		res.x = mat.m00 * pos.x + mat.m01 * pos.y + mat.m02 * pos.z + mat.m03 * pos.w;
		res.y = mat.m10 * pos.x + mat.m11 * pos.y + mat.m12 * pos.z + mat.m13 * pos.w;
		res.z = mat.m20 * pos.x + mat.m21 * pos.y + mat.m22 * pos.z + mat.m23 * pos.w;
		res.w = mat.m30 * pos.x + mat.m31 * pos.y + mat.m32 * pos.z + mat.m33 * pos.w;
		return res;
	}

	static void mul4x4(FloatBuffer left, int left_i, FloatBuffer right, int right_i, FloatBuffer dest, int dest_i) {

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

	public static void mul4x4(float[] a, float[] b, float[] r) {
		float a00 = a[0];
		float a10 = a[1];
		float a20 = a[2];
		float a30 = a[3];
		float a01 = a[4];
		float a11 = a[5];
		float a21 = a[6];
		float a31 = a[7];
		float a02 = a[8];
		float a12 = a[9];
		float a22 = a[10];
		float a32 = a[11];
		float a03 = a[12];
		float a13 = a[13];
		float a23 = a[14];
		float a33 = a[15];

		float b00 = b[0];
		float b10 = b[1];
		float b20 = b[2];
		float b30 = b[3];
		float b01 = b[4];
		float b11 = b[5];
		float b21 = b[6];
		float b31 = b[7];
		float b02 = b[8];
		float b12 = b[9];
		float b22 = b[10];
		float b32 = b[11];
		float b03 = b[12];
		float b13 = b[13];
		float b23 = b[14];
		float b33 = b[15];

		float m00 = a00 * b00 + a01 * b10 + a02 * b20 + a03 * b30;
		float m01 = a00 * b01 + a01 * b11 + a02 * b21 + a03 * b31;
		float m02 = a00 * b02 + a01 * b12 + a02 * b22 + a03 * b32;
		float m03 = a00 * b03 + a01 * b13 + a02 * b23 + a03 * b33;

		float m10 = a10 * b00 + a11 * b10 + a12 * b20 + a13 * b30;
		float m11 = a10 * b01 + a11 * b11 + a12 * b21 + a13 * b31;
		float m12 = a10 * b02 + a11 * b12 + a12 * b22 + a13 * b32;
		float m13 = a10 * b03 + a11 * b13 + a12 * b23 + a13 * b33;

		float m20 = a20 * b00 + a21 * b10 + a22 * b20 + a23 * b30;
		float m21 = a20 * b01 + a21 * b11 + a22 * b21 + a23 * b31;
		float m22 = a20 * b02 + a21 * b12 + a22 * b22 + a23 * b32;
		float m23 = a20 * b03 + a21 * b13 + a22 * b23 + a23 * b33;

		float m30 = a30 * b00 + a31 * b10 + a32 * b20 + a33 * b30;
		float m31 = a30 * b01 + a31 * b11 + a32 * b21 + a33 * b31;
		float m32 = a30 * b02 + a31 * b12 + a32 * b22 + a33 * b32;
		float m33 = a30 * b03 + a31 * b13 + a32 * b23 + a33 * b33;

		r[0] = m00;
		r[1] = m10;
		r[2] = m20;
		r[3] = m30;
		r[4] = m01;
		r[5] = m11;
		r[6] = m21;
		r[7] = m31;
		r[8] = m02;
		r[9] = m12;
		r[10] = m22;
		r[11] = m32;
		r[12] = m03;
		r[13] = m13;
		r[14] = m23;
		r[15] = m33;
	}

	public static void quaternionToMatrix4x4(float[] q, float[] m) {
		float invLength = 1.0F / (float) Math.sqrt(dot(q, q));

		float qx = q[0] * invLength;
		float qy = q[1] * invLength;
		float qz = q[2] * invLength;
		float qw = q[3] * invLength;
		m[0] = 1.0F - 2.0F * qy * qy - 2.0F * qz * qz;
		m[1] = 2.0F * (qx * qy + qw * qz);
		m[2] = 2.0F * (qx * qz - qw * qy);
		m[3] = 0.0F;
		m[4] = 2.0F * (qx * qy - qw * qz);
		m[5] = 1.0F - 2.0F * qx * qx - 2.0F * qz * qz;
		m[6] = 2.0F * (qy * qz + qw * qx);
		m[7] = 0.0F;
		m[8] = 2.0F * (qx * qz + qw * qy);
		m[9] = 2.0F * (qy * qz - qw * qx);
		m[10] = 1.0F - 2.0F * qx * qx - 2.0F * qy * qy;
		m[11] = 0.0F;
		m[12] = 0.0F;
		m[13] = 0.0F;
		m[14] = 0.0F;
		m[15] = 1.0F;
	}

	public static float[] setIdentity4x4(float[] m) {
		Arrays.fill(m, 0.0F);
		m[0] = 1.0F;
		m[5] = 1.0F;
		m[10] = 1.0F;
		m[15] = 1.0F;
		return m;
	}

	public static void invert4x4(float[] m, float[] inv) {
		float m0 = m[0];
		float m1 = m[1];
		float m2 = m[2];
		float m3 = m[3];
		float m4 = m[4];
		float m5 = m[5];
		float m6 = m[6];
		float m7 = m[7];
		float m8 = m[8];
		float m9 = m[9];
		float mA = m[10];
		float mB = m[11];
		float mC = m[12];
		float mD = m[13];
		float mE = m[14];
		float mF = m[15];

		inv[0] = m5 * mA * mF - m5 * mB * mE - m9 * m6 * mF + m9 * m7 * mE + mD * m6 * mB - mD * m7 * mA;

		inv[4] = -m4 * mA * mF + m4 * mB * mE + m8 * m6 * mF - m8 * m7 * mE - mC * m6 * mB + mC * m7 * mA;

		inv[8] = m4 * m9 * mF - m4 * mB * mD - m8 * m5 * mF + m8 * m7 * mD + mC * m5 * mB - mC * m7 * m9;

		inv[12] = -m4 * m9 * mE + m4 * mA * mD + m8 * m5 * mE - m8 * m6 * mD - mC * m5 * mA + mC * m6 * m9;

		inv[1] = -m1 * mA * mF + m1 * mB * mE + m9 * m2 * mF - m9 * m3 * mE - mD * m2 * mB + mD * m3 * mA;

		inv[5] = m0 * mA * mF - m0 * mB * mE - m8 * m2 * mF + m8 * m3 * mE + mC * m2 * mB - mC * m3 * mA;

		inv[9] = -m0 * m9 * mF + m0 * mB * mD + m8 * m1 * mF - m8 * m3 * mD - mC * m1 * mB + mC * m3 * m9;

		inv[13] = m0 * m9 * mE - m0 * mA * mD - m8 * m1 * mE + m8 * m2 * mD + mC * m1 * mA - mC * m2 * m9;

		inv[2] = m1 * m6 * mF - m1 * m7 * mE - m5 * m2 * mF + m5 * m3 * mE + mD * m2 * m7 - mD * m3 * m6;

		inv[6] = -m0 * m6 * mF + m0 * m7 * mE + m4 * m2 * mF - m4 * m3 * mE - mC * m2 * m7 + mC * m3 * m6;

		inv[10] = m0 * m5 * mF - m0 * m7 * mD - m4 * m1 * mF + m4 * m3 * mD + mC * m1 * m7 - mC * m3 * m5;

		inv[14] = -m0 * m5 * mE + m0 * m6 * mD + m4 * m1 * mE - m4 * m2 * mD - mC * m1 * m6 + mC * m2 * m5;

		inv[3] = -m1 * m6 * mB + m1 * m7 * mA + m5 * m2 * mB - m5 * m3 * mA - m9 * m2 * m7 + m9 * m3 * m6;

		inv[7] = m0 * m6 * mB - m0 * m7 * mA - m4 * m2 * mB + m4 * m3 * mA + m8 * m2 * m7 - m8 * m3 * m6;

		inv[11] = -m0 * m5 * mB + m0 * m7 * m9 + m4 * m1 * mB - m4 * m3 * m9 - m8 * m1 * m7 + m8 * m3 * m5;

		inv[15] = m0 * m5 * mA - m0 * m6 * m9 - m4 * m1 * mA + m4 * m2 * m9 + m8 * m1 * m6 - m8 * m2 * m5;

		float det = m0 * inv[0] + m1 * inv[4] + m2 * inv[8] + m3 * inv[12];
		if (Math.abs(det) <= 1.0E-8F) {
			setIdentity4x4(inv);
		}
	}

	private static float dot(float[] a, float[] b) {
		float sum = 0.0F;
		for (int i = 0; i < a.length; ++i) {

			sum += a[i] * b[i];
		}
		return sum;
	}

	@SideOnly(Side.CLIENT)
	static void read(FloatBuffer fb, boolean transpose) {
		float[] data = new float[16];
		fb.get(data);
		read(data, transpose);
	}

	@SideOnly(Side.CLIENT)
	static void read(float[] data, boolean transpose) {
		if (transpose) {
			Matrix4f mat = new net.minecraft.client.renderer.Matrix4f(data);
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
		System.out.println(String.format("[%.2f,%.2f,%.2f,%.2f]", data[0], data[1], data[2], data[3]));
		System.out.println(String.format("[%.2f,%.2f,%.2f,%.2f]", data[4], data[5], data[6], data[7]));
		System.out.println(String.format("[%.2f,%.2f,%.2f,%.2f]", data[8], data[9], data[10], data[11]));
		System.out.println(String.format("[%.2f,%.2f,%.2f,%.2f]", data[12], data[13], data[14], data[15]));

		System.out.println(String.format("pos[%.2f,%.2f,%.2f]", data[12], data[13], data[14]));
		System.out.println(String.format("scale[%.2f,%.2f,%.2f]",
				Math.sqrt(data[0] * data[0] + data[4] * data[4] + data[8] * data[8]), //X軸
				Math.sqrt(data[1] * data[1] + data[5] * data[5] + data[9] * data[9]), //Y軸
				Math.sqrt(data[2] * data[2] + data[6] * data[6] + data[10] * data[10])//Z軸
		));

	}
}
