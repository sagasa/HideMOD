package types.attachments;

import java.util.ArrayList;
import java.util.List;

import types.base.ValueChange;
import types.items.ItemData;

public abstract class AttachmentsData extends ItemData{
	/**アタッチメントの装備部位*/
	public String ATTACHMENT_TYPE;

	/** アタッチメントの部位 */
	public String TYPE = "default";

	public List<ValueChange> CHANGE_LIST = new ArrayList<>();
}
