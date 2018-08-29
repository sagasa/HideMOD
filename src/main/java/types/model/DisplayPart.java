package types.model;

public class DisplayPart {
	public Polygon[] Polygon;

	/** 回転の基準 */
	public float X;
	/** 回転の基準 */
	public float Y;
	/** 回転の基準 */
	public float Z;

	public int displayList;
	public boolean compiled;


	/**回転の基準を*/
	void setPoint(float x, float y, float z) {
		X = x;
		Y = y;
		Z = z;
	}

	void setRotate(float yaw, float pitch) {

	}
}