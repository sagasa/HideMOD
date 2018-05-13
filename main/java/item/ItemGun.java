package item;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.PlayerHandler;
import helper.NBTWrapper;
import types.guns.GunData;
import types.guns.GunFireMode;
import types.guns.LoadedMagazine;
import types.guns.GunData.GunDataList;
import hideMod.PackLoader;
import net.minecraft.block.Block;
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

	public static final ItemGun INSTANCE = new ItemGun();
	//========================================================================
	//登録
	public ItemGun() {
		this.setCreativeTab(CreativeTabs.tabCombat);
		this.setUnlocalizedName("hidegun");
	}

	/** クリエイティブタブの中にサブタイプを設定 */
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		for(String name:PackLoader.GUN_DATA_MAP.keySet()){
			subItems.add(makeGun(name));
		}
	}

	/**アイテムスタックを作成*/
	public static ItemStack makeGun(String name){
		if(PackLoader.GUN_DATA_MAP.containsKey(name)){
			ItemStack stack = new ItemStack(INSTANCE);
			stack.setTagCompound(new NBTTagCompound());
			NBTWrapper.setGunName(stack, name);
			return setGunNBT(stack);
		}
		return null;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getGunData(stack).getDataString(GunDataList.DISPLAY_NAME);
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



	//TODO 銃剣のオプション次第
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {

		return true;
	}

	//=========================================================
	//   更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + NBTWrapper.getGunFireMode(stack));
		tooltip.add(ChatFormatting.GRAY + "UseBullet : " + NBTWrapper.getGunUseingBullet(stack));
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
		return NBTWrapper.getGunName(item);
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return PackLoader.GUN_DATA_MAP.get(getGunName(item));
	}
}
