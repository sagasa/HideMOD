package hide.model.impl;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.google.gson.annotations.SerializedName;

public class AccessorImpl implements IDisposable {

	protected int byteOffset;
	protected ComponentType componentType;
	@SerializedName("type")
	protected ElementType elementType;
	protected int count;
	protected Number[] max;
	protected Number[] min;

	transient protected BufferViewImpl buffer;

	/**新しくメモリを確保して同じプロパティのアクセサを作成*/
	public AccessorImpl copy() {
		AccessorImpl res = new AccessorImpl();
		res.byteOffset = byteOffset;
		res.componentType = componentType;
		res.elementType = elementType;
		res.count = count;
		res.max = max;
		res.min = min;
		res.buffer = buffer.copy();
		return res;
	}

	public int getCount() {
		return count;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public ElementType getElementType() {
		return elementType;
	}

	public Number[] getMax() {
		return max;
	}

	public Number[] getMin() {
		return min;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getByteIndex(int elementIndex, int componentIndex) {
		return buffer.getByteOffset() + elementIndex * elementType.size * componentType.size + componentIndex * componentType.size;
	}

	public void setTarget(int target) {
		buffer.setTarget(target);
	}

	public ByteBuffer getBuffer() {
		return buffer.getBuffer();
	}

	public void uploadData() {
		buffer.uploadData();
	}

	public void bind() {
		buffer.bind();
	}

	public void bindAttribPointer(int index) {
		bind();
		GL20.glEnableVertexAttribArray(index);
		GL20.glVertexAttribPointer(index, elementType.size, componentType.gl, false, buffer.getByteStride(), byteOffset);
	}

	@Override
	public void dispose() {
		buffer.dispose();
	}

	public void writeAsFloat() {
		ByteBuffer buf = getBuffer();
		System.out.println("print acceser data count = " + count);
		for (int i = 0; i < count; i++) {
			System.out.print("[");
			for (int j = 0; j < getElementType().size; j++) {
				if (j != 0)
					System.out.print(", ");
				System.out.print(getComponentType().getValue(buf, getByteIndex(i, j)));
			}
			System.out.print("]");
		}
		System.out.println();
	}

	public enum ComponentType {
		@SerializedName("5120")
		BYTE(1, GL11.GL_BYTE, (buf, index) -> String.valueOf(buf.get(index))), @SerializedName("5121")
		UNSIGNED_BYTE(1, GL11.GL_UNSIGNED_BYTE, (buf, index) -> String.valueOf(buf.get() & 0xFF)), @SerializedName("5122")
		SHORT(2, GL11.GL_SHORT, (buf, index) -> String.valueOf(buf.getShort(index))), @SerializedName("5123")
		UNSIGNED_SHORT(2, GL11.GL_UNSIGNED_SHORT, (buf, index) -> String.valueOf(buf.getShort(index) & 0xFFFF)), @SerializedName("5124")
		INT(4, GL11.GL_INT, (buf, index) -> String.valueOf(buf.getInt(index))), @SerializedName("5125")
		UNSIGNED_INT(4, GL11.GL_UNSIGNED_INT, (buf, index) -> String.valueOf((long) buf.getInt(index) & 0xFFFFFFFF)), @SerializedName("5126")
		FLOAT(4, GL11.GL_FLOAT, (buf, index) -> String.valueOf(buf.getFloat(index)));

		public final int size;
		public final int gl;

		private final BiFunction<ByteBuffer, Integer, String> getter;

		ComponentType(final int size, final int gl, BiFunction<ByteBuffer, Integer, String> getter) {
			this.size = size;
			this.gl = gl;
			this.getter = getter;
		}

		public String getValue(ByteBuffer buf, int index) {
			return getter.apply(buf, index);
		}
	}

	public enum ElementType {
		SCALAR(1), VEC2(2), VEC3(3), VEC4(4), MAT2(4), MAT3(9), MAT4(16);

		public final int size;

		ElementType(final int size) {
			this.size = size;
		}
	}
}
