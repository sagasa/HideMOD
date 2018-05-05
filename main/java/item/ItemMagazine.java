package item;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import hideMod.PackLoader;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.GunData;
import types.GunData.GunDataList;

public class ItemMagazine extends Item{

	public static final String NBT_Name = "HideMagazine";

	public static final String NBT_BULLETNUM = "BulletNum";

	//========================================================================
	//登録
	public ItemMagazine() {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName("hidegun");
		this.setHasSubtypes(true);
	}
	/** クリエイティブタブの中にサブタイプを設定 */
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		for(int meta:PackLoader.BULLET_NAME_MAP.keySet()){
			subItems.add(setBulletNBT(new ItemStack(this, 1, meta).setStackDisplayName(PackLoader.BULLET_DATA_MAP.get(PackLoader.BULLET_NAME_MAP.get(meta)).getDataString(BulletDataList.DISPLAY_NAME))));
		}
	}
	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack setBulletNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return item;
		}
		BulletData data = getBulletData(item);
		NBTTagCompound value;
		if (!item.hasTagCompound()) {
			value = new NBTTagCompound();
		} else {
			value = item.getTagCompound();
		}
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger(NBT_BULLETNUM, getBulletData(item).getDataInt(BulletDataList.MAGAZINE_SIZE));

		value.setTag(NBT_Name, nbt);
		item.setTagCompound(value);
		return item;
	}
	@Override
	public int getItemStackLimit(ItemStack stack) {
		return getBulletData(stack).getDataInt(BulletDataList.STACK_SIZE);
	}
	//=========================================================
	//   更新 便利機能
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
	public static boolean isMagazine(ItemStack item,String str){
		if(item.getItem() instanceof ItemMagazine&& PackLoader.BULLET_NAME_MAP.get(item.getMetadata()).equals(str)){
			return true;
		}
		return false;
	}

	/**残弾数取得*/
	public static int getBulletNum(ItemStack stack){
		return stack.getTagCompound().getCompoundTag(NBT_Name).getInteger(NBT_BULLETNUM);
	}

	/**装弾数取得*/
	public static int getMagazineSize(ItemStack stack){
		return getBulletData(stack).getDataInt(BulletDataList.MAGAZINE_SIZE);
	}

	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		NBTTagCompound nbt = stack.getTagCompound().getCompoundTag(NBT_Name);
		tooltip.add(ChatFormatting.GRAY + "Ammo : " + getMagazineSize(stack)+"/"+getBulletNum(stack));
	}

	/**リロードする弾を取得 アイテムを削除*/
	public static int getReloadItem(EntityPlayer player,String bulletName){
		int slot = 0;
		int bulletNum = -1;
		for(int i = 0;i<36;i++){
			ItemStack item = player.inventory.mainInventory[i];
			if(item!=null&&isMagazine(item,bulletName)){
				if(getMagazineSize(item)<=getBulletNum(item)){
					slot = i;
					bulletNum = getBulletNum(item);
					break;
				}
				if(bulletNum<getBulletNum(item)){
					slot = i;
					bulletNum = getBulletNum(item);
				}
			}
		}
		if(bulletNum == -1){
			return -1;
		}
		ItemStack item = player.inventory.mainInventory[slot];
		if(item.stackSize>1){
			item.stackSize--;
			player.inventory.mainInventory[slot] = item;
		}else{
			player.inventory.mainInventory[slot] = null;
		}
		return bulletNum;
	}

	/** BulletData取得 */
	public static BulletData getBulletData(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return null;
		}
		return PackLoader.BULLET_DATA_MAP.get(getBulletName(item));
	}
	/**スタックから銃の登録名を取得*/
	public static String getBulletName(ItemStack item){
		return PackLoader.BULLET_NAME_MAP.get(item.getMetadata());
	}
}
