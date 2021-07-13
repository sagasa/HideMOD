package hide.model.gltf.base;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.google.gson.annotations.SerializedName;

public class Accessor implements IDisposable {

	private int bufferView;
	private int byteOffset;
	private ComponentType componentType;
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
		BYTE(1, GL11.GL_BYTE), @SerializedName("5121")
		UNSIGNED_BYTE(1, GL11.GL_UNSIGNED_BYTE), @SerializedName("5122")
		SHORT(2, GL11.GL_SHORT), @SerializedName("5123")
		UNSIGNED_SHORT(2, GL11.GL_UNSIGNED_SHORT), @SerializedName("5125")
		UNSIGNED_INT(4, GL11.GL_UNSIGNED_INT), @SerializedName("5126")
		FLOAT(4, GL11.GL_FLOAT);

		public final int size;
		public final int gl;

		ComponentType(final int size, final int gl) {
			this.size = size;
			this.gl = gl;
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
		GL20.glEnableVertexAttribArray(index);
		GL20.glVertexAttribPointer(index, elementType.size, componentType.gl, false, buffer.getByteStride(), byteOffset);
	}

	@Override
	public void dispose() {
		buffer.dispose();
	}

}
