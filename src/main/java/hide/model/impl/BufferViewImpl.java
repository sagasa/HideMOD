package hide.model.impl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

public class BufferViewImpl implements IDisposable {
	protected int byteLength = 0;
	protected int byteOffset = 0;
	protected int byteStride = 0;
	protected int target = GL15.GL_ARRAY_BUFFER;

	transient protected ByteBuffer buffer;

	/**新しくメモリを確保してコピーを作製*/
	public BufferViewImpl copy() {
		BufferViewImpl res = new BufferViewImpl();
		res.byteLength = byteLength;
		res.byteOffset = byteOffset;
		res.byteStride = byteStride;
		res.target = target;
		res.buffer = buffer.duplicate();
		return res;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	transient private int vbo = -1;

	public void uploadData() {
		if (vbo != -1) {
			GL15.glBindBuffer(target, vbo);
			GL15.glBufferData(target, buffer, GL15.GL_STATIC_DRAW);
		}
	}

	public void bind() {
		if (vbo == -1) {
			vbo = GL15.glGenBuffers();
			GL15.glBindBuffer(target, vbo);
			GL15.glBufferData(target, buffer, GL15.GL_STATIC_DRAW);

		} else
			GL15.glBindBuffer(target, vbo);
	}

	@Override
	public void dispose() {
		if (vbo != -1) {
			GL15.glDeleteBuffers(vbo);
		}
	}

	public int getByteStride() {
		return byteStride;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getByteLength() {
		return byteLength;
	}

	public void setTarget(int target) {
		this.target = target;
	}

}
