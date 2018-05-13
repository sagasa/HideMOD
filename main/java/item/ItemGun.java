package item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.PlayerHandler;
import helper.NBTWrapper;
import types.guns.GunData;
import types.guns.GunFireMode;
import types.guns.LoadedMagazine;
import types.guns.GunData.GunDataList;
import hideMod.HideMod;
import hideMod.PackLoader;
import net.minecraft.block.Block;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.LanguageRegistry;
import types.BulletData;
import types.BulletData.BulletDataList;

public class ItemGun extends Item {

	private static Map<String, ItemGun> INSTANCE_MAP = new HashMap<String, ItemGun>();

	public String RegisterName;
	public GunData GunData;
	public String Domain;

	// ========================================================================
	// 登録
	public ItemGun(GunData data, String name, String domain) {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName(name);
		this.setMaxStackSize(1);
		this.RegisterName = name;
		this.GunData = data;
		this.Domain = domain;
		INSTANCE_MAP.put(name, this);
	}

	/** クリエイティブタブの中にサブタイプを設定 */
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		subItems.add(makeGun(RegisterName));
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(String name) {
		if (PackLoader.GUN_DATA_MAP.containsKey(name)) {
			ItemStack stack = new ItemStack(INSTANCE_MAP.get(name));
			stack.setTagCompound(new NBTTagCompound());
			NBTWrapper.setGunName(stack, name);
			return checkGunNBT(stack);
		}
		return null;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getGunData(stack).getDataString(GunDataList.DISPLAY_NAME);
	}

	public static void setUUID(ItemStack item) {
		if (NBTWrapper.getGunID(item) == -1) {
			NBTWrapper.setGunID(item, UUID.randomUUID().getLeastSignificantBits());
		}
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack checkGunNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む
		if (!item.hasTagCompound()) {
			item.setTagCompound(new NBTTagCompound());
			ItemGun instance = (ItemGun) item.getItem();
			GunData data = getGunData(item);

			NBTWrapper.setGunID(item, -1);
			NBTWrapper.setGunShootDelay(item, 0);
			NBTWrapper.setGunReloadProgress(item, -1);
			NBTWrapper.setGunFireMode(item, GunFireMode.getFireMode(
					Arrays.asList(data.getDataStringArray(GunDataList.FIRE_MODE)).iterator().next().toString()));
			NBTWrapper.setGunUseingBullet(item, instance.Domain + "_magazine_"
					+ Arrays.asList(data.getDataStringArray(GunDataList.TYPES_BULLETS)).iterator().next().toString());
		}
		return item;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack checkGunMagazines(ItemStack item) {
		// マガジンの弾の登録があるかを確認 無ければ破棄
		LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
		for (int i = 0; i < magazines.length; i++) {
			if (!ItemMagazine.isMagazineExist(magazines[i].name)) {
				magazines[i] = null;
			}
		}
		NBTWrapper.setGunLoadedMagazines(item, magazines);
		return item;
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
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + NBTWrapper.getGunFireMode(stack));
		tooltip.add(ChatFormatting.GRAY + "UseBullet : " + ItemMagazine
				.getBulletData(NBTWrapper.getGunUseingBullet(stack)).getDataString(BulletDataList.DISPLAY_NAME));
		for (LoadedMagazine magazine : NBTWrapper.getGunLoadedMagazines(stack)) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getBulletData(magazine.name).getDataString(BulletDataList.DISPLAY_NAME) + "x"
						+ magazine.num);
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

	/** 次の射撃モードを取得 */
	public static GunFireMode getNextFireMode(GunData data, GunFireMode now) {
		List<String> modes = Arrays.asList(data.getDataStringArray(GunDataList.FIRE_MODE));
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		return GunFireMode.getFireMode(modes.get(index));
	}

	/** スタックから銃の登録名を取得 */
	public static String getGunName(ItemStack item) {
		return ((ItemGun) item.getItem()).RegisterName;
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return ((ItemGun) item.getItem()).GunData;
	}
}
