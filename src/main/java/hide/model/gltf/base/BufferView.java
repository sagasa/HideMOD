package hide.model.gltf.base;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

public class BufferView implements IDisposable {
	private int byteLength = 0;
	private int byteOffset = 0;
	private int byteStride = 0;
	private int target = GL15.GL_ARRAY_BUFFER;

	transient private ByteBuffer buffer;

	public void slice(ByteBuffer from) {
		from.slice().position(byteOffset)
				.limit(byteOffset + byteLength);
	}

	/**新しくメモリを確保してコピーを作製*/
	public BufferView copy() {
		BufferView res = new BufferView();
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
			buffer.rewind();
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

	public void setTarget(int target) {
		this.target = target;
	}
}
