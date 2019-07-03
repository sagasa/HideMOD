package items;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import helper.HideNBT;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import pack.PackData;
import types.items.MagazineData;

public class ItemMagazine extends Item {

	// ========================================================================
	// 登録
	public ItemMagazine(String name) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
	}

	public static final ItemMagazine INSTANCE = new ItemMagazine("magazine");

	/** アイテムスタックを作成 残弾指定 */
	public static ItemStack makeMagazine(String name, int ammoNum) {
		return HideNBT.setMagazineBulletNum(makeMagazine(name), ammoNum);
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeMagazine(String name) {
		return makeMagazine(PackData.getBulletData(name));
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeMagazine(MagazineData data) {
		if (data != null) {
			ItemStack stack = new ItemStack(INSTANCE);
			stack.setTagCompound(new NBTTagCompound());
			return makeMagazineNBT(stack, data);
		}
		return null;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack makeMagazineNBT(ItemStack item, MagazineData data) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return item;
		}
		NBTTagCompound hideTag = HideNBT.getMagazineTag(item);
		hideTag.setString(HideNBT.MAGAZINE_NAME, data.ITEM_SHORTNAME);
		HideNBT.setMagazineBulletNum(hideTag, data.MAGAZINE_SIZE);
		return item;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab == CreativeTabs.COMBAT)
			PackData.MAGAZINE_DATA_MAP.values().forEach(mag -> {
				System.out.println("aaaadd");
				items.add(makeMagazine(mag));
			});
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		MagazineData data = getMagazineData(stack);
		return data != null ? data.ITEM_DISPLAYNAME : null;
	}

	// =========================================================
	// 更新 便利機能
	@Override
	public int getItemStackLimit(ItemStack stack) {
		return getMagazineData(stack).ITEM_STACK_SIZE;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getMagazineSize(stack) > getBulletNum(stack);
	}

	@Override
	public int getDamage(ItemStack stack) {
		return getMagazineSize(stack) - getBulletNum(stack);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return getMagazineSize(stack);
	}

	@Override
	public boolean isDamaged(ItemStack stack) {
		return false;
	}

	public static boolean isMagazine(ItemStack item, String str) {
		if (item != null && ItemMagazine.getMagazineData(item) != null
				&& ItemMagazine.getMagazineData(item).ITEM_SHORTNAME.equals(str)) {
			return true;
		}
		return false;
	}

	public static boolean isMagazine(ItemStack item, String str, int size) {
		if (item != null && ItemMagazine.getMagazineData(item) != null
				&& ItemMagazine.getMagazineData(item).ITEM_SHORTNAME.equals(str)
				&& HideNBT.getMagazineBulletNum(item) == size) {
			return true;
		}
		return false;
	}

	/** 残弾数取得 */
	public static int getBulletNum(ItemStack stack) {
		return HideNBT.getMagazineBulletNum(stack);
	}

	/** 残弾数書き込み */
	public static ItemStack setBulletNum(ItemStack stack, int num) {
		HideNBT.setMagazineBulletNum(stack, num);
		return stack;
	}

	/** 装弾数取得 */
	public static int getMagazineSize(ItemStack stack) {
		MagazineData data = getMagazineData(stack);
		return data == null ? 0 : data.MAGAZINE_SIZE;
	}

	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if (!stack.hasTagCompound()) {
			return;
		}
		tooltip.add(ChatFormatting.GRAY + "Ammo : " + getBulletNum(stack) + "/" + getMagazineSize(stack));
	}

	/** 表示名取得 */
	public static String getMagazineName(String name) {
		MagazineData data = PackData.getBulletData(name);
		return data == null ? "None" : data.ITEM_DISPLAYNAME;
	}

	/** BulletData取得 */
	public static MagazineData getMagazineData(String name) {
		return PackData.getBulletData(name);
	}

	/** BulletData取得 */
	public static MagazineData getMagazineData(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return null;
		}
		return PackData.getBulletData(HideNBT.getMagazineTag(item).getString(HideNBT.MAGAZINE_NAME));
	}

	/** その名前の弾は存在するか */
	public static boolean isMagazineExist(String name) {
		return PackData.getBulletData(name) != null;
	}
}
