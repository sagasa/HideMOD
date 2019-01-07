package types.attachments;

import types.base.ItemData;
import types.guns.GunData;

public class GunCustomizePart extends ItemData {

	private static final GunData BLANK_DATA_1;
	private static final GunData BLANK_DATA_0;
	static {
		BLANK_DATA_1 = new GunData();
		BLANK_DATA_1.setValue(1);
		BLANK_DATA_0 = new GunData();
		BLANK_DATA_0.setValue(1);
	}

	/** スタックサイズ */
	public int STACK_SIZE = 1;
	/** アタッチメントの部位 */
	public String TYPE = "default";
	public GunData DATA_ADD = (GunData) BLANK_DATA_0.clone();
	public GunData DATA_DIA = (GunData) BLANK_DATA_1.clone();
}
