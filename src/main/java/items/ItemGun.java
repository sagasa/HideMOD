package items;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import gamedata.LoadedMagazine.Magazine;
import guns.GunController;
import handler.client.HideItemRender;
import helper.HideNBT;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import pack.PackData;
import types.base.GunFireMode;
import types.items.GunData;

public class ItemGun extends Item {
	/** モデル描画 */
	// public RenderHideGun Model;

	// ========================================================================

	public ItemGun(String name) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
		setTileEntityItemStackRenderer(new HideItemRender());
	}

	public static final ItemGun INSTANCE = new ItemGun("gun");

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(String name) {
		return makeGun(PackData.getGunData(name));
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(GunData data) {
		if (data != null) {
			ItemStack stack = new ItemStack(INSTANCE);
			stack = makeGunNBT(stack, data);
			return stack;
		}
		return null;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		GunData data = getGunData(stack);
		return data != null ? data.ITEM_DISPLAYNAME : null;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack makeGunNBT(ItemStack item, GunData data) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む;
		NBTTagCompound hideTag = HideNBT.getGunTag(item);
		hideTag.setString(HideNBT.GUN_NAME, data.ITEM_SHORTNAME);
		HideNBT.setHideID(hideTag, UUID.randomUUID().getLeastSignificantBits());
		HideNBT.setGunShootDelay(hideTag, 0);
		HideNBT.setGunFireMode(hideTag,
				GunFireMode.getFireMode(Arrays.asList(data.FIREMODE).iterator().next().toString()));
		HideNBT.setGunUseingBullet(hideTag, GunController.LOAD_ANY);
		return item;
	}

	/**サブタイプに銃を書き込む*/
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (tab == CreativeTabs.COMBAT)
			PackData.getGunData().forEach(gun -> {
				items.add(makeGun(gun));
			});
	}

	/** データ破損チェック */
	public static boolean isNormalData(GunData data) {
		// 弾が登録されているか
		if (data.MAGAZINE_USE.length == 0) {
			return false;
		}
		return true;
	}

	// TODO 銃剣のオプション次第
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {

		return true;
	}

	// =========================================================
	// 更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		// 破損チェック
		NBTTagCompound hideTag = HideNBT.getGunTag(stack);
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + HideNBT.getGunFireMode(hideTag));
		String useBullet = HideNBT.getGunUseingBullet(hideTag);
		tooltip.add(ChatFormatting.GRAY + "UseBullet : "
				+ (GunController.LOAD_ANY.equals(useBullet) ? GunController.LOAD_ANY
						: ItemMagazine.getMagazineName(useBullet)));
		for (Magazine magazine : HideNBT.getGunLoadedMagazines(hideTag).getList()) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getMagazineName(magazine.name) + "x" + magazine.num);
			} else {
				tooltip.add("empty");
			}
		}
	}

	/** 銃かどうか */
	public static boolean isGun(ItemStack item) {
		if (item != null && item.getItem() instanceof ItemGun) {
			return true;
		}
		return false;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		// TODO 自動生成されたメソッド・スタブ
		return EnumAction.BOW;
	}

	/** GunData取得 */
	public static GunData getGunData(String name) {
		return PackData.getGunData(name);
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return PackData.getGunData(HideNBT.getGunTag(item).getString(HideNBT.GUN_NAME));
	}
}
