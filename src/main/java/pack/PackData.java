package pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hide.common.entity.EntityDebugAABB;
import hide.common.entity.RenderAABB;
import hide.guns.HideGunNBT;
import hide.guns.entiry.EntityBullet;
import hide.guns.entiry.RenderBullet;
import hide.types.base.NamedData;
import hide.types.items.AttachmentsData;
import hide.types.items.GunData;
import hide.types.items.ItemData;
import hide.types.items.MagazineData;
import hide.types.pack.PackInfo;
import hidemod.HideMod;
import items.ItemGun;
import items.ItemMagazine;
import model.HideModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

public class PackData {
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

	/** サイトの画像 登録名 - byte[] MAP */
	Map<String, byte[]> SCOPE_MAP = new HashMap<>();

	/** テクスチャ 登録名 - byte[] MAP */
	Map<String, byte[]> TEXTURE_MAP = new HashMap<>();

	/** モデル 登録名 - Map<String,ModelPart> MAP */
	Map<String, HideModel> MODEL_MAP = new HashMap<>();

	static PackData readData = new PackData();

	static PackData currentData = new PackData();

	/** 登録名からGunData取得 */
	public static ItemData getItemData(ItemStack item) {
		Class<? extends Item> clazz = item.getItem().getClass();
		if (ItemGun.class.equals(clazz)) {
			return PackData.getGunData(HideGunNBT.getHideTag(item).getString(HideGunNBT.DATA_NAME));
		}
		if (ItemMagazine.class.equals(clazz)) {
			return PackData.getBulletData(HideGunNBT.getHideTag(item).getString(HideGunNBT.DATA_NAME));
		}
		return null;
	}

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

	/**リソース名からモデルを取得*/
	public static HideModel getModel(String name) {
		if (name.contains(":"))
			name = name.split(":", 2)[1];
		return currentData.MODEL_MAP.get(name);
	}

	/** アイテム登録 */
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		ItemGun.INSTANCE = new ItemGun("gun", currentData.GUN_DATA_MAP);
		ItemMagazine.INSTANCE = new ItemMagazine("magazine", currentData.MAGAZINE_DATA_MAP);
		register.register(ItemGun.INSTANCE);
		register.register(ItemMagazine.INSTANCE);
	}

	/** モデル登録 */
	@SideOnly(Side.CLIENT)
	public static void registerModel() {
		RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, RenderBullet::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityDebugAABB.class, RenderAABB::new);
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

	//
	public void from(PackData from) {
		ATTACHMENT_DATA_MAP.putAll(from.ATTACHMENT_DATA_MAP);
		GUN_DATA_MAP.putAll(from.GUN_DATA_MAP);
		ICON_MAP.putAll(from.ICON_MAP);
		MAGAZINE_DATA_MAP.putAll(from.MAGAZINE_DATA_MAP);
		MODEL_MAP.putAll(from.MODEL_MAP);
		SCOPE_MAP.putAll(from.SCOPE_MAP);
		SOUND_MAP.putAll(from.SOUND_MAP);
		TEXTURE_MAP.putAll(from.TEXTURE_MAP);

		NamedData.resolvParent(GUN_DATA_MAP.values());
		NamedData.resolvParent(MAGAZINE_DATA_MAP.values());
	}

	public void clear() {
		ATTACHMENT_DATA_MAP.clear();
		GUN_DATA_MAP.clear();
		ICON_MAP.clear();
		MAGAZINE_DATA_MAP.clear();
		MODEL_MAP.clear();
		SCOPE_MAP.clear();
		SOUND_MAP.clear();
		TEXTURE_MAP.clear();
	}

	private void clearAndCopy(Map to, Map from) {
		to.clear();
		to.putAll(from);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO 自動生成されたメソッド・スタブ
		return super.clone();
	}
}
