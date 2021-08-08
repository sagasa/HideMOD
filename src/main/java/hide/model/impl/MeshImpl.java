package hide.model.impl;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.annotations.SerializedName;

import hide.model.util.HideShader;

public abstract class MeshImpl implements IDisposable {

	protected abstract MeshPrimitivesImpl[] getPrimitives();

	protected float[] weights = ArrayUtils.EMPTY_FLOAT_ARRAY;

	void postInit() {
		for (MeshPrimitivesImpl meshPrimitives : getPrimitives()) {
			meshPrimitives.postInit();
		}
	}

	public boolean hasWeights() {
		return weights.length != 0;
	}

	public void calcWeight(float[] weight) {
		for (MeshPrimitivesImpl meshPrimitives : getPrimitives()) {
			meshPrimitives.calcWeight(weight);
		}
	}

	public void setShader(HideShader shader) {
		for (MeshPrimitivesImpl meshPrimitives : getPrimitives()) {
			meshPrimitives.setShader(shader);
		}
	}

	public void render() {
		for (MeshPrimitivesImpl meshPrimitives : getPrimitives()) {
			meshPrimitives.render();
		}
	}

	@Override
	public void dispose() {
		for (MeshPrimitivesImpl p : getPrimitives()) {
			p.dispose();
		}
	}

	public enum Mode {
		@SerializedName("0")
		POINTS(0), @SerializedName("1")
		LINES(1), @SerializedName("2")
		LINE_LOOP(2), @SerializedName("3")
		LINE_STRIP(3), @SerializedName("4")
		TRIANGLES(4), @SerializedName("5")
		TRIANGLE_STRIP(5), @SerializedName("6")
		TRIANGLE_FAN(6);

		public final int gl;

		Mode(final int gl) {
			this.gl = gl;
		}
	}

	public enum Attribute {
		POSITION(0), NORMAL(1), TEXCOORD_0(2), TANGENT(3), COLOR_0(4), JOINTS_0(5), WEIGHTS_0(6);

		public final int index;

		Attribute(final int index) {
			this.index = index;
		}
	}

}
