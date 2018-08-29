package hideMod;

import java.util.HashMap;

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

}
