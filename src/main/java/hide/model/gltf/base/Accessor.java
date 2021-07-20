package hide.model.gltf.base;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.BiFunction;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.google.gson.annotations.SerializedName;

public class Accessor implements IDisposable {

	private int bufferView;
	private int byteOffset;
	private ComponentType componentType;
	@SerializedName("type")
	private ElementType elementType;
	private int count;
	public Number[] max;
	public Number[] min;

	transient private BufferView buffer;

	public Accessor register(ArrayList<BufferView> bufArray) {
		buffer = bufArray.get(bufferView);
		return this;
	}

	/**新しくメモリを確保して同じプロパティのアクセサを作成*/
	public Accessor copy() {
		Accessor res = new Accessor();
		res.byteOffset = byteOffset;
		res.componentType = componentType;
		res.elementType = elementType;
		res.count = count;
		res.max = max;
		res.min = min;
		res.buffer = buffer.copy();
		return res;
	}

	public enum ComponentType {
		@SerializedName("5120")
		BYTE(1, GL11.GL_BYTE, (buf, index) -> String.valueOf(buf.get(index))), @SerializedName("5121")
		UNSIGNED_BYTE(1, GL11.GL_UNSIGNED_BYTE, (buf, index) -> String.valueOf(buf.get() & 0xFF)), @SerializedName("5122")
		SHORT(2, GL11.GL_SHORT, (buf, index) -> String.valueOf(buf.getShort(index))), @SerializedName("5123")
		UNSIGNED_SHORT(2, GL11.GL_UNSIGNED_SHORT, (buf, index) -> String.valueOf(buf.getShort(index) & 0xFFFF)), @SerializedName("5125")
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

	public int getCount() {
		return count;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public ElementType getElementType() {
		return elementType;
	}

	public int getByteOffset() {
		return byteOffset;
	}

	public int getByteIndex(int elementIndex, int componentIndex) {
		return buffer.getByteOffset() + elementIndex * elementType.size * componentType.size + componentIndex*componentType.size;
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

	public void writeAsFloat() {
		ByteBuffer buf = getBuffer();
		System.out.println("print acceser data count = " + count);
		for (int i = 0; i < count; i++) {
			System.out.print("[");
			for (int j = 0; j < getElementType().size; j++) {
				if (j != 0)
					System.out.print(", ");
				System.out.print(getComponentType().getValue(buf, buffer.getByteOffset() + i * getComponentType().size));
			}
			System.out.print("]");
		}
		System.out.println();

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

}
