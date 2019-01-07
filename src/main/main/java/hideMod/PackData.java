package hideMod;

import java.util.HashMap;
import java.util.Map;

import entity.EntityBullet;
import entity.render.RenderBullet;
import hideMod.model.ModelPart;
import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import types.guns.BulletData;
import types.guns.GunData;

public class PackData {
	/** 弾 ショートネーム - BulletData MAP */
	public static Map<String, BulletData> BULLET_DATA_MAP = new HashMap<String, BulletData>();

	/** 銃 ショートネーム - BulletData MAP */
	public static Map<String, GunData> GUN_DATA_MAP = new HashMap<String, GunData>();

	/** アイコン 登録名 - byte[] MAP */
	public static Map<String, byte[]> ICON_MAP = new HashMap<String, byte[]>();

	/** サウンド 登録名 - byte[] MAP */
	public static Map<String, byte[]> SOUND_MAP = new HashMap<String, byte[]>();

	/** テクスチャ 登録名 - byte[] MAP */
	public static Map<String, byte[]> TEXTURE_MAP = new HashMap<String, byte[]>();

	/** モデル 登録名 - Map<String,ModelPart> MAP */
	public static Map<String, Map<String, ModelPart>> MODEL_MAP = new HashMap<String, Map<String, ModelPart>>();

	/** 登録名からGunData取得 */
	public static GunData getGunData(String name) {
		return GUN_DATA_MAP.get(name);
	}

	/** 登録名からBulletData取得 */
	public static BulletData getBulletData(String name) {
		return BULLET_DATA_MAP.get(name);
	}

	/** アイテム登録 */
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		for (GunData data : GUN_DATA_MAP.values()) {
			Item item = new ItemGun(data);
			register.register(item);
		}
		for (BulletData data : BULLET_DATA_MAP.values()) {
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
					new ResourceLocation(HideMod.MOD_ID, item.BulletData.ITEM_SHORTNAME), "inventory"));
		}
		RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, new IRenderFactory() {
			@Override
			public Render createRenderFor(RenderManager manager) {
				return new RenderBullet(manager);
			}
		});
	}

}
