package item;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import helper.NBTWrapper;
import hideMod.PackLoader;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.guns.GunData;
import types.guns.GunData.GunDataList;

public class ItemMagazine extends Item{

	public static final ItemMagazine INSTANCE = new ItemMagazine();



	//========================================================================
	//登録
	public ItemMagazine() {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName("hidegun");
	}
	/** クリエイティブタブの中にサブタイプを設定 */
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		for(String name:PackLoader.BULLET_DATA_MAP.keySet()){
			subItems.add(makeMagazine(name));
		}
	}
	public static ItemStack makeMagazine(String name) {
		if(PackLoader.BULLET_DATA_MAP.containsKey(name)){
			ItemStack stack = new ItemStack(INSTANCE);
			stack.setTagCompound(new NBTTagCompound());
			NBTWrapper.setMagazineName(stack, name);
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
		if (!item.hasTagCompound()) {
			item.setTagCompound(new NBTTagCompound());
		}
		NBTWrapper.setMagazineBulletNum(item, data.getDataInt(BulletDataList.MAGAZINE_SIZE));
		return item;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getBulletData(stack).getDataString(GunDataList.DISPLAY_NAME);
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
		if(item!=null&&item.getItem() instanceof ItemMagazine){
			return true;
		}
		return false;
	}

	/**残弾数取得*/
	public static int getBulletNum(ItemStack stack){
		return NBTWrapper.getMagazineBulletNum(stack);
	}

	/**残弾数書き込み*/
	public static ItemStack setBulletNum(ItemStack stack,int num){
		NBTWrapper.setMagazineBulletNum(stack, num);
		return stack;
	}

	/**装弾数取得*/
	public static int getMagazineSize(ItemStack stack){
		return getBulletData(stack).getDataInt(BulletDataList.MAGAZINE_SIZE);
	}

	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		tooltip.add(ChatFormatting.GRAY + "Ammo : " + getMagazineSize(stack)+"/"+getBulletNum(stack));
	}

	/**リロードする弾を取得 アイテムを削除*/
	public static int ReloadItem(EntityPlayer player,String bulletName,int amount){
		int bulletNum = amount;
		for(int i = 0;i<36;i++){
			ItemStack item = player.inventory.mainInventory[i];
			if(item!=null&&isMagazine(item,bulletName)){
				player.inventory.inventoryChanged = true;
				bulletNum -= getBulletNum(item);

				//端数を返す
				if(bulletNum < 0){
					ItemStack newMag = item.copy();
					newMag.stackSize = 1;
					//アイテムを1つ削除
					item.stackSize--;
					if(item.stackSize == 0){
						item = null;
					}
					player.inventory.mainInventory[i] = item;
					player.inventory.addItemStackToInventory(setBulletNum(newMag, bulletNum*-1));
					return amount;
				}
				//アイテムを1つ削除
				item.stackSize--;
				if(item.stackSize == 0){
					item = null;
				}
				player.inventory.mainInventory[i] = item;
			}
		}
		return amount - bulletNum;
	}


	/** BulletData取得 */
	public static BulletData getBulletData(ItemStack item) {
		if (!(item.getItem() instanceof ItemMagazine)) {
			return null;
		}
		return PackLoader.BULLET_DATA_MAP.get(getBulletName(item));
	}
	/** BulletData取得 */
	public static BulletData getBulletData(String name) {
		return PackLoader.BULLET_DATA_MAP.get(name);
	}
	/**スタックから銃の登録名を取得*/
	public static String getBulletName(ItemStack item){
		return NBTWrapper.getMagazineName(item);
	}
}
