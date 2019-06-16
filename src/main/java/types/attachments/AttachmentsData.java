package types.attachments;

import java.util.HashMap;
import java.util.Map;

import types.items.ItemData;

public abstract class AttachmentsData extends ItemData{
	/**アタッチメントの装備部位*/
	public String ATTACHMENT_TYPE;

	/** アタッチメントの部位 */
	public String TYPE = "default";

	public Map<String,Float> FLOAT_ADD_MAP = new HashMap<>();
	public Map<String,Float> FLOAT_DIA_MAP = new HashMap<>();
	public Map<String,Float> FLOAT_SET_MAP = new HashMap<>();

	public Map<String,String> STRING_SET_MAP = new HashMap<>();
}
