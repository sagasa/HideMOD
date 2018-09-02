package item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.realmsclient.gui.ChatFormatting;

import helper.NBTWrapper;
import hideMod.PackData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import scala.actors.threadpool.Arrays;
import types.BulletData;

public class ItemMagazine extends Item {

	public static Map<String, ItemMagazine> INSTANCE_MAP = new HashMap<String, ItemMagazine>();

	public BulletData BulletData;

	// ========================================================================
	// 登録
	public ItemMagazine(BulletData data) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		String name = data.ITEM_INFO.NAME_SHORT;
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(data.STACK_SIZE);
		this.BulletData = data;
		INSTANCE_MAP.put(name, this);
	}


	/** アイテムスタックを作成 残弾指定 */
	public static ItemStack makeMagazine(String name, int ammoNum) {
		return NBTWrapper.setMagazineBulletNum(makeMagazine(name), ammoNum);
	}

	/**アイテムスタック作成時に呼ばれる これの中でNBTを設定する*/
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		setBulletNBT(stack);
		return super.initCapabilities(stack, nbt);
	}


	/** アイテムスタックを作成 */
	public static ItemStack makeMagazine(String name) {
		if (PackData.BULLET_DATA_MAP.containsKey(name)) {
			ItemStack stack = new ItemStack(INSTANCE_MAP.get(name));
			stack.setTagCompound(new NBTTagCompound());
			return setBulletNBT(stack);
		}
		return null;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack setBulletNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return item;
		}
		BulletData data = getBulletData(item);
		NBTWrapper.setMagazineBulletNum(item, data.MAGAZINE_SIZE);
		return item;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getBulletData(stack).ITEM_INFO.NAME_DISPLAY;
	}

	// =========================================================
	// 更新 便利機能
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
		if (item != null && item.getItem() instanceof ItemMagazine
				&&ItemMagazine.getBulletData(item).ITEM_INFO.NAME_SHORT.equals(str)) {
			return true;
		}
		return false;
	}

	/** 残弾数取得 */
	public static int getBulletNum(ItemStack stack) {
		return NBTWrapper.getMagazineBulletNum(stack);
	}

	/** 残弾数書き込み */
	public static ItemStack setBulletNum(ItemStack stack, int num) {
		NBTWrapper.setMagazineBulletNum(stack, num);
		return stack;
	}

	/** 装弾数取得 */
	public static int getMagazineSize(ItemStack stack) {
		return getBulletData(stack).MAGAZINE_SIZE;
	}

	/** アップデート 表示更新など */
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if(!stack.hasTagCompound()){
			return;
		}
		tooltip.add(ChatFormatting.GRAY + "Ammo : " + getBulletNum(stack) + "/" + getMagazineSize(stack));
	}

	/** リロードする弾を取得 アイテムを削除 */
	public static int ReloadItem(EntityPlayer player, String bulletName, int amount) {
		int bulletNum = amount;
		for (ItemStack item:player.inventory.mainInventory) {
			if (item != null && isMagazine(item, bulletName)) {
				bulletNum -= getBulletNum(item);

				// 端数を返す
				if (bulletNum < 0) {
					ItemStack newMag = item.copy();
					newMag.setCount(1+newMag.getCount());
					// アイテムを1つ削除
					item.stackSize--;
					if (item.stackSize == 0) {
						item = null;
					}
					player.inventory.mainInventory[i] = item;
					player.inventory.addItemStackToInventory(setBulletNum(newMag, bulletNum * -1));
					return amount;
				}
				// アイテムを1つ削除
				item.stackSize--;
				if (item.stackSize == 0) {
					item = null;
				}
				player.inventory.mainInventory[i] = item;
				// 破棄しない設定なら空のマガジンを追加
				if (!ItemMagazine.getBulletData(bulletName).MAGAZINE_BREAK) {
					player.inventory.addItemStackToInventory(makeMagazine(bulletName, 0));
				}
			}
		}
		return amount - bulletNum;
	}

	/** BulletData取得 */
	public static BulletData getBulletData(String name) {
		return PackData.BULLET_DATA_MAP.get(name);
	}

	/** BulletData取得 */
	public static BulletData getBulletData(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return null;
		}
		return ((ItemMagazine) item.getItem()).BulletData;
	}

	/** スタックから弾の登録名を取得 */
	public static String getBulletName(ItemStack item) {
		return ItemMagazine.getBulletData(item).ITEM_INFO.NAME_SHORT;
	}

	/** その名前の弾は存在するか */
	public static boolean isMagazineExist(String name) {
		return PackData.BULLET_DATA_MAP.containsKey(name);
	}
}
