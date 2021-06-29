package hide.model.gltf.base;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

public class BufferView {
	private int byteLength = 0;
	private int byteOffset = 0;
	private int byteStride = 0;
	private int target = GL15.GL_ARRAY_BUFFER;

	transient private ByteBuffer data;

	public void slice(ByteBuffer from) {
		 from.slice().position(byteOffset)
			.limit(byteOffset + byteLength);
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getByteLength() {
		return byteLength;
	}

	public ByteBuffer getData() {
		return data;
	}
}
