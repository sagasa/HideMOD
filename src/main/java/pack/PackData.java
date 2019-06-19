package pack;

import java.util.HashMap;
import java.util.Map;

import hidemod.HideMod;
import items.ItemGun;
import items.ItemMagazine;
import model.ModelPart;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import types.attachments.AttachmentsData;
import types.items.GunData;
import types.items.MagazineData;

public class PackData {
	/** 弾 ショートネーム - MagazineData MAP */
	static Map<String, MagazineData> MAGAZINE_DATA_MAP = new HashMap<>();

	/** 銃 ショートネーム - GunData MAP */
	static Map<String, GunData> GUN_DATA_MAP = new HashMap<>();

	/** 銃 ショートネーム - Attachment MAP */
	static Map<String, AttachmentsData> ATTACHMENT_DATA_MAP = new HashMap<>();

	/** アイコン 登録名 - byte[] MAP */
	static Map<String, byte[]> ICON_MAP = new HashMap<>();

	/** サウンド 登録名 - byte[] MAP */
	static Map<String, byte[]> SOUND_MAP = new HashMap<>();

	/** テクスチャ 登録名 - byte[] MAP */
	static Map<String, byte[]> TEXTURE_MAP = new HashMap<>();

	/** モデル 登録名 - Map<String,ModelPart> MAP */
	static Map<String, Map<String, ModelPart>> MODEL_MAP = new HashMap<>();

	/** 登録名からGunData取得 */
	public static GunData getGunData(String name) {
		return GUN_DATA_MAP.get(name);
	}

	/** 登録名からBulletData取得 */
	public static MagazineData getBulletData(String name) {
		return MAGAZINE_DATA_MAP.get(name);
	}
	/**登録名からアタッチメントを取得*/
	public static AttachmentsData getAttachmentData(String name) {
		return ATTACHMENT_DATA_MAP.get(name);
	}

	/** アイテム登録 */
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		for (GunData data : GUN_DATA_MAP.values()) {
			Item item = new ItemGun(data);
			register.register(item);
		}
		for (MagazineData data : MAGAZINE_DATA_MAP.values()) {
			register.register(new ItemMagazine(data));
		}
	}

	/** モデル登録 */
	@SideOnly(Side.CLIENT)
	public static void registerModel() {
		for (ItemGun item : ItemGun.INSTANCE_MAP.values()) {
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(
					new ResourceLocation(HideMod.MOD_ID, item.GunData.ITEM_SHORTNAME), "inventory"));
		}
		for (ItemMagazine item : ItemMagazine.INSTANCE_MAP.values()) {
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(
					new ResourceLocation(HideMod.MOD_ID, item.MagazineData.ITEM_SHORTNAME), "inventory"));
		}
//		RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, new IRenderFactory() {
//			@Override
//			public Render createRenderFor(RenderManager manager) {
//				return new RenderBullet(manager);
//			}
	//	});
	}

	public static String debug() {
		return MAGAZINE_DATA_MAP.entrySet().toString();
	}

}
