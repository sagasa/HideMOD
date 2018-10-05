package hideMod.render;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import hideMod.model.DisplayPart;

/** 複数のModelPartをまとめたグループ */
public class DisplayGroup {

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
	/** 表示の可否 */
	public boolean Visibility = true;
	/**表示リスト*/
	private List<DisplayPart> DisplayList = new ArrayList<>(2);

	public void rotatePoint(float x, float y, float z) {
		rotatepointX = x;
		rotatepointY = y;
		rotatepointZ = z;
	}

	public void rotate(float yaw, float pitch) {
		rotateYaw = yaw;
		rotatePitch = pitch;
	}

	public void translate(float x, float y, float z) {
		translateX = x;
		translateY = y;
		translateZ = z;
	}

	public void scale(float x, float y, float z) {
		scaleX = x;
		scaleY = y;
		scaleZ = z;
	}
	
	/**描画するPartを設定*/
	public void addDisplayPart(DisplayPart part){
		DisplayList.add(part);
	}
	
	/**PartListをクリア*/
	public void clearDisplayList(){
		DisplayList.clear();
	}

	/**位置を調整してグループを描画する*/
	public void render() {
		GL11.glPushMatrix();
		GL11.glTranslatef(translateX, translateY, translateZ);

		GL11.glTranslatef(rotatepointX, rotatepointY, rotatepointZ);
		GL11.glRotatef(rotateYaw, 0, 1, 0);
		GL11.glRotatef(rotatePitch, 1, 0, 0);
		GL11.glTranslatef(-rotatepointX, -rotatepointY, -rotatepointZ);

		DisplayList.forEach(part->part.render());
		
		GL11.glPopMatrix();
	}
}
