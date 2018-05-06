package item;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.PlayerHandler;
import types.LoadedMagazine;
import hideMod.PackLoader;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunData.GunDataList;

public class ItemGun extends Item {

	public static final ItemGun INSTANCE = new ItemGun();

	/** NBTのルート直下にこの名前でデータ保存用タグを保存 */
	public static final String NBT_Name = "HideGun";

	public static final String NBT_FireMode = "FireMode";
	public static final String NBT_UseingBullet = "UseingBullet";
	public static final String NBT_ShootDelay = "ShootDelay";
	public static final String NBT_ReloadProgress = "ReloadProgress";
	public static final String NBT_Magazines = "Magazines";

	public static final String NBT_Magazine_Name = "MagazineName";
	public static final String NBT_Magazine_Number = "MagazineNumber";

	//========================================================================
	//登録
	public ItemGun() {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName("hidegun");
		this.setHasSubtypes(true);
	}

	/** クリエイティブタブの中にサブタイプを設定 */
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		for(int meta:PackLoader.GUN_NAME_MAP.keySet()){
			subItems.add(setGunNBT(new ItemStack(this, 1, meta).setStackDisplayName(PackLoader.GUN_DATA_MAP.get(PackLoader.GUN_NAME_MAP.get(meta)).getDataString(GunDataList.DISPLAY_NAME))));
		}
	}

	@Override
	public int getItemStackLimit() {
		return 1;
	}
	@Override
	public String getUnlocalizedName(ItemStack item) {
		return getUnlocalizedName()+getGunName(item);
	}
	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack setGunNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		GunData data = getGunData(item);
		NBTTagCompound value;
		if (!item.hasTagCompound()) {
			value = new NBTTagCompound();
		} else {
			value = item.getTagCompound();
		}
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger(NBT_ShootDelay, 0);
		nbt.setInteger(NBT_ReloadProgress, -1);
		nbt.setString(NBT_FireMode,
				Arrays.asList(data.getDataStringArray(GunDataList.FIRE_MODE)).iterator().next().toString());
		if (data.getDataStringArray(GunDataList.TYPES_BULLETS).length > 0) {
			nbt.setString(NBT_UseingBullet,
					Arrays.asList(data.getDataStringArray(GunDataList.TYPES_BULLETS)).iterator().next().toString());
		}
		nbt.setTag(NBT_Magazines, new NBTTagCompound());

		value.setTag(NBT_Name, nbt);
		item.setTagCompound(value);
		return item;
	}

	@Override
	public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
		System.out.println("drop");
		item = new ItemStack(this, 1, 0);
		return super.onDroppedByPlayer(item, player);
	}

	//=========================================================
	//   更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		NBTTagCompound nbt = stack.getTagCompound().getCompoundTag(NBT_Name);
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + nbt.getString(NBT_FireMode));
	}
	/** 銃かどうか */
	public static boolean isGun(ItemStack item) {
		if (item != null && item.getItem() instanceof ItemGun) {
			return true;
		}
		return false;
	}

	/**スタックから銃の登録名を取得*/
	public static String getGunName(ItemStack item){
		return PackLoader.GUN_NAME_MAP.get(item.getMetadata());
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return PackLoader.GUN_DATA_MAP.get(getGunName(item));
	}

	/** マガジンの内容を取得 */
	public static LoadedMagazine[] getLoadedMagazines(ItemStack gun) {
		LoadedMagazine[] loadedMagazines = new LoadedMagazine[getGunData(gun).getDataInt(GunDataList.MAGAZINE_NUMBER)];

		NBTTagCompound magazines = gun.getTagCompound().getCompoundTag(NBT_Name).getCompoundTag(NBT_Magazines);

		for (int i = 0; i < loadedMagazines.length; i++) {
			if(magazines.hasKey(i+"")&&magazines.getCompoundTag(i+"").getInteger(NBT_Magazine_Number)>0){
				NBTTagCompound magData = magazines.getCompoundTag(i+"");
				loadedMagazines[i] = new LoadedMagazine(magData.getString(NBT_Magazine_Name), magData.getInteger(NBT_Magazine_Number));
			}
		}
		return loadedMagazines;
	}
	/** マガジンの内容を書き込み */
	public static ItemStack setLoadedMagazines(ItemStack gun,LoadedMagazine[] newMagazines) {
		NBTTagCompound magazines = new NBTTagCompound();
		for (int i = 0; i < newMagazines.length; i++) {
			if(newMagazines[i]!=null){
				NBTTagCompound magazine = new NBTTagCompound();
				magazine.setInteger(NBT_Magazine_Number, newMagazines[i].num);
				magazine.setString(NBT_Magazine_Name, newMagazines[i].name);
				magazines.setTag(i+"", magazine);
			}
		}
		NBTTagCompound newTag = gun.getTagCompound().getCompoundTag(NBT_Name);
		newTag.setTag(NBT_Magazines, magazines);
		NBTTagCompound value = new NBTTagCompound();
		value.setTag(NBT_Name, newTag);
		gun.setTagCompound(value);
		return gun;
	}
}
