package io;

import java.io.ByteArrayInputStream;
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
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.impl.locale.BaseLocale;

import helper.ArrayEditor;
import helper.ObjLoader;
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
import types.ItemInfo;
import types.PackInfo;
import types.effect.Sound;
import types.guns.BulletData;
import types.guns.GunData;
import types.model.ModelPart;

/** パックの読み取り */
public class PackLoader {
	private static final Logger LOGGER = LogManager.getLogger();

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
	private Map<String, byte[]> cashIcon;
	private Map<String, byte[]> cashTexture;
	private Map<String, byte[]> cashSound;
	private Map<String, Map<String, ModelPart>> cashModel;

	/** gson オプションはなし */
	private static Gson gson = new Gson();

	/**
	 * ディレクトリからパックを検索し読み込む アイテム登録はしていないので注意
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
				LOGGER.info("Loading content pack : " + file.getName() + "...");
				try {
					PackLoader reader = new PackLoader();
					reader.PackRead(file);
				} catch (IOException e) {
					LOGGER.error("error : IOException");
				}
			}
		}
	}

	/** ZIPからデータを読み込む 中身の分岐は別 */
	private void PackRead(File file) throws IOException {
		// 初期値
		cashPack = null;
		cashGunData = new ArrayList<GunData>();
		cashBulletData = new ArrayList<BulletData>();
		cashIcon = new HashMap<>();
		cashTexture = new HashMap<>();
		cashSound = new HashMap<>();
		cashModel = new HashMap<>();
		// 読み込むファイル
		FileInputStream in = new FileInputStream(file);

		ZipInputStream zipIn = new ZipInputStream(in, Charset.forName("Shift_JIS"));
		ZipEntry entry = null;
		while ((entry = zipIn.getNextEntry()) != null) {
			// dirかどうか確認
			if (!entry.isDirectory()) {
				// 内容を読み取り

				// byte[] buffer = new byte[0x6400000];
				byte[] buffer = new byte[1024];
				byte[] data = new byte[0];
				int size;
				while (0 < (size = zipIn.read(buffer))) {
					data = ArrayEditor.ByteArrayCombining(data, Arrays.copyOf(buffer, size));
					buffer = new byte[1024];
				}
				// パックラッパーに送る
				PackWrapper(data, entry.getName());
			}
			zipIn.closeEntry();
		}
		zipIn.close();
		in.close();

		// このタイミングでデータチェック
		if (cashPack == null) {
			LOGGER.error("error : Missing PackInfo");
			return;
		}
		// 銃登録
		for (GunData data : cashGunData) {
			// ショートネームを登録名に書き換え
			setGunDomain(cashPack.PACK_ROOTNAME, data);
			String name = data.ITEM_INFO.NAME_SHORT;
			ItemGun gun = new ItemGun(data);
			// 重複しないかどうか
			if (PackData.GUN_DATA_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + name);
				continue;
			}
			// データが破損していないか
			if (!ItemGun.isNormalData(data)) {
				LOGGER.warn("GunData is damaged :" + name);
				continue;
			}
			PackData.GUN_DATA_MAP.put(name, data);
		}
		// 弾登録
		for (BulletData data : cashBulletData) {
			// ショートネームを登録名に書き換え
			setMagazineDomain(cashPack.PACK_ROOTNAME, data);
			String name = data.ITEM_INFO.NAME_SHORT;
			ItemMagazine gun = new ItemMagazine(data);
			// 重複しないかどうか
			if (PackData.BULLET_DATA_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + name);
				continue;
			}
			PackData.BULLET_DATA_MAP.put(name, data);
		}
		// Icon登録
		for (String name : cashIcon.keySet()) {
			// ショートネームを登録名に書き換え
			String newname = addDomain(name, cashPack.PACK_ROOTNAME);
			// 重複しないかどうか
			if (PackData.ICON_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname);
				continue;
			}
			PackData.ICON_MAP.put(newname, cashIcon.get(name));
		}
		// Texture登録
		for (String name : cashTexture.keySet()) {
			// ショートネームを登録名に書き換え
			String newname = addDomain(name, cashPack.PACK_ROOTNAME);
			// 重複しないかどうか
			if (PackData.TEXTURE_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname);
				continue;
			}
			PackData.TEXTURE_MAP.put(newname, cashTexture.get(name));
		}
		// Sound登録
		for (String name : cashSound.keySet()) {
			// ショートネームを登録名に書き換え
			String newname = addDomain(name, cashPack.PACK_ROOTNAME);
			// 重複しないかどうか
			if (PackData.SOUND_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname);
				continue;
			}
			PackData.SOUND_MAP.put(newname, cashSound.get(name));
		}
		// Model登録
		for (String name : cashModel.keySet()) {
			// ショートネームを登録名に書き換え
			String newname = addDomain(name, cashPack.PACK_ROOTNAME);
			// 重複しないかどうか
			if (PackData.MODEL_MAP.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname);
				continue;
			}
			PackData.MODEL_MAP.put(newname, cashModel.get(name));
		}
		LOGGER.info("Load Successful!");
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
					.replaceAll("").toLowerCase();
			cashIcon.put(n, data);
		}
		// model
		if (Pattern.compile("^(.*)model/(.*).obj").matcher(name).matches()) {
			System.out.println("model");
			String n = Pattern.compile(".obj$").matcher(Pattern.compile("^(.*)models/").matcher(name).replaceAll(""))
					.replaceAll("").toLowerCase();
			cashModel.put(n, ObjLoader.LoadModel(new ByteArrayInputStream(data)));
		}
		// texture
		if (Pattern.compile("^(.*)texture/(.*).png").matcher(name).matches()) {
			System.out.println("texture");
		}
		// sounds
		if (Pattern.compile("^(.*)sounds/(.*).ogg").matcher(name).matches()) {
			System.out.println("sounds");
			String n = Pattern.compile(".ogg$").matcher(Pattern.compile("^(.*)sounds/").matcher(name).replaceAll(""))
					.replaceAll("").toLowerCase();
			cashSound.put(n, data);
		}
	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private void setMagazineDomain(String Domain, BulletData data) {
		ItemInfo item = data.ITEM_INFO;
		item.NAME_SHORT = item.NAME_SHORT + PackLoader.DOMAIN_MAGAZINE + Domain;
		item.NAME_ICON = item.NAME_ICON + addDomain(item.NAME_ICON, Domain);

		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_HIT_ENTITY, Domain);
		checkSoundDomain(data.SOUND_HIT_GROUND, Domain);
		checkSoundDomain(data.SOUND_PASSING, Domain);
	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private void setGunDomain(String Domain, GunData data) {
		ItemInfo item = data.ITEM_INFO;
		item.NAME_SHORT = item.NAME_SHORT + PackLoader.DOMAIN_GUN + Domain;
		item.NAME_ICON = item.NAME_ICON + addDomain(item.NAME_ICON, Domain);

		String[] bullets = (String[]) data.BULLET_USE;
		for (int i = 0; i < bullets.length; i++) {
			bullets[i] = bullets[i] + PackLoader.DOMAIN_MAGAZINE + Domain;
		}
		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_RELOAD, Domain);
		checkSoundDomain(data.SOUND_SHOOT, Domain);
	}

	/** 音のドメインをチェック */
	private void checkSoundDomain(Sound sound, String domain) {
		String name = addDomain(sound.NAME, domain);
		if (!name.contains(":")) {
			name = HideMod.MOD_ID + ":" + name;
		}
		sound.NAME = name;
	}

	private String addDomain(String name, String domain) {
		return domain + "_" + name;
	}
}