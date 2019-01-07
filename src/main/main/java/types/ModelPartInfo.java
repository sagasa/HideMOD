package types;

import types.base.DataBase;

public class ModelPartInfo extends DataBase{
	/** 回転の基準 */
	public float rotatepointX = 0;
	/** 回転の基準 */
	public float rotatepointY = 0;
	/** 回転の基準 */
	public float rotatepointZ = 0;
	/** 回転の数値 */
	public float rotateYaw = 0;
	/** 回転の数値 */
	public float rotatePitch = 0;
	/** 移動の数値 */
	public float translateX = 0;
	/** 移動の数値 */
	public float translateY = 0;
	/** 移動の数値 */
	public float translateZ = 0;
	/** 縮尺の数値 */
	public float scaleX = 1;
	/** 縮尺の数値 */
	public float scaleY = 1;
	/** 縮尺の数値 */
	public float scaleZ = 1;
}
