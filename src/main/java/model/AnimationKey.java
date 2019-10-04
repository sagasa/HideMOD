package model;

public class AnimationKey implements Comparable<AnimationKey> {
	public float Key = 0;

	public float translateX = 0, translateY = 0, translateZ = 0;
	public float scaleX = 0, scaleY = 0, scaleZ = 0;
	public float rotateX = 0, rotateY = 0, rotateZ = 0;

	public static void applyAnimation() {

	}

	@Override
	public int compareTo(AnimationKey o) {
		return Float.compare(this.Key, o.Key);
	}

}
