package hideMod;

import java.util.HashMap;

import item.ItemGun;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.registries.IForgeRegistry;
import types.BulletData;
import types.GunData;

public class PackData {
	/** 弾 ショートネーム - BulletData MAP */
	public static HashMap<String, BulletData> BULLET_DATA_MAP = new HashMap<String, BulletData>();

	/** 銃 ショートネーム - BulletData MAP */
	public static HashMap<String, GunData> GUN_DATA_MAP = new HashMap<String, GunData>();

	/** アイコン 登録名 - byte[] MAP */
	public static HashMap<String, byte[]> ICON_MAP = new HashMap<String, byte[]>();

	/** サウンド 登録名 - byte[] MAP */
	public static HashMap<String, byte[]> SOUND_MAP = new HashMap<String, byte[]>();

	/**登録名からGunData取得*/
	public static GunData getGunData(String name){
		return GUN_DATA_MAP.get(name);
	}
	/**登録名からBulletData取得*/
	public static BulletData getBulletData(String name){
		return BULLET_DATA_MAP.get(name);
	}
	
	/**アイテム登録*/
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		for(GunData data:GUN_DATA_MAP.values()){
			new ItemGun(data);
		}
	}
}
