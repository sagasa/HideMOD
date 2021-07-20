package hide.model.gltf.animation;

import java.nio.ByteBuffer;

import com.google.gson.annotations.SerializedName;

import hide.model.gltf.GltfLoader;
import hide.model.gltf.HideNode;
import hide.model.gltf.base.Accessor;

public class Skin {
	@SerializedName("inverseBindMatrices")
	private int inverseBindMatricesIndex;
	@SerializedName("joints")
	private int[] jointsIndex;

	transient private Accessor inverseBindMatrices;
	transient private HideNode[] joints;

	public Skin register(GltfLoader loader) {
		inverseBindMatrices = loader.getAccessor(inverseBindMatricesIndex);
		joints = new HideNode[jointsIndex.length];
		for (int i = 0; i < joints.length; i++) {
			joints[i] = loader.getNode(jointsIndex[i]);

		}

		return this;
	}

	public Accessor getInverseBindMatrices() {
		return inverseBindMatrices;
	}

	public HideNode[] getJoints() {
		return joints;
	}

	public float[] getInverseBindMatrix(int index, float[] res) {
		if (res != null)
			res = new float[16];
		ByteBuffer buf = inverseBindMatrices.getBuffer();
		for (int i = 0; i < 16; i++) {
			res[i] = buf.getFloat(inverseBindMatrices.getByteIndex(index, i));
		}
		return res;
	}
}
