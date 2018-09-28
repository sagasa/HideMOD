package types.model;

public class ModelPart {
	public Polygon[] Polygon;

	/** 回転の基準 */
	public  float rotatepointX;
	/** 回転の基準 */
	public  float rotatepointY;
	/** 回転の基準 */
	public  float rotatepointZ;
	/**回転の数値*/
	public  float rotateYaw;
	/**回転の数値*/
	public  float rotatePitch;
	/**移動の数値*/
	public float translateX;
	/**移動の数値*/
	public  float translateY;
	/**移動の数値*/
	public  float translateZ;
	/**縮尺の数値*/
	public  float scaleX;
	/**縮尺の数値*/
	public  float scaleY;
	/**縮尺の数値*/
	public  float scaleZ;

	public ModelPart(Polygon[] array) {
		Polygon = array;
	}
	public void rotatePoint(float x, float y, float z) {
		rotatepointX = x;
		rotatepointY = y;
		rotatepointZ = z;
	}
	public void rotate(float yaw, float pitch) {
		rotateYaw = yaw;
		rotatePitch = pitch;
	}
	public void translate(float x, float y, float z){
		translateX = x;
		translateY = y;
		translateZ = z;
	}
	public void scale(float x, float y, float z){
		scaleX = x;
		scaleY = y;
		scaleZ = z;
	}
}
