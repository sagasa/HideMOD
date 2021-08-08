package hide.model.gltf;

import com.google.gson.annotations.SerializedName;

import hide.model.impl.AccessorImpl;
import hide.model.impl.ISkin;
import hide.model.impl.NodeImpl;

class Skin implements ISkin {
	@SerializedName("inverseBindMatrices")
	private int inverseBindMatricesIndex;
	@SerializedName("joints")
	private int[] jointsIndex;

	transient private AccessorImpl inverseBindMatrices;
	transient private NodeImpl[] joints;

	public Skin register(GltfLoader loader) {
		inverseBindMatrices = loader.getAccessor(inverseBindMatricesIndex);
		joints = new NodeImpl[jointsIndex.length];
		for (int i = 0; i < joints.length; i++) {
			joints[i] = loader.getNode(jointsIndex[i]);

		}
		return this;
	}

	@Override
	public AccessorImpl getInverseBindMatrices() {
		return inverseBindMatrices;
	}

	@Override
	public NodeImpl[] getJoints() {
		return joints;
	}
}
