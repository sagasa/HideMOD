package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.impl.locale.BaseLocale;

import hideMod.HideMod;
import hideMod.PackData;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import types.BulletData;
import types.GunData;
import types.ItemInfo;
import types.PackInfo;
import types.Sound;

/** パックの読み取り */
public class PackLoader {
	/** パックを置くディレクトリ */
	public static File HideDir;

	/** 追加するクリエイティブタブのリスト */
	public static List<String> newCreativeTabs = new ArrayList<String>();
	
	/** ドメイン 銃 */
	public static final String DOMAIN_GUN = "_gun_";
	/** ドメイン 弾 */
	public static final String DOMAIN_MAGAZINE = "_magazine_";

	// 仮
	private List<GunData> cashGunData;
	private List<BulletData> cashBulletData;
	private PackInfo cashPack;

	/**gson オプションはなし*/
	private static Gson gson = new Gson();

	/**
	 * ディレクトリからパックを検索し読み込む
	 * アイテム登録はしていないので注意
	 */
	public static void load(FMLPreInitializationEvent event) {
		// パックのディレクトリを参照作成
		HideDir = new File(event.getModConfigurationDirectory().getParentFile(), "/Hide/");

		if (!HideDir.exists()) {
			HideDir.mkdirs();
		}
		// 使うパターン
		Pattern zip = Pattern.compile("(.+).zip$");

		// パックを読む
		for (File file : HideDir.listFiles()) {
			// Load folders and valid zip files
			System.out.println(file.getName());
			if (zip.matcher(file.getName()).matches()) {
				// Add the directory to the content pack list
				HideMod.log("Loading content pack : " + file.getName() + "...");
				try {
					PackLoader reader = new PackLoader();
					reader.PackRead(file);
				} catch (IOException e) {
					HideMod.log("error : IOException");
				}
			}
		}
	}

	/** ZIPからデータを読み込む 中身の分岐は別 */
	private void PackRead(File file) throws IOException {
		cashGunData = new ArrayList<GunData>();
		cashBulletData = new ArrayList<BulletData>();
		// 読み込むファイル
		FileInputStream in = new FileInputStream(file);

		ZipInputStream zipIn = new ZipInputStream(in, Charset.forName("Shift_JIS"));
		ZipEntry entry = null;
		while ((entry = zipIn.getNextEntry()) != null) {
			// dirかどうか確認
			if (!entry.isDirectory()) {
				// 内容を読み取り

				// byte[] buffer = new byte[0x6400000];
				byte[] buffer = new byte[(int) entry.getSize()];
				zipIn.read(buffer);
				// パックラッパーに送る
				PackWrapper(buffer, entry.getName());
			}
			zipIn.closeEntry();
		}
		zipIn.close();
		in.close();

		// このタイミングでデータチェック
		if (cashPack != null) {
			for (GunData data : cashGunData) {
				// ショートネームを登録名に書き換え
				setGunDomain(cashPack.PACK_ROOTNAME, data);
				String name = data.ITEM_INFO.NAME_SHORT;
				ItemGun gun = new ItemGun(data, name);
				// 重複しないかどうか
				if (PackData.GUN_DATA_MAP.containsKey(name)) {
					HideMod.log("Item has already been added :" + name);
					continue;
				}
				// データが破損していないか
				if (!ItemGun.isNormalData(data)) {
					HideMod.log("GunData is damaged :" + name);
					continue;
				}
				PackData.GUN_DATA_MAP.put(name, data);
			}
			for (BulletData data : cashBulletData) {
				// ショートネームを登録名に書き換え
				setMagazineDomain(cashPack.PACK_ROOTNAME, data);
				String name = data.ITEM_INFO.NAME_SHORT;
				ItemMagazine gun = new ItemMagazine(data, name);
				// 重複しないかどうか
				if (PackData.BULLET_DATA_MAP.containsKey(name)) {
					HideMod.log("Item has already been added :" + name);
					continue;
				}
				PackData.BULLET_DATA_MAP.put(name, data);
			}
			HideMod.log("Load Successful!");
		} else {
			HideMod.log("error : Missing PackInfo");
		}
	}

	/**
	 * byte配列とNameからパックの要素の当てはめる
	 *
	 * @throws IOException
	 */
	private void PackWrapper(byte[] data, String name) throws IOException {
		// JsonObject newData = gson.fromJson(new String(Arrays.copyOf(data,
		// data.length)), JsonObject.class);
		// Gun認識
		if (Pattern.compile("^(.*)guns/(.*).json").matcher(name).matches()) {
			GunData newGun = gson.fromJson(new String(data, Charset.forName("UTF-8")), GunData.class);
			cashGunData.add(newGun);
		}
		// bullet認識
		else if (Pattern.compile("^(.*)bullets/(.*).json").matcher(name).matches()) {
			BulletData newBullet = gson.fromJson(new String(data, Charset.forName("UTF-8")), BulletData.class);
			System.out.println(newBullet);
			cashBulletData.add(newBullet);
		}
		// packInfo認識
		else if (Pattern.compile("^(.*)pack.json").matcher(name).matches()) {
			cashPack = gson.fromJson(new String(data, Charset.forName("UTF-8")), PackInfo.class);
		}

		// Resources認識
		// Icon
		if (Pattern.compile("^(.*)icon/(.*).png").matcher(name).matches()) {
			String n = Pattern.compile(".png$").matcher(Pattern.compile("^(.*)icon/").matcher(name).replaceAll(""))
					.replaceAll("");
			if (PackData.ICON_MAP.containsKey(n)) {
				HideMod.log("error : Resource is Already exists Name:" + n);
			} else {
				PackData.ICON_MAP.put(n, data);
			}
		}
		// model
		if (Pattern.compile("^(.*)model/(.*).json").matcher(name).matches()) {
			System.out.println("model");
		}
		// texture
		if (Pattern.compile("^(.*)texture/(.*).png").matcher(name).matches()) {
			System.out.println("texture");
		}
		// sounds
		if (Pattern.compile("^(.*)sounds/(.*).ogg").matcher(name).matches()) {
			System.out.println("sounds");
			String n = Pattern.compile(".ogg$").matcher(Pattern.compile("^(.*)sounds/").matcher(name).replaceAll(""))
					.replaceAll("");
			if (PackData.SOUND_MAP.containsKey(n)) {
				HideMod.log("error : Resource is Already exists Name:" + n);
			} else {
				PackData.SOUND_MAP.put(n, data);
			}
		}

	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private void setMagazineDomain(String Domain, BulletData data) {
		ItemInfo item = data.ITEM_INFO;
		item.NAME_SHORT = item.NAME_SHORT + PackLoader.DOMAIN_MAGAZINE + Domain;

		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_HIT_ENTITY);
		checkSoundDomain(data.SOUND_HIT_GROUND);
		checkSoundDomain(data.SOUND_PASSING);
	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private void setGunDomain(String Domain, GunData data) {
		ItemInfo item = data.ITEM_INFO;
		item.NAME_SHORT = item.NAME_SHORT + PackLoader.DOMAIN_GUN + Domain;

		String[] bullets = (String[]) data.BULLET_USE;
		for (int i = 0; i < bullets.length; i++) {
			bullets[i] = bullets[i] + PackLoader.DOMAIN_MAGAZINE + Domain;
		}
		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_RELOAD);
		checkSoundDomain(data.SOUND_SHOOT);
	}

	/** 音のドメインをチェック */
	private void checkSoundDomain(Sound sound) {
		String name = sound.NAME;
		if (!name.contains(":")) {
			sound.NAME = HideMod.MOD_ID + ":" + name;
		}
	}
}