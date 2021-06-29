package hide.model.gltf.base;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.google.gson.annotations.SerializedName;

public class Accessor {

	private int bufferView;
	private int byteOffset;
	private ComponentType componentType;
	private ElementType elementType;
	private int count;
	private Number[] max;
	private Number[] min;

	private BufferView buffer;
	public Accessor register(ArrayList<BufferView> bufArray) {
		buffer = bufArray.get(bufferView);
		return this;
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


}
