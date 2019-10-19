package items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mojang.realmsclient.gui.ChatFormatting;

import gamedata.LoadedMagazine.Magazine;
import guns.GunController;
import helper.HideNBT;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import pack.PackData;
import types.base.GunFireMode;
import types.items.GunData;

public class ItemGun extends HideItem<GunData> {
	/** モデル描画 */
	// public RenderHideGun Model;

	// ========================================================================

	public ItemGun(String name, Map<String, GunData> data) {
		super(name, data);
	}

	public static ItemGun INSTANCE;

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

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack makeGunNBT(ItemStack item, GunData data) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む;
		NBTTagCompound hideTag = HideNBT.getHideTag(item);
		hideTag.setString(HideNBT.DATA_NAME, data.ITEM_SHORTNAME);
		HideNBT.setGunShootDelay(hideTag, 0);
		HideNBT.setGunFireMode(hideTag,
				GunFireMode.getFireMode(Arrays.asList(data.FIREMODE).iterator().next().toString()));
		HideNBT.setGunUseingBullet(hideTag, GunController.LOAD_ANY);
		return item;
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

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return super.initCapabilities(stack, nbt);
	}

	// =========================================================
	// 更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		// 破損チェック
		NBTTagCompound hideTag = HideNBT.getHideTag(stack);
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
		return getGunData(item) != null;
	}

	/** GunData取得 */
	public static GunData getGunData(String name) {
		return PackData.getGunData(name);
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (item != null && !(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return PackData.getGunData(HideNBT.getHideTag(item).getString(HideNBT.DATA_NAME));
	}

	@Override
	public ItemStack makeItem(GunData data) {
		// TODO 自動生成されたメソッド・スタブ
		return makeGun(data);
	}
}
