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
import hideMod.model.ModelPart;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import types.PackInfo;
import types.effect.Sound;
import types.guns.BulletData;
import types.guns.GunData;

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

	/** ファイルから読み込むモジュール */
	private static class PackReader {
		private List<GunData> Guns = new ArrayList<>();
		private List<BulletData> Bullets = new ArrayList<>();
		private PackInfo Pack = null;
		private Map<String, byte[]> Icons = new HashMap<>();
		private Map<String, byte[]> Textures = new HashMap<>();
		private Map<String, byte[]> Sounds = new HashMap<>();
		private Map<String, Map<String, ModelPart>> Models = new HashMap<>();

		public PackReader(File file) throws IOException {
			LOGGER.debug("start read pack["+file.getName()+"] to PackCash");
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
					addToPack(data, entry.getName());
				}
				zipIn.closeEntry();
			}
			zipIn.close();
			in.close();
			LOGGER.debug("end read pack["+file.getName()+"] to PackCash");
		}

		/**
		 * byte配列とNameからパックの要素の当てはめる
		 *
		 * @throws IOException
		 */
		private void addToPack(byte[] data, String name) throws IOException {
			// Gun認識
			if (Pattern.compile("^(.*)guns/(.*).json").matcher(name).matches()) {
				GunData newGun = gson.fromJson(new String(data, Charset.forName("UTF-8")), GunData.class);
				Guns.add(newGun);
				LOGGER.debug("add gun[" + newGun.ITEM_DISPLAYNAME + "] to PackReader");
			}
			// bullet認識
			else if (Pattern.compile("^(.*)bullets/(.*).json").matcher(name).matches()) {
				BulletData newBullet = gson.fromJson(new String(data, Charset.forName("UTF-8")), BulletData.class);
				Bullets.add(newBullet);
				LOGGER.debug("add bullet[" + newBullet.ITEM_DISPLAYNAME + "] to PackReader");
			}
			// packInfo認識
			else if (Pattern.compile("^(.*)pack.json").matcher(name).matches()) {
				Pack = gson.fromJson(new String(data, Charset.forName("UTF-8")), PackInfo.class);
				LOGGER.debug("set pack[" + Pack.PACK_NAME + "] to PackReader");
			}
			// Resources認識
			// Icon
			if (Pattern.compile("^(.*)icon/(.*).png").matcher(name).matches()) {
				String n = Pattern.compile(".png$").matcher(Pattern.compile("^(.*)icon/").matcher(name).replaceAll(""))
						.replaceAll("").toLowerCase();
				Icons.put(n, data);
				LOGGER.debug("add icon[" + n + "] to PackReader");
			}
			// model
			if (Pattern.compile("^(.*)models/(.*).obj").matcher(name).matches()) {
				String n = Pattern.compile(".obj$")
						.matcher(Pattern.compile("^(.*)models/").matcher(name).replaceAll("")).replaceAll("")
						.toLowerCase();
				Models.put(n, ObjLoader.LoadModel(new ByteArrayInputStream(data)));
				LOGGER.debug("add model[" + n + "] to PackReader");
			}
			// texture
			if (Pattern.compile("^(.*)texture/(.*).png").matcher(name).matches()) {

			}
			// sounds
			if (Pattern.compile("^(.*)sounds/(.*).ogg").matcher(name).matches()) {
				String n = Pattern.compile(".ogg$")
						.matcher(Pattern.compile("^(.*)sounds/").matcher(name).replaceAll("")).replaceAll("")
						.toLowerCase();
				Sounds.put(n, data);
				LOGGER.debug("add sound[" + n + "] to PackReader");
			}
		}
	}

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
			if (zip.matcher(file.getName()).matches()) {
				try {
					LOGGER.info("Start read file["+file.getName()+"]");
					PackReader cash = new PackReader(file);
					// キャッシュを検証して追加
					if (cash.Pack == null) {
						LOGGER.error("error : Missing PackInfo");
						return;
					}
					LOGGER.info("Start check and add pack["+cash.Pack.PACK_NAME+"]");
					String packDomain = cash.Pack.PACK_ROOTNAME;
					// 銃登録
					for (GunData data : cash.Guns) {
						// ショートネームを登録名に書き換え
						setGunDomain(packDomain, data);
						String name = data.ITEM_SHORTNAME;
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
					for (BulletData data : cash.Bullets) {
						// ショートネームを登録名に書き換え
						setMagazineDomain(packDomain, data);
						String name = data.ITEM_SHORTNAME;
						ItemMagazine gun = new ItemMagazine(data);
						// 重複しないかどうか
						if (PackData.BULLET_DATA_MAP.containsKey(name)) {
							LOGGER.warn("Duplicate name :" + name);
							continue;
						}
						PackData.BULLET_DATA_MAP.put(name, data);
					}
					// Icon登録
					for (String name : cash.Icons.keySet()) {
						// ショートネームを登録名に書き換え
						String newname = addDomain(name, packDomain);
						// 重複しないかどうか
						if (PackData.ICON_MAP.containsKey(name)) {
							LOGGER.warn("Duplicate name :" + newname);
							continue;
						}
						PackData.ICON_MAP.put(newname, cash.Icons.get(name));
					}
					// Texture登録
					for (String name : cash.Textures.keySet()) {
						// ショートネームを登録名に書き換え
						String newname = addDomain(name, packDomain);
						// 重複しないかどうか
						if (PackData.TEXTURE_MAP.containsKey(name)) {
							LOGGER.warn("Duplicate name :" + newname);
							continue;
						}
						PackData.TEXTURE_MAP.put(newname, cash.Textures.get(name));
					}
					// Sound登録
					for (String name : cash.Sounds.keySet()) {
						// ショートネームを登録名に書き換え
						String newname = addDomain(name, packDomain);
						// 重複しないかどうか
						if (PackData.SOUND_MAP.containsKey(name)) {
							LOGGER.warn("Duplicate name :" + newname);
							continue;
						}
						PackData.SOUND_MAP.put(newname, cash.Sounds.get(name));
					}
					// Model登録
					for (String name : cash.Models.keySet()) {
						// ショートネームを登録名に書き換え
						String newname = addDomain(name, packDomain);
						// 重複しないかどうか
						if (PackData.MODEL_MAP.containsKey(name)) {
							LOGGER.warn("Duplicate name :" + newname);
							continue;
						}
						PackData.MODEL_MAP.put(newname, cash.Models.get(name));
					}
					LOGGER.info("End read file["+file.getName()+"]");
				} catch (IOException e1) {
					LOGGER.error("error : IOException");
				}
			}
		}
	}

	/** ZIPからデータを読み込む 中身の分岐は別 */
	private void PackRead(File file) throws IOException {

		// このタイミングでデータチェック


	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private static void setMagazineDomain(String Domain, BulletData data) {
		data.ITEM_SHORTNAME = data.ITEM_SHORTNAME + PackLoader.DOMAIN_MAGAZINE + Domain;
		data.ITEM_ICONNAME = addDomain(data.ITEM_ICONNAME, Domain);

		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_HIT_ENTITY, Domain);
		checkSoundDomain(data.SOUND_HIT_GROUND, Domain);
		checkSoundDomain(data.SOUND_PASSING, Domain);
	}

	/** 使用マガジンやアタッチメントなどの名前を更新 */
	private static void setGunDomain(String Domain, GunData data) {
		data.ITEM_SHORTNAME = data.ITEM_SHORTNAME + PackLoader.DOMAIN_GUN + Domain;
		data.ITEM_ICONNAME = addDomain(data.ITEM_ICONNAME, Domain);

		String[] bullets = (String[]) data.BULLET_USE;
		for (int i = 0; i < bullets.length; i++) {
			bullets[i] = bullets[i] + PackLoader.DOMAIN_MAGAZINE + Domain;
		}
		// 音のドメインがなければ定義
		checkSoundDomain(data.SOUND_RELOAD, Domain);
		checkSoundDomain(data.SOUND_SHOOT, Domain);
	}

	/** 音のドメインをチェック */
	private static void checkSoundDomain(Sound sound, String domain) {
		String name = addDomain(sound.NAME, domain);
		if (!name.contains(":")) {
			name = HideMod.MOD_ID + ":" + name;
		}
		sound.NAME = name;
	}

	private static String addDomain(String name, String domain) {
		return domain + "_" + name;
	}
}