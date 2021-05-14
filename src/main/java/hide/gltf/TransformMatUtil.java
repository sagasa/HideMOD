package hide.gltf;

import java.nio.FloatBuffer;
import java.util.List;

import de.javagl.jgltf.model.MathUtils;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;

public class TransformMatUtil {
	static void computeJointMatrix(final NodeModel nodeModel, FloatBuffer fb) {
		final SkinModel skinModel = nodeModel.getSkinModel();

		float[] base = computeGlobalTransform(nodeModel, new float[16]);
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

		float[] s;
		if (nodeModel.getMatrix() != null) {
			return nodeModel.getMatrix();

		} else {
			System.out.println("ここは使わない予定");
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
}
