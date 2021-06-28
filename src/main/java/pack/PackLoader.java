package pack;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import com.google.gson.Gson;

import helper.ArrayEditor;
import hide.types.base.DataBase;
import hide.types.base.DataBase.DataEntry;
import hide.types.base.DataBase.ValueEntry;
import hide.types.base.Info;
import hide.types.items.GunData;
import hide.types.items.ItemData;
import hide.types.items.MagazineData;
import hide.types.pack.PackInfo;
import hidemod.HideMod;
import model.HideModel;
import model.HideModel.HideVertex;
import net.minecraftforge.fml.common.Loader;

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

	/**DataBase系クラスの初期化*/
	static {
		new GunData();
		new MagazineData();
		new PackInfo();
	}

	public static void reloadInGame() {
		load();
	}

	/**
	 * ディレクトリからパックを検索し読み込む アイテム登録はしていないので注意
	 */
	public static void load() {
		// パックのディレクトリを参照作成
		HideDir = new File(Loader.instance().getConfigDir().getParentFile(), "/Hide/");

		if (!HideDir.exists()) {
			HideDir.mkdirs();
		}

		PackData.readData.clear();
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
					LOGGER.info("Start check and add pack[" + cash.Pack.get(PackInfo.PackName) + "]");
					String packDomain = toRegisterName(cash.Pack.get(PackInfo.PackDomain));
					// パックの登録
					PackData.readData.PACK_INFO.add(cash.Pack);
					// 銃登録
					checkAndAddToMap(PackData.readData.GUN_DATA_MAP, cash.Guns, packDomain);
					// 弾登録
					checkAndAddToMap(PackData.readData.MAGAZINE_DATA_MAP, cash.Magazines, packDomain);
					// Icon登録
					checkAndAddToMap(PackData.readData.ICON_MAP, cash.Icons, packDomain);
					// Texture登録
					checkAndAddToMap(PackData.readData.TEXTURE_MAP, cash.Textures, packDomain);
					// Scope登録
					checkAndAddToMap(PackData.readData.SCOPE_MAP, cash.Scopes, packDomain);
					// Sound登録
					checkAndAddToMap(PackData.readData.SOUND_MAP, cash.Sounds, packDomain);
					// Model登録
					cash.ModelInfos.entrySet()
							.forEach(entry -> entry.getValue().setModel(cash.Models.get(entry.getKey())));
					checkAndAddToMap(PackData.readData.MODEL_MAP, cash.ModelInfos, packDomain);
					LOGGER.info("End check and add pack[" + cash.Pack.get(PackInfo.PackName) + "]");
					LOGGER.info("End read file[" + file.getName() + "]");
				} catch (IOException e1) {
					LOGGER.error("error : IOException");
				}
			}
		}
		LOGGER.info("copy to currentData");
		PackData.currentData.from(PackData.readData);
	}

	/** ファイルから読み込むモジュール */
	private static class PackReader {
		private List<GunData> Guns = new ArrayList<>();
		private List<MagazineData> Magazines = new ArrayList<>();
		private PackInfo Pack = null;
		private Map<String, byte[]> Icons = new HashMap<>();
		private Map<String, byte[]> Textures = new HashMap<>();
		private Map<String, byte[]> Sounds = new HashMap<>();
		private Map<String, byte[]> Scopes = new HashMap<>();
		private Map<String, Map<String, HideVertex[]>> Models = new HashMap<>();
		private Map<String, HideModel> ModelInfos = new HashMap<>();

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
				GunData newGun = DataBase.fromJson(new String(data, Charset.forName("UTF-8")));
				Guns.add(newGun);
				LOGGER.info("add gun[" + newGun.get(ItemData.DisplayName) + "] to PackReader");
			}
			// magazine認識
			else if (PackPattern.MAGAZINE.mache(name)) {
				MagazineData newBullet = DataBase.fromJson(new String(data, Charset.forName("UTF-8")));
				Magazines.add(newBullet);
				LOGGER.info("add bullet[" + newBullet.get(ItemData.DisplayName) + "] to PackReader");
			}
			// packInfo認識
			else if (PackPattern.PACKINFO.mache(name)) {
				Pack = DataBase.fromJson(new String(data, Charset.forName("UTF-8")));
				LOGGER.debug("set pack[" + Pack.get(PackInfo.PackName) + "] to PackReader");
			}
			// Resources認識
			// Icon
			if (PackPattern.ICON.mache(name)) {
				String n = PackPattern.ICON.trim(name);
				Icons.put(n, data);
				LOGGER.info("add icon[" + n + "] to PackReader");
			}
			// model
			if (PackPattern.MODEL_GLB.mache(name)) {
				String n = PackPattern.MODEL_GLB.trim(name);
				Models.put(n, ObjLoader.LoadModel(new ByteArrayInputStream(data)));
				LOGGER.info("add model[" + n + "] to PackReader");
			}
			// model
			if (PackPattern.MODEL_OBJ.mache(name)) {
				String n = PackPattern.MODEL_OBJ.trim(name);
				Models.put(n, ObjLoader.LoadModel(new ByteArrayInputStream(data)));
				LOGGER.info("add model[" + n + "] to PackReader");
			}
			// model
			if (PackPattern.MODEL_INFO.mache(name)) {
				String n = PackPattern.MODEL_INFO.trim(name);
				ModelInfos.put(n, gson.fromJson(new String(data, Charset.forName("UTF-8")), HideModel.class));
				LOGGER.info("add model[" + n + "] to PackReader");
			}
			// scope
			if (PackPattern.SCOPE.mache(name)) {
				String n = PackPattern.SCOPE.trim(name);
				Scopes.put(n, data);
				LOGGER.info("add scope[" + n + "] to PackReader");
			}
			// texture
			if (PackPattern.TEXTURE.mache(name)) {
				// TODO
				String n = PackPattern.TEXTURE.trim(name);
				Textures.put(n, data);
				LOGGER.info("add texture[" + n + "] to PackReader");
			}
			// sounds
			if (PackPattern.SOUND.mache(name)) {
				String n = PackPattern.SOUND.trim(name);
				Sounds.put(n, data);
				LOGGER.info("add sound[" + n + "] to PackReader");
			}
		}

		/** パック認識用パターン エディター側と完全互換 */
		private enum PackPattern {
			GUN("guns", "json"), MAGAZINE("magazines", "json"), PACKINFO(Pattern.compile("^(.*)pack\\.json$"),
					"json"), ICON("icons", "png"), SCOPE("scopes",
							"png"), TEXTURE("textures", "png"), SOUND("sounds",
									"ogg"), MODEL_OBJ("models", "obj"), MODEL_GLB("models", "glb"), MODEL_INFO("models", "json");

			private PackPattern(Pattern mache, String end) {
				this.mache = mache;
				this.end = end;
				this.start = "\\.";
			}

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
	private static <T extends ItemData> void checkAndAddToMap(Map<String, T> to, List<T> from,
			String packDomain) {
		for (T data : from) {
			// ショートネームを登録名に書き換え アイテムとリソースで別処理
			setDomain(packDomain, data);
			String name = data.getSystemName();

			// 重複しないかどうか
			if (to.containsKey(name)) {
				LOGGER.error("Duplicate name : " + name + ",Type : " + data.getClass().getSimpleName());
				return;
			}
			// データが破損していないか
			if (!checkData(data)) {
				LOGGER.error("GunData is damaged :" + name + ",Type : " + data.getClass().getSimpleName());
				return;
			}
			to.put(name, data);
		}
	}

	/** Resourceチェックモジュール */
	private static <T> void checkAndAddToMap(Map<String, T> to, Map<String, T> from, String packDomain) {
		for (String name : from.keySet()) {
			// ショートネームを登録名に書き換え
			if (from.get(name) instanceof DataBase) {
				setDomain(packDomain, (DataBase) from.get(name));
			}
			String newname = appendPackDomain(name, packDomain);
			// 重複しないかどうか
			if (to.containsKey(name)) {
				LOGGER.warn("Duplicate name :" + newname + ",Type : Resource");
				return;
			}
			to.put(newname, from.get(name));
		}
	}

	/** アノテーションをもとにデータチェック */
	private static boolean checkData(DataBase data) {
		//表示名が空ならfalse
		for (DataEntry<?> entry : data.getEntries().values()) {
			Info info = entry.Info;
			if (info != null && info.NoEmpty)
				if (entry.Default.getClass().isArray() && ((Object[]) data.get(entry)).length == 0) {
					LOGGER.error("null is not allow at" + data.getClass().getSimpleName() + "."
							+ entry.getName());
					return false;
				} else if (List.class.isAssignableFrom(entry.Default.getClass())
						&& ((List) data.get(entry)).size() == 0) {
					LOGGER.error("emply list is not allow at" + data.getClass().getSimpleName() + "."
							+ entry.getName());
					return false;
				}
		}
		return true;
	}

	/** アノテーションをもとに名前を更新 */
	private static void setDomain(String Domain, DataBase data) {
		// アノテーションが付いたフィールドの値を更新

		for (DataEntry<?> entry : data.getKeySet()) {
			if (DataBase.class.isAssignableFrom(entry.Default.getClass())) {
				//再帰
				DataBase db = (DataBase) data.getEntry(entry).getValue();
				if (db != entry.Default) {
					setDomain(Domain, db);
				}
				continue;
			}

			Info info = entry.Info;
			if (info == null)
				continue;

			//System.out.println("Acc " + entry + " " + entry.Info.IsName + " " + entry.Info.IsResourceName + " " + entry.Default.getClass().equals(String[].class));

			if (entry.Default.getClass().equals(String.class)) {
				ValueEntry<String> valueentry = (ValueEntry<String>) data.getEntry(entry);
				String value = valueentry.getValue();
				if (info.IsResourceName)
					valueentry.setValue(appendModDomain(appendPackDomain(value, info.ResourceHeader, Domain)));
				else if (info.IsName)
					valueentry.setValue(appendPackDomain(value, Domain));
			} else if (entry.Default.getClass().equals(String[].class)) {
				ValueEntry<String[]> valueentry = (ValueEntry<String[]>) data.getEntry(entry);
				String[] value = valueentry.getValue();
				if (info.IsResourceName)
					valueentry.setValue(Arrays.stream(value).map(name -> appendModDomain(appendPackDomain(name, info.ResourceHeader, Domain))).toArray(String[]::new));
				else if (info.IsName)
					valueentry.setValue(Arrays.stream(value).map(name -> appendPackDomain(name, Domain)).toArray(String[]::new));
			}
		}
	}

	/** ドメインを追加 */
	private static String appendPackDomain(String name, String domain) {
		return appendPackDomain(name, Strings.EMPTY, domain);
	}

	/** ドメインを追加 */
	private static String appendPackDomain(String name, String header, String domain) {
		return Strings.isEmpty(name) ? Strings.EMPTY : header + domain + "_" + toRegisterName(name);
	}

	/** ドメインを追加(リソース用) */
	private static String appendModDomain(String name) {
		if (!name.contains(":")) {
			name = Strings.isEmpty(name) ? Strings.EMPTY : HideMod.MOD_ID + ":" + name;
		}
		return name;
	}

	/**利用可能な登録名に変更*/
	private static String toRegisterName(String string) {
		String res = string.toLowerCase().replaceAll("[\\. -]", "_").replaceAll("[^a-z0-9_]", "");
		if (!string.equals(res))
			LOGGER.warn("register name [" + string + "] is invalid change to [" + res + "]");
		return res;
	}
}