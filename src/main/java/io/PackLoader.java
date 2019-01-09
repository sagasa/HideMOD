package io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
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
import jline.internal.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import types.Info;
import types.PackInfo;
import types.base.DataBase;
import types.effect.Sound;
import types.items.GunData;
import types.items.ItemData;
import types.items.MagazineData;

/** パックの読み取り */
public class PackLoader {
	private static final Logger LOGGER = LogManager.getLogger();

	/** パックを置くディレクトリ */
	public static File HideDir;

	/** ドメイン 銃 */
	public static final String DOMAIN_GUN = "_gun_";
	/** ドメイン 弾 */
	public static final String DOMAIN_MAGAZINE = "_magazine_";
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
					LOGGER.info("Start read file[" + file.getName() + "]");
					PackReader cash = new PackReader(file);
					// キャッシュを検証して追加
					if (cash.Pack == null) {
						LOGGER.error("error : Missing PackInfo");
						return;
					}
					LOGGER.info("Start check and add pack[" + cash.Pack.PACK_NAME + "]");
					String packDomain = cash.Pack.PACK_ROOTNAME;
					// 銃登録
					cash.Guns.forEach(data -> checkAndAddToMap(PackData.GUN_DATA_MAP, data, packDomain));
					// 弾登録
					cash.Magazines.forEach(data -> checkAndAddToMap(PackData.MAGAZINE_DATA_MAP, data, packDomain));
					// Icon登録
					checkAndAddToMap(cash.Icons, PackData.ICON_MAP, packDomain);
					// Texture登録
					checkAndAddToMap(cash.Textures, PackData.TEXTURE_MAP, packDomain);
					// Sound登録
					checkAndAddToMap(cash.Sounds, PackData.SOUND_MAP, packDomain);
					// Model登録
					checkAndAddToMap(cash.Models, PackData.MODEL_MAP, packDomain);
					LOGGER.info("End read file[" + file.getName() + "]");
				} catch (IOException e1) {
					LOGGER.error("error : IOException");
				}
			}
		}
	}

	/** ファイルから読み込むモジュール */
	private static class PackReader {
		private List<GunData> Guns = new ArrayList<>();
		private List<MagazineData> Magazines = new ArrayList<>();
		private PackInfo Pack = null;
		private Map<String, byte[]> Icons = new HashMap<>();
		private Map<String, byte[]> Textures = new HashMap<>();
		private Map<String, byte[]> Sounds = new HashMap<>();
		private Map<String, Map<String, ModelPart>> Models = new HashMap<>();

		/** ファイルからパックのキャッシュを作成する */
		public PackReader(File file) throws IOException {
			LOGGER.debug("start read pack[" + file.getName() + "] to PackCash");
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
			LOGGER.debug("end read pack[" + file.getName() + "] to PackCash");
		}

		/**
		 * byte配列とNameからパックの要素の当てはめる
		 *
		 * @throws IOException
		 */
		private void addToPack(byte[] data, String name) throws IOException {
			// Gun認識
			if (PackPattern.GUN.mache(name)) {
				GunData newGun = gson.fromJson(new String(data, Charset.forName("UTF-8")), GunData.class);
				Guns.add(newGun);
				LOGGER.debug("add gun[" + newGun.ITEM_DISPLAYNAME + "] to PackReader");
			}
			// magazine認識
			else if (PackPattern.MAGAZINE.mache(name)) {
				MagazineData newBullet = gson.fromJson(new String(data, Charset.forName("UTF-8")), MagazineData.class);
				Magazines.add(newBullet);
				LOGGER.debug("add bullet[" + newBullet.ITEM_DISPLAYNAME + "] to PackReader");
			}
			// packInfo認識
			else if (PackPattern.PACKINFO.mache(name)) {
				Pack = gson.fromJson(new String(data, Charset.forName("UTF-8")), PackInfo.class);
				LOGGER.debug("set pack[" + Pack.PACK_NAME + "] to PackReader");
			}
			// Resources認識
			// Icon
			if (PackPattern.ICON.mache(name)) {
				String n = PackPattern.ICON.trim(name);
				Icons.put(n, data);
				LOGGER.debug("add icon[" + n + "] to PackReader");
			}
			// model
			if (PackPattern.MODEL.mache(name)) {
				String n = PackPattern.MODEL.trim(name);
				Models.put(n, ObjLoader.LoadModel(new ByteArrayInputStream(data)));
				LOGGER.debug("add model[" + n + "] to PackReader");
			}
			// texture
			if (PackPattern.TEXTURE.mache(name)) {
				// TODO
			}
			// sounds
			if (PackPattern.SOUND.mache(name)) {
				String n = PackPattern.SOUND.trim(name);
				Sounds.put(n, data);
				LOGGER.debug("add sound[" + n + "] to PackReader");
			}
		}

		/** パック認識用パターン エディター側と完全互換 */
		private enum PackPattern {
			GUN("guns", "json"), MAGAZINE("magazines", "json"), PACKINFO("pack", "json"), ICON("icons", "png"), SCOPE(
					"scopes", "png"), TEXTURE("textures", "png"), SOUND("soubds", "ogg"), MODEL("models", "obj");
			private PackPattern(String start, String end) {
				mache = Pattern.compile("^(.*)" + start + "/(.*)\\." + end + "$");
				rep_start = Pattern.compile("^(.*)" + start + "/");
				rep_end = Pattern.compile("\\." + end + "$");
				this.start = start;
				this.end = end;
			}

			boolean mache(String path) {
				return mache.matcher(path).matches();
			}

			String trim(String path) {
				return rep_start.matcher(rep_end.matcher(path).replaceFirst("")).replaceFirst("");
			}

			String toPath(String name) {
				return start + "/" + name + "." + end;
			}

			private Pattern mache;
			private Pattern rep_start;
			private Pattern rep_end;
			private String start;
			private String end;
		}
	}

	/** Itemチェックモジュール */
	private static <T extends ItemData> void checkAndAddToMap(Map<String, T> map, T data, String packDomain) {
		// ショートネームを登録名に書き換え
		setDomain(packDomain, data);
		String name = data.ITEM_SHORTNAME;
		// 重複しないかどうか
		if (map.containsKey(name)) {
			LOGGER.error("Duplicate name : " + name + ",Type : " + data.getClass().getSimpleName());
			return;
		}
		// データが破損していないか
		if (!checkData(data)) {
			LOGGER.error("GunData is damaged :" + name + ",Type : " + data.getClass().getSimpleName());
			return;
		}
		map.put(name, data);
	}

	/** Resourceチェックモジュール */
	private static <T> void checkAndAddToMap(Map<String, T> to, Map<String, T> from, String packDomain) {
		for (String name : from.keySet()) {
			// ショートネームを登録名に書き換え
			String newname = setResourceDomain(name, packDomain);
			// 重複しないかどうか
			if (to.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname + ",Typr : Resource");
				return;
			}
			to.put(newname, from.get(name));
		}
	}

	/** アノテーションをもとにデータチェック */
	private static boolean checkData(DataBase data) {
		try {
			for (Field field : data.getFieldsByType(data.getClass(), null, new ArrayList<>(), true)) {
				// 空リストの判別部分
				if (field.getAnnotation(Info.class).noEmpty()) {
					if (field.getType().isArray() && ((String[]) field.get(data)).length == 0) {
						Log.error(
								"emply list is not allow at" + data.getClass().getSimpleName() + "." + field.getName());
						return false;
					} else if (List.class.isAssignableFrom(field.getType()) && ((List) field.get(data)).size() == 0) {
						Log.error(
								"emply list is not allow at" + data.getClass().getSimpleName() + "." + field.getName());
						return false;
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/** アノテーションをもとに名前を更新 */
	private static void setDomain(String Domain, DataBase data) {
		// アノテーションが付いたフィールドの値を更新
		data.getFieldsByType(data.getClass(), String.class, new ArrayList<>(), true).forEach(field -> {
			try {
				if (field.getAnnotation(Info.class).isResourceName())
					field.set(data, setResourceDomain((String) field.get(data), Domain));
				if (field.getAnnotation(Info.class).isName())
					field.set(data, setDomain((String) field.get(data), Domain));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		});
	}

	/** ドメインを追加 */
	private static String setDomain(String name, String domain) {
		return domain + "_" + name;
	}

	/** ドメインを追加(リソース用) */
	private static String setResourceDomain(String name, String domain) {
		if (!name.contains(":")) {
			name = HideMod.MOD_ID + ":" + name;
		}
		return setDomain(name, domain);
	}
}