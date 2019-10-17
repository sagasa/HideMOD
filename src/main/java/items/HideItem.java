package items;

import java.util.Map;

import helper.HideNBT;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import types.items.ItemData;

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
	protected boolean hasUUID = true;

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
		return dagaMap.get(HideNBT.getHideTag(stack).getString(HideNBT.DATA_NAME));
	}

	public abstract ItemStack makeItem(T data);

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		ItemData data = getData(stack);
		return data == null ? defaultName : data.ITEM_DISPLAYNAME;
	}

	/**サブタイプに銃を書き込む*/
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab == CreativeTabs.COMBAT)
			dagaMap.values().forEach(data -> {
				items.add(makeItem(data));
			});
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		// TODO 自動生成されたメソッド・スタブ
		System.out.println("initCapabilities");
		return new ItemUID();
	}

	class ItemUID implements ICapabilityProvider {

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			// TODO 自動生成されたメソッド・スタブ
			System.out.println("hasCapability " + capability + " " + facing);
			return false;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			System.out.println("getCapability " + capability + " " + facing);
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}

	}
}
