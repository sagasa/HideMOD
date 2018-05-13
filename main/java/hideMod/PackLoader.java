package hideMod;

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

import helper.ArrayEditor;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import types.BulletData;
import types.ContentsPack;
import types.ContentsPack.PackDataList;
import types.ImageData;
import types.BulletData.BulletDataList;
import types.guns.GunData;
import types.guns.GunData.GunDataList;

/**パックの読み取り*/
public class PackLoader {
	/**パックを置くディレクトリ*/
	public static File HideDir;
	/**パックを置くパス*/
	public static String HidePath;

	/**コンテンツパックのリスト*/
	public static List<ContentsPack> contentsPackList = new ArrayList<ContentsPack>();
	/**追加するクリエイティブタブのリスト*/
	public static List<String> newCreativeTabs = new ArrayList<String>();

	/**弾 ショートネーム - BulletData MAP*/
	public static HashMap<String,BulletData> BULLET_DATA_MAP = new HashMap<String,BulletData>();

	/**銃 ショートネーム - BulletData MAP*/
	public static HashMap<String, GunData> GUN_DATA_MAP = new HashMap<String,GunData>();

	//仮
	private List<GunData> cashGunData;
	private List<BulletData> cashBulletData;
	private ContentsPack cashPack;

	/**パックから読み込む*/
	public static void load(FMLPreInitializationEvent event) {
		//パックのディレクトリを参照
		HideDir = new File(event.getModConfigurationDirectory().getParentFile(), "/Hide/");
		//パスにする
		HidePath = HideDir.getAbsolutePath()+"/";

		if (!HideDir.exists()){
			HideDir.mkdirs();
		}
		//使うパターン
		Pattern zip = Pattern.compile("(.+).zip$");

		//パックを読む
		for (File file : HideDir.listFiles()){
			//Load folders and valid zip files
			System.out.println(file.getName());
			if (zip.matcher(file.getName()).matches())
			{
				//Add the directory to the content pack list
				HideMod.log("Loading content pack : " + file.getName()+"...");
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

		//このタイミングでレジスターに書き込む
		if(cashPack != null){
			for (GunData data : cashGunData) {
				String registerName = cashPack.getDataString(PackDataList.PACK_ROOTNAME)+"_gun_"+data.getDataString(GunDataList.SHORT_NAME);
				ItemGun gun = new ItemGun(data,registerName,cashPack.getDataString(PackDataList.PACK_ROOTNAME));
				//ショートネームを登録名に書き換え
				data.setData(GunDataList.SHORT_NAME, registerName);
				GUN_DATA_MAP.put(registerName, data);
				//レジスターに書き込む
				GameRegistry.registerItem(gun,registerName);
		        if (FMLCommonHandler.instance().getSide().isClient()) {
		        	ModelLoader.setCustomModelResourceLocation(gun, 0, new ModelResourceLocation(HideMod.MOD_ID + ":" + registerName, "inventory"));
		        }
			}
			for (BulletData data : cashBulletData) {
				String registerName = cashPack.getDataString(PackDataList.PACK_ROOTNAME)+"_magazine_"+data.getDataString(BulletDataList.SHORT_NAME);
				ItemMagazine gun = new ItemMagazine(data,registerName,cashPack.getDataString(PackDataList.PACK_ROOTNAME));
				//ショートネームを登録名に書き換え
				data.setData(BulletDataList.SHORT_NAME, registerName);
				BULLET_DATA_MAP.put(registerName, data);
				//レジスターに書き込む
				GameRegistry.registerItem(gun,registerName);
		        if (FMLCommonHandler.instance().getSide().isClient()) {
		        	ModelLoader.setCustomModelResourceLocation(gun, 0, new ModelResourceLocation(HideMod.MOD_ID + ":" + registerName, "inventory"));
		        }
			}
			contentsPackList.add(cashPack);
			HideMod.log("Load Successful!");
		}else{
			HideMod.log("error : Missing PackInfo");
		}
	}

	/** byte配列とNameからパックの要素の当てはめる
	 * @throws IOException */
	private void PackWrapper(byte[] data, String name) throws IOException {
		// JsonObject newData = gson.fromJson(new String(Arrays.copyOf(data,
		// data.length)), JsonObject.class);
		// Gun認識
		if (Pattern.compile("^(.*)guns/(.*).json").matcher(name).matches()) {
			GunData newGun = new GunData(new String(data, Charset.forName("UTF-8")));
			cashGunData.add(newGun);
		}
		// bullet認識
		else if (Pattern.compile("^(.*)bullets/(.*).json").matcher(name).matches()) {
			BulletData newBullet = new BulletData(new String(data, Charset.forName("UTF-8")));
			cashBulletData.add(newBullet);
		}
		// packInfo認識
		else if (Pattern.compile("^(.*)pack.json").matcher(name).matches()) {
			cashPack = new ContentsPack(new String(data, Charset.forName("UTF-8")));
		}

		// Resources認識
		// Icon
		if (Pattern.compile("^(.*)resources/icon/(.*).png").matcher(name).matches()) {
			String n = Pattern.compile(".png$").matcher(Pattern.compile("^(.*)resources/icon/").matcher(name).replaceAll("")).replaceAll("");
			ImageData newImage = new ImageData(data,n);
 			System.out.println("icon");
		}
		//model
		if (Pattern.compile("^(.*)resources/model/(.*).json").matcher(name).matches()) {
			System.out.println("model");
		}
		//texture
		if (Pattern.compile("^(.*)resources/texture/(.*).png").matcher(name).matches()) {
			System.out.println("texture");
		}
		//sounds
		if (Pattern.compile("^(.*)resources/sounds/(.*).ogg").matcher(name).matches()) {
			System.out.println("sounds");
		}

	}
}