package gamedata;

import helper.NBTWrapper;
import item.ItemGun;
import net.minecraft.item.ItemStack;
import types.GunData;

/**銃に共通で必要な項目 NBTからアップデートで読み取り*/
public class Gun {
	public GunData gundata;
	public LoadedMagazine magazine;

	public boolean stopshoot = false;
	public float shootDelay = 0;
	public int shootNum = 0;

	public ItemStack itemGun;
	/**アイテムの銃から作成*/
	public Gun(ItemStack gun){
		itemGun = gun;
		gundata = ItemGun.getGunData(gun);
		magazine = NBTWrapper.getGunLoadedMagazines(gun);
		init();
	}
	/**通知系の初期化*/
	private void init(){

	}

	/**tick処理*/
	public void update(){
		if(0<shootDelay){
			shootDelay -= 1f;
		}
	}
}
