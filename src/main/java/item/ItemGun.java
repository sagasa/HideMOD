package item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import gamedata.LoadedMagazine.Magazine;
import helper.NBTWrapper;
import hideMod.PackData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import types.items.GunData;
import types.base.GunFireMode;

public class ItemGun extends Item {

	public static Map<String, ItemGun> INSTANCE_MAP = new HashMap<>();

	public GunData GunData;

	/** モデル描画 */
	// public RenderHideGun Model;

	// ========================================================================
	// 登録
	public ItemGun(GunData data) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		String name = data.ITEM_SHORTNAME;
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
		this.GunData = data;
		INSTANCE_MAP.put(name, this);
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(String name) {
		if (PackData.GUN_DATA_MAP.containsKey(name)) {
			ItemStack stack = new ItemStack(INSTANCE_MAP.get(name));
			stack = checkGunNBT(stack);
			return stack;
		}
		return null;
	}

	/** アイテムスタック作成時に呼ばれる これの中でNBTを設定する */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		checkGunNBT(stack);
		return super.initCapabilities(stack, nbt);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getGunData(stack).ITEM_DISPLAYNAME;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack checkGunNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む
		GunData data = getGunData(item);

		NBTTagCompound hideTag = NBTWrapper.getHideTag(item);
		NBTWrapper.setHideID(hideTag, UUID.randomUUID().getLeastSignificantBits());
		NBTWrapper.setGunShootDelay(hideTag, 0);
		NBTWrapper.setGunFireMode(hideTag,
				GunFireMode.getFireMode(Arrays.asList(data.FIREMODE).iterator().next().toString()));
		NBTWrapper.setGunUseingBullet(hideTag, Arrays.asList(data.MAGAZINE_USE).iterator().next().toString());
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

	// =========================================================
	// 更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		// 破損チェック
		if (!stack.hasTagCompound()) {
			return;
		}
		NBTTagCompound hideTag = NBTWrapper.getHideTag(stack);
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + NBTWrapper.getGunFireMode(hideTag));
		tooltip.add(ChatFormatting.GRAY + "UseBullet : "
				+ ItemMagazine.getBulletData(NBTWrapper.getGunUseingBullet(hideTag)).ITEM_DISPLAYNAME);
		for (Magazine magazine : NBTWrapper.getGunLoadedMagazines(hideTag).getList()) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getBulletData(magazine.name).ITEM_DISPLAYNAME + "x" + magazine.num);
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

	/** スタックから銃の登録名を取得 */
	public static String getGunName(ItemStack item) {
		return getGunData(item).ITEM_SHORTNAME;
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
		return ((ItemGun) item.getItem()).GunData;
	}
}
