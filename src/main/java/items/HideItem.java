package items;

import java.util.Map;

import hide.guns.HideGunNBT;
import hide.types.items.ItemData;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public abstract class HideItem<T extends ItemData> extends Item {
	public HideItem(String name, Map<String, T> data) {
		super();
		dagaMap = data;
		this.setCreativeTab(CreativeTabs.COMBAT);
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
	}

	private final Map<String, T> dagaMap;

	/**デフォルトのアイテム名*/
	protected String defaultName = "Not Set";

	/**HideItemとして処理可能か*/
	public static boolean isHideItem(ItemStack item) {
		if (item == null) {
			return false;
		}
		return item.getItem() instanceof HideItem;
	}

	public T getData(ItemStack stack) {
		if (!isHideItem(stack))
			return null;
		return dagaMap.get(HideGunNBT.DATA_NAME.get(HideGunNBT.getHideTag(stack)));
	}

	public abstract ItemStack makeItem(T data);

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		ItemData data = getData(stack);
		return data == null ? defaultName : data.get(ItemData.DisplayName);
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		ItemData data = getData(stack);
		if (data == null)
			return 0;
		return data.get(ItemData.StackSize);
	}

	/**サブタイプに銃を書き込む*/
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab == CreativeTabs.COMBAT)
			dagaMap.values().forEach(data -> {
				items.add(makeItem(data));
			});
	}
}
