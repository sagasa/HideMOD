package hide.model.impl;

import java.nio.ByteBuffer;

public interface ISkin {

	AccessorImpl getInverseBindMatrices();

	NodeImpl[] getJoints();

	default float[] getInverseBindMatrix(int index, float[] res) {

		if (res != null)
			res = new float[16];
		ByteBuffer buf = getInverseBindMatrices().getBuffer();
		for (int i = 0; i < 16; i++) {
			res[i] = buf.getFloat(getInverseBindMatrices().getByteIndex(index, i));
		}
		return res;
	}

}
