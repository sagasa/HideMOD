package pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.EntityBullet;
import entity.render.RenderBullet;
import hidemod.HideMod;
import items.ItemGun;
import items.ItemMagazine;
import model.HideModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import types.PackInfo;
import types.attachments.AttachmentsData;
import types.items.GunData;
import types.items.MagazineData;

public class PackData implements Cloneable {
	/*=================== 配信するもの ========================*/
	/**パックの情報*/
	List<PackInfo> PACK_INFO = new ArrayList<>();

	/** 弾 ショートネーム - MagazineData MAP */
	Map<String, MagazineData> MAGAZINE_DATA_MAP = new HashMap<>();

	/** 銃 ショートネーム - GunData MAP */
	Map<String, GunData> GUN_DATA_MAP = new HashMap<>();

	/** アタッチメント ショートネーム - Attachment MAP */
	Map<String, AttachmentsData> ATTACHMENT_DATA_MAP = new HashMap<>();

	/** アイコン 登録名 - byte[] MAP */
	Map<String, byte[]> ICON_MAP = new HashMap<>();

	/** サウンド 登録名 - byte[] MAP */
	Map<String, byte[]> SOUND_MAP = new HashMap<>();

	/** テクスチャ 登録名 - byte[] MAP */
	Map<String, byte[]> TEXTURE_MAP = new HashMap<>();

	/** モデル 登録名 - Map<String,ModelPart> MAP */
	Map<String, HideModel> MODEL_MAP = new HashMap<>();

	static PackData readData = new PackData();

	static PackData currentData = new PackData();

	/** 登録名からGunData取得 */
	public static GunData getGunData(String name) {
		return currentData.GUN_DATA_MAP.get(name);
	}

	/**リストは編集しないで*/
	public static Collection<GunData> getGunData() {
		return currentData.GUN_DATA_MAP.values();
	}

	/** 登録名からBulletData取得 */
	public static MagazineData getBulletData(String name) {
		return currentData.MAGAZINE_DATA_MAP.get(name);
	}

	/**リストは編集しないで*/
	public static Collection<MagazineData> getMagazineData() {
		return currentData.MAGAZINE_DATA_MAP.values();
	}

	/**登録名からアタッチメントを取得*/
	public static AttachmentsData getAttachmentData(String name) {
		return currentData.ATTACHMENT_DATA_MAP.get(name);
	}

	/**登録名からアタッチメントを取得*/
	public static HideModel getModel(String name) {
		return currentData.MODEL_MAP.get(name);
	}

	/** アイテム登録 */
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		register.register(ItemGun.INSTANCE);
		register.register(ItemMagazine.INSTANCE);
	}

	/** モデル登録 */
	@SideOnly(Side.CLIENT)
	public static void registerModel() {
		RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, new IRenderFactory<EntityBullet>() {
			@Override
			public Render createRenderFor(RenderManager manager) {
				return new RenderBullet(manager);
			}
		});
	}

	@SideOnly(Side.CLIENT)
	public static void registerSound(Register<SoundEvent> event) {
		for (String name : currentData.SOUND_MAP.keySet()) {
			System.out.println("SoundRegister " + name);
			event.getRegistry().register(
					new SoundEvent(new ResourceLocation(HideMod.MOD_ID, name))
							.setRegistryName(name));
		}
	}

	/**シャドーコピーだから注意*/
	@Override
	public PackData clone() {
		try {
			PackData data = (PackData) super.clone();
			data.ATTACHMENT_DATA_MAP.putAll(ATTACHMENT_DATA_MAP);
			data.GUN_DATA_MAP.putAll(GUN_DATA_MAP);
			data.ICON_MAP.putAll(ICON_MAP);
			data.MAGAZINE_DATA_MAP.putAll(MAGAZINE_DATA_MAP);
			data.MODEL_MAP.putAll(MODEL_MAP);
			data.SOUND_MAP.putAll(SOUND_MAP);
			data.TEXTURE_MAP.putAll(TEXTURE_MAP);
			return data;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
