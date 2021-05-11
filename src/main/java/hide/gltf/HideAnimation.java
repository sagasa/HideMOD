package hide.gltf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.AnimationModel.Channel;
import de.javagl.jgltf.model.AnimationModel.Interpolation;
import de.javagl.jgltf.model.NodeModel;

public class HideAnimation {

	private final float minKey;
	private final float maxKey;

	private List<HideChannel> channels = new ArrayList<>();

	class HideChannel {
		final float[] times;
		final AccessorFloatData output;
		Interpolation interpolation;
		final AnimationPath path;
		final NodeModel node;
		final int elementCount;

		HideChannel(Channel channel) {
			AccessorModel input = channel.getSampler().getInput();
			times = new float[input.getCount()];
			for (int i = 0; i < input.getCount(); i++) {
				times[i] = input.getBufferViewModel().getBufferViewData().getFloat(i * 4);
			}

			path = AnimationPath.valueOf(channel.getPath());
			node = channel.getNodeModel();

			AccessorModel outAccessor = channel.getSampler().getOutput();
			elementCount = path == AnimationPath.weights ? outAccessor.getCount() / times.length : outAccessor.getElementType().getNumComponents();
			interpolation = channel.getSampler().getInterpolation();
			output = AccessorDatas.createFloat(channel.getSampler().getOutput());
		}

		/**key以下の最大のIndex*/
		private int getIndex(float key) {
			int index = Arrays.binarySearch(times, key);
			return index >= 0 ? index : Math.max(0, -index - 2);
		}

		void apply(float key) {
			int index0 = getIndex(key);
			int index1 = Math.min(times.length - 1, index0 + 1);
			float alpha = getAlpha(key, index0);

			switch (interpolation) {
			case CUBICSPLINE:
				//TODO
				break;
			case LINEAR:
				linearInterpolator(index0, index1, alpha);
				break;
			case STEP:
				//TODO
				break;
			}

		}

		private float getAlpha(float key, int index) {
			if (key <= times[0]) {
				return 0.0f;
			}
			if (key >= times[times.length - 1]) {
				return 1.0f;
			}
			float local = key - times[index];
			float delta = times[index + 1] - times[index];
			float alpha = local / delta;
			return alpha;
		}

		float[] get() {
			float[] value = null;
			switch (path) {
			case rotation:
				value = node.getRotation();
				if (value == null) {
					value = new float[4];
					node.setRotation(value);
				}
				break;
			case scale:
				value = node.getScale();
				if (value == null) {
					value = new float[3];
					node.setScale(value);
				}
				break;
			case translation:
				value = node.getTranslation();
				if (value == null) {
					value = new float[3];
					node.setTranslation(value);
				}
				break;
			case weights:
				value = node.getWeights();
				if (value == null) {
					value = new float[elementCount];
					node.setWeights(value);
				}
				break;
			}
			return value;
		}

		void linearInterpolator(int index0, int index1, float alpha) {
			float[] value = get();
			for (int i = 0; i < value.length; i++) {
				float a = output.get(index0 * elementCount + i);
				float b = output.get(index1 * elementCount + i);
				value[i] = a + alpha * (b - a);
			}
			//System.out.println(ArrayUtils.toString(value));
		}
	}

	public HideAnimation(AnimationModel animation) {
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (Channel channel : animation.getChannels()) {
			min = Math.min((float) channel.getSampler().getInput().getMin()[0], min);
			max = Math.max((float) channel.getSampler().getInput().getMax()[0], max);
			channels.add(new HideChannel(channel));
		}
		maxKey = max;
		minKey = min;
		System.out.println(min + " " + max);
	}

	/**0-1*/
	public void apply(float value) {
		float key = minKey + value * (maxKey - minKey);
		for (HideChannel channel : channels) {
			channel.apply(key);
		}
	}

	enum AnimationPath {
		translation, rotation, scale, weights
	}
}
