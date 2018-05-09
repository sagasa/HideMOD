package item;

import java.util.List;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.PlayerHandler;
import helper.NBTWrapper;
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
import types.GunFireMode;

public class ItemGun extends Item {

	public static final ItemGun INSTANCE = new ItemGun();
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

	public static void setUUID(ItemStack item){
		if(NBTWrapper.getGunID(item)==-1){
			NBTWrapper.setGunID(item, UUID.randomUUID().getLeastSignificantBits());
		}
	}
	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack setGunNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		if (!item.hasTagCompound()) {
			item.setTagCompound(new NBTTagCompound());
		}
		GunData data = getGunData(item);

		NBTWrapper.setGunID(item, -1);
		NBTWrapper.setGunShootDelay(item, 0);
		NBTWrapper.setGunReloadProgress(item, -1);
		NBTWrapper.setGunFireMode(item, GunFireMode.getFireMode(Arrays.asList(data.getDataStringArray(GunDataList.FIRE_MODE)).iterator().next().toString()));
		if (data.getDataStringArray(GunDataList.TYPES_BULLETS).length > 0) {
			NBTWrapper.setGunUseingBullet(item, Arrays.asList(data.getDataStringArray(GunDataList.TYPES_BULLETS)).iterator().next().toString());
		}
		return item;
	}

	//=========================================================
	//   更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + NBTWrapper.getGunFireMode(stack));
		for(LoadedMagazine magazine :NBTWrapper.getGunLoadedMagazines(stack)){
			if(magazine != null){
				tooltip.add(magazine.name+"x"+magazine.num);
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
	/**次の射撃モードを取得*/
	public static GunFireMode getNextFireMode(GunData data,GunFireMode now){
		List<String> modes = Arrays.asList(data.getDataStringArray(GunDataList.FIRE_MODE));
		int index = modes.indexOf(now.toString())+1;
		if(index > modes.size()-1){
			index = 0;
		}
		return GunFireMode.getFireMode(modes.get(index));
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
}
