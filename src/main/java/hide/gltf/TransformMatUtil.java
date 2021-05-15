package hide.gltf;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import de.javagl.jgltf.model.MathUtils;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TransformMatUtil {

	private static float[] cash = new float[16];
	static void computeJointMatrix(final NodeModel nodeModel, FloatBuffer fb) {
		final SkinModel skinModel = nodeModel.getSkinModel();

		float[] base = computeGlobalTransform(nodeModel, cash);
		MathUtils.invert4x4(base, base);

		float[] bindShapeMatrix = skinModel.getBindShapeMatrix(null);

		float[] jointMat = new float[16];

		List<NodeModel> jointList = skinModel.getJoints();
		for (int i = 0; i < jointList.size(); i++) {
			NodeModel joint = jointList.get(i);

			computeGlobalTransform(joint, jointMat);
			MathUtils.mul4x4(base, jointMat, jointMat);

			float[] inverseBindMatrix = skinModel.getInverseBindMatrix(i, null);
			MathUtils.mul4x4(jointMat, inverseBindMatrix, jointMat);

			MathUtils.mul4x4(jointMat, bindShapeMatrix, jointMat);
			fb.put(jointMat);
		}
	}

	static float[] computeGlobalTransform(NodeModel nodeModel, float[] result) {
		float[] localResult = result;
		NodeModel currentNode = nodeModel;
		MathUtils.setIdentity4x4(localResult);
		while (currentNode != null && currentNode.getParent() != null) {
			MathUtils.mul4x4(getLocalTransform(currentNode), localResult, localResult);
			currentNode = currentNode.getParent();
		}
		return localResult;
	}

	public static float[] getLocalTransform(NodeModel nodeModel) {


		if (nodeModel.getMatrix() != null) {
			return nodeModel.getMatrix();

		} else {
			System.out.println("ここは使わない予定");
			float[] s;
			float[] localResult = new float[16];
			MathUtils.setIdentity4x4(localResult);
			if (nodeModel.getTranslation() != null) {
				s = nodeModel.getTranslation();
				localResult[12] = s[0];
				localResult[13] = s[1];
				localResult[14] = s[2];
			}
			float[] m;
			if (nodeModel.getRotation() != null) {

				s = nodeModel.getRotation();
				m = new float[16];
				MathUtils.quaternionToMatrix4x4(s, m);
				MathUtils.mul4x4(localResult, m, localResult);
			}
			if (nodeModel.getScale() != null) {

				s = nodeModel.getScale();
				m = new float[16];
				MathUtils.setIdentity4x4(m);
				m[0] = s[0];
				m[5] = s[1];
				m[10] = s[2];
				m[15] = 1.0F;
				MathUtils.mul4x4(localResult, m, localResult);
			}
			return localResult;
		}
	}

	static Vector4f mul(Matrix4f mat, Vector4f pos) {
		Vector4f res = new Vector4f();
		res.x = mat.m00 * pos.x + mat.m01 * pos.y + mat.m02 * pos.z + mat.m03 * pos.w;
		res.y = mat.m10 * pos.x + mat.m11 * pos.y + mat.m12 * pos.z + mat.m13 * pos.w;
		res.z = mat.m20 * pos.x + mat.m21 * pos.y + mat.m22 * pos.z + mat.m23 * pos.w;
		res.w = mat.m30 * pos.x + mat.m31 * pos.y + mat.m32 * pos.z + mat.m33 * pos.w;
		return res;
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

	@SideOnly(Side.CLIENT)
	static void read(FloatBuffer fb, boolean transpose) {
		float[] data = new float[16];
		fb.get(data);
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
		System.out.println(String.format("pos[%.2f,%.2f,%.2f]", data[12], data[13], data[14]));
		System.out.println(String.format("scale[%.2f,%.2f,%.2f]",
				Math.sqrt(data[0] * data[0] + data[4] * data[4] + data[8] * data[8]), //X軸
				Math.sqrt(data[1] * data[1] + data[5] * data[5] + data[9] * data[9]), //Y軸
				Math.sqrt(data[2] * data[2] + data[6] * data[6] + data[10] * data[10])//Z軸
		));

	}

}
