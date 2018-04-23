package item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import types.BulletData;
import types.GunData;

public class ItemGun extends Item{
	private GunData gundata;
	/**NBTのルート直下にこの名前でデータ保存用タグを保存*/
	public static final String NBT_Name = "HideGun";

	public static final String NBT_FireMode = "FireMode";
	public static final String NBT_UseingBullet = "UseingBullet";
	public static final String NBT_ShootDelay = "ShootDelay";
	public static final String NBT_ReloadProgress = "ReloadProgress";
	public static final String NBT_Magazines = "Magazines";

	public static final String NBT_Magazine_Name = "MagazineName";
	public static final String NBT_Magazine_Number = "MagazineNumber";


	public ItemGun(GunData data) {
		gundata = data;
	}
	/**銃かどうか*/
	public static boolean isGun(ItemStack item){
		if (item != null&& item.getItem() instanceof ItemGun){
			return true;
		}
		return false;
	}
	/**クリエイティブタブの中にNBTを付与*/
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems) {
		ItemStack itemStack = new ItemStack(this, 1, 0);
		NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger(NBT_ShootDelay, 0);
        nbt.setInteger(NBT_ReloadProgress, 0);

        NBTTagCompound value = new NBTTagCompound();
        value.setTag(NBT_Name, nbt);
        itemStack.setTagCompound(value);
        subItems.add(itemStack);
	}
	/**GunData取得*/
	public GunData getGunData(){
		return gundata;
	}
	/**次に発射される弾を取得*/
	public static BulletData getNextBullet(ItemStack gun){
		return null;
	}
	/**弾を消費*/
	public static void useBullet(ItemStack gun){

	}
	/**インベントリにマガジンを排出*/
	public static void exitMagazine(ItemStack gun,EntityLivingBase owner){
		gun.getTagCompound().getCompoundTag(NBT_Magazines);
	}
	/**インベントリからマガジンをロード*/
	public static void loadMagazine(ItemStack gun,EntityLivingBase owner){

	}
}
