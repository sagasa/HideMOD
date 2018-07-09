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

public class ItemGun extends Item {

	private static Map<String, ItemGun> INSTANCE_MAP = new HashMap<String, ItemGun>();

	public String RegisterName;
	public GunData GunData;

	// ========================================================================
	// 登録
	public ItemGun(GunData data, String name) {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName(name);
		this.setMaxStackSize(1);
		this.RegisterName = name;
		this.GunData = data;
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
			stack = checkGunNBT(stack);
			NBTWrapper.setGunName(stack, name);
			return stack;
		}
		return null;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getGunData(stack).ITEM_INFO.NAME_DISPLAY;
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
			GunData data = getGunData(item);

			NBTWrapper.setGunID(item, -1);
			NBTWrapper.setGunShootDelay(item, 0);
			NBTWrapper.setGunFireMode(item, GunFireMode.getFireMode(
					Arrays.asList(data.FIREMODE).iterator().next().toString()));
			NBTWrapper.setGunUseingBullet(item, Arrays.asList((String[])data.BULLET_USE).iterator().next().toString());
		}
		return item;
	}
	/**データ破損チェック*/
	public static boolean isNormalData(GunData data){
		//弾が登録されているか
		if(((String[])data.BULLET_USE).length==0){
			return false;
		}
		return true;
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
		tooltip.add(ChatFormatting.GRAY + "UseBullet : " + ItemMagazine.getBulletData(NBTWrapper.getGunUseingBullet(stack)).ITEM_INFO.NAME_DISPLAY);
		for (LoadedMagazine magazine : NBTWrapper.getGunLoadedMagazines(stack)) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getBulletData(magazine.name).ITEM_INFO.NAME_DISPLAY + "x"
						+ magazine.num);
			}else{
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

	/** 次の射撃モードを取得 */
	public static GunFireMode getNextFireMode(GunData data, GunFireMode now) {
		List<String> modes = Arrays.asList(data.FIREMODE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		return GunFireMode.getFireMode(modes.get(index));
	}
	/** 次の使用する弾を取得 */
	public static String getNextUseMagazine(GunData data, String now) {
		List<String> modes = Arrays.asList( data.BULLET_USE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
		//	System.out.println(index);
			index = 0;
		}
		//System.out.println(modes+" "+modes.get(index));
		if(!ItemMagazine.isMagazineExist(modes.get(index))){
			return now;
		}
		return modes.get(index);
	}

	/** スタックから銃の登録名を取得 */
	public static String getGunName(ItemStack item) {
		return ((ItemGun) item.getItem()).RegisterName;
	}

	/** GunData取得 */
	public static GunData getGunData(String name) {
		return PackLoader.GUN_DATA_MAP.get(name);
	}
	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return ((ItemGun) item.getItem()).GunData;
	}
}
