package pack;

import java.util.HashMap;
import java.util.Map;

import entity.EntityBullet;
import entity.render.RenderBullet;
import hidemod.HideMod;
import io.netty.buffer.ByteBuf;
import items.ItemGun;
import items.ItemMagazine;
import model.ModelPart;
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
import types.attachments.AttachmentsData;
import types.base.DataBase;
import types.items.GunData;
import types.items.MagazineData;

public class PackData {
	/** 弾 ショートネーム - MagazineData MAP */
	public static Map<String, MagazineData> MAGAZINE_DATA_MAP = new HashMap<>();

	/** 銃 ショートネーム - GunData MAP */
	public static Map<String, GunData> GUN_DATA_MAP = new HashMap<>();

	/** アタッチメント ショートネーム - Attachment MAP */
	public static Map<String, AttachmentsData> ATTACHMENT_DATA_MAP = new HashMap<>();

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
		for (String name : SOUND_MAP.keySet()) {
			System.out.println("SoundRegister " + name);
			event.getRegistry().register(
					new SoundEvent(new ResourceLocation(HideMod.MOD_ID, name))
							.setRegistryName(name));
		}
	}

	public static void writeBuffer(ByteBuf buf) {
	}

	public static void writeBuffer(ByteBuf buf, DataBase data) {

	}
}
