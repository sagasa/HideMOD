package types.attachments;

import types.items.GunData;
import types.items.ItemData;

public class GunCustomizePart extends ItemData {

	/** スタックサイズ */
	public int STACK_SIZE = 1;
	/** アタッチメントの部位 */
	public String TYPE = "default";

	public Map<String,Float> add;
}
