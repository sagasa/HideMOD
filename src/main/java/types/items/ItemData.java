package types.items;

import types.base.DataBase;

public abstract class ItemData extends DataBase {
	/** 登録名 */
	public String ITEM_SHORTNAME = "sample";
	/** 表示名 */
	public String ITEM_DISPLAYNAME = "sample";
	/** アイコン名 */
	public String ITEM_ICONNAME = "sample";
	/** スタックサイズ : int型 **/
	public int STACK_SIZE = 4;
	/**所持したときのHPブースト 防具の場合は着た時*/
	public float ITEM_MAX_HEALTH = 0f;
	/**所持したときの速度ブースト  防具の場合は着た時*/
	public float ITEM_MOVE_SPEED = 0f;
	/**所持したときのノックバック耐性ブースト  防具の場合は着た時*/
	public float ITEM_KNOCKBACK_RESISTANCE = 0f;
	/**所持したときの近接ダメージブースト  防具の場合は着た時*/
	public float ITEM_ATTACK_DAMAGE = 0f;
}
