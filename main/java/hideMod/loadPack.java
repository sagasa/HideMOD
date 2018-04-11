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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import types.BulletData;
import types.ContentsPack;
import types.GunData;
import types.ImageData;
import types.GunData.GunDataList;

/**パックの読み取り*/
public class loadPack {
	/**パックを置くディレクトリ*/
	public static File HideDir;
	/**パックを置くパス*/
	public static String HidePath;


	/**コンテンツパックのリスト*/
	public static List<ContentsPack> contentsPackList = new ArrayList<ContentsPack>();
	/**追加するクリエイティブタブのリスト*/
	public static List<String> newCreativeTabs = new ArrayList<String>();

	/**弾のリスト*/
	public static HashMap<String,BulletData> bulletMap = new HashMap<String,BulletData>();

	/**銃のリスト*/
	public static HashMap<String,GunData> gunMap = new HashMap<String,GunData>();


		//初期化
		/*List <types.BulletData> BulletList;
		BulletData bulletData = new BulletData();
		*/

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
					zipRead(file);
				} catch (IOException e) {
					HideMod.log("error : IOException");
				}
				HideMod.log("Load Successful!");
			}
		}
	}

	/** ZIPからデータを読み込む 中身の分岐は別 */
	static void zipRead(File file) throws IOException {
		// 読み込むファイル
		FileInputStream in = new FileInputStream(file);

		// 以下、zipを展開して、中身を確認する
		// TODO 将来的にlangで切り替え可能に…したいなぁ
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

				// String zipString = new String(data);
				// System.out.println(zipString+" "+entry.getName()+"
				// "+entry.getSize());
			}
			zipIn.closeEntry();
		}
		zipIn.close();
		in.close();
	}

	/** byte配列とNameからパックの要素の当てはめる
	 * @throws IOException */
	static void PackWrapper(byte[] data, String name) throws IOException {
		// JsonObject newData = gson.fromJson(new String(Arrays.copyOf(data,
		// data.length)), JsonObject.class);
		// Gun認識
		if (Pattern.compile("^(.*)guns/(.*).json").matcher(name).matches()) {
			GunData newGun = new GunData(new String(data, Charset.forName("UTF-8")));
			gunMap.put(newGun.getData(GunDataList.SHORT_NAME).toString(),newGun);
			System.out.println("gun");
		}
		// bullet認識
		else if (Pattern.compile("^(.*)bullets/(.*).json").matcher(name).matches()) {
			BulletData newBullet = new BulletData(new String(data, Charset.forName("UTF-8")));
		//	bulletList.out(newBullet.,newBullet);
			System.out.println("bullet");
		}
		// packInfo認識
		else if (Pattern.compile("^(.*)pack.json").matcher(name).matches()) {
			System.out.println("pack :" + new String(data, Charset.forName("UTF-8")));
			contentsPackList.add(new ContentsPack(new String(data, Charset.forName("UTF-8"))));
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

	/**レジスタに書き込み*/
	public static void Register() {
		//アイテムをレジスタに
		for(GunData data:gunMap.values()){
			Item testitem = new ItemGun()
	                .setCreativeTab(CreativeTabs.tabCombat)/*クリエイティブのタブ*/
	                .setUnlocalizedName(data.getData(GunDataList.SHORT_NAME).toString())/*システム名の登録*/
	                .setMaxStackSize(1);/*スタックできる量。デフォルト64*/
	    	GameRegistry.registerItem(testitem, data.getData(GunDataList.SHORT_NAME).toString());
	    	System.out.println(data.getData(GunDataList.SHORT_NAME).toString());
		}

    	//ModelLoader.setCustomModelResourceLocation(testitem, 0, new ModelResourceLocation(HideMod.MOD_ID + ":" + "testitem", "inventory"));
	}
}