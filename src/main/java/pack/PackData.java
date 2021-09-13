package pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hide.common.entity.EntityDebugAABB;
import hide.common.entity.EntityDebugLine;
import hide.common.entity.RenderDebug;
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
	public final List<PackInfo> packInfo = new ArrayList<>();

	/** 弾 ショートネーム - MagazineData MAP */
	public final Map<String, MagazineData> magazineDataMap = new HashMap<>();

	/** 銃 ショートネーム - GunData MAP */
	public final Map<String, GunData> gunDataMap = new HashMap<>();

	/** アタッチメント ショートネーム - Attachment MAP */
	public final Map<String, AttachmentsData> attachmentDataMap = new HashMap<>();

	/** アイコン 登録名 - byte[] MAP */
	public final Map<String, byte[]> iconMap = new HashMap<>();

	/** サウンド 登録名 - byte[] MAP */
	public final Map<String, byte[]> soundMap = new HashMap<>();

	/** サイトの画像 登録名 - byte[] MAP */
	public final Map<String, byte[]> scopeMap = new HashMap<>();

	/** テクスチャ 登録名 - byte[] MAP */
	public final Map<String, byte[]> textureMap = new HashMap<>();

	/** モデル 登録名 - Map<String,ModelPart> MAP */
	public final Map<String, HideModel> modelMap = new HashMap<>();

	private static long sessionTime;

	static PackData CurrentData = new PackData();

	/** 登録名からGunData取得 */
	public static ItemData getItemData(ItemStack item) {
		Class<? extends Item> clazz = item.getItem().getClass();
		if (ItemGun.class.equals(clazz)) {
			return PackData.getGunData(HideGunNBT.DATA_NAME.get(HideGunNBT.getHideTag(item)));
		}
		if (ItemMagazine.class.equals(clazz)) {
			return PackData.getBulletData(HideGunNBT.DATA_NAME.get(HideGunNBT.getHideTag(item)));
		}
		return null;
	}

	/** 登録名からGunData取得 */
	public static GunData getGunData(String name) {
		return CurrentData.gunDataMap.get(name);
	}

	/**リストは編集しないで*/
	public static Collection<GunData> getGunData() {
		return CurrentData.gunDataMap.values();
	}

	/** 登録名からBulletData取得 */
	public static MagazineData getBulletData(String name) {
		return CurrentData.magazineDataMap.get(name);
	}

	/**リストは編集しないで*/
	public static Collection<MagazineData> getMagazineData() {
		return CurrentData.magazineDataMap.values();
	}

	/**登録名からアタッチメントを取得*/
	public static AttachmentsData getAttachmentData(String name) {
		return CurrentData.attachmentDataMap.get(name);
	}

	/**リソース名からモデルを取得*/
	public static HideModel getModel(String name) {
		if (name.contains(":"))
			name = name.split(":", 2)[1];
		return CurrentData.modelMap.get(name);
	}

	public static long getSessionTime() {
		return sessionTime;
	}

	/** アイテム登録 */
	public static void registerItems(Register<Item> event) {
		IForgeRegistry<Item> register = event.getRegistry();
		ItemGun.INSTANCE = new ItemGun("gun", CurrentData.gunDataMap);
		ItemMagazine.INSTANCE = new ItemMagazine("magazine", CurrentData.magazineDataMap);
		register.register(ItemGun.INSTANCE);
		register.register(ItemMagazine.INSTANCE);
	}

	/** モデル登録 */
	@SideOnly(Side.CLIENT)
	public static void registerModel() {
		RenderingRegistry.registerEntityRenderingHandler(EntityBullet.class, RenderBullet::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityDebugAABB.class, RenderDebug::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityDebugLine.class, RenderDebug::new);
	}

	@SideOnly(Side.CLIENT)
	public static void registerSound(Register<SoundEvent> event) {
		for (String name : CurrentData.soundMap.keySet()) {
			System.out.println("SoundRegister " + name);
			event.getRegistry().register(
					new SoundEvent(new ResourceLocation(HideMod.MOD_ID, name))
							.setRegistryName(name));
		}
	}

	public static void setPack(PackData from) {
		CurrentData.clear();
		CurrentData.from(from);
		sessionTime = System.currentTimeMillis();
	}

	//
	void from(PackData from) {
		attachmentDataMap.putAll(from.attachmentDataMap);
		gunDataMap.putAll(from.gunDataMap);
		iconMap.putAll(from.iconMap);
		magazineDataMap.putAll(from.magazineDataMap);
		modelMap.putAll(from.modelMap);
		scopeMap.putAll(from.scopeMap);
		soundMap.putAll(from.soundMap);
		textureMap.putAll(from.textureMap);

		NamedData.resolvParent(gunDataMap.values());
		NamedData.resolvParent(magazineDataMap.values());
	}

	void clear() {
		attachmentDataMap.clear();
		gunDataMap.clear();
		iconMap.clear();
		magazineDataMap.clear();
		modelMap.clear();
		scopeMap.clear();
		soundMap.clear();
		textureMap.clear();
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
