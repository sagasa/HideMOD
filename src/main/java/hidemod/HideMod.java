package hidemod;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.Logger;

import handler.client.HideItemRender;
import handler.client.InputHandler;
import helper.HideDamage;
import hide.common.HideEventHandler;
import hide.common.PacketHandler;
import hide.common.entity.EntityDebugAABB;
import hide.core.HideBase;
import hide.core.HideSubSystem;
import hide.guns.HideGunCommand;
import hide.guns.HideGunSystem;
import hide.guns.entiry.EntityBullet;
import hide.model.gltf.GltfLoader;
import hide.opengl.ServerRenderContext;
import hide.types.effects.Sound;
import hide.types.guns.ProjectileData;
import hide.types.util.DataView;
import hide.types.value.Operator;
import hide.ux.HideUXSystem;
import items.ItemGun;
import items.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import overwrite.HideHook;
import pack.PackLoader;
import pack.ResourceLoader;

@Mod(modid = HideMod.MOD_ID, name = HideMod.MOD_NAME, dependencies = HideMod.MOD_DEPENDENCIES, acceptedMinecraftVersions = HideMod.MOD_ACCEPTED_MC_VERSIONS, useMetadata = true)

/** メインクラス */
public class HideMod {
	/** ModId文字列 */
	public static final String MOD_ID = "hidemod";
	/** MOD名称 */
	public static final String MOD_NAME = "HideMod";
	/** 早紀に読み込まれるべき前提MODをバージョン込みで指定 */
	public static final String MOD_DEPENDENCIES = "required:forge@[14.23.4.2705,);";
	/** 起動出来るMinecraft本体のバージョン。記法はMavenのVersion Range Specificationを検索すること。 */
	public static final String MOD_ACCEPTED_MC_VERSIONS = "[1.12,1.12.2]";

	private static File ModeDir;

	public static File getModDir() {
		return ModeDir;
	}

	public static Logger LOGGER;

	private HideSubSystem subSystem = new HideSubSystem();

	/* イニシャライズ */
	@EventHandler
	public void construct(FMLConstructionEvent event) {
		MinecraftForge.EVENT_BUS.register(new HideEventHandler());
		subSystem.register(event, new HideGunSystem());
		subSystem.register(event, new HideUXSystem());
	}
	//

	@EventHandler
	public void start(FMLServerStartingEvent event) {
		subSystem.serverStart(event);
		event.registerServerCommand(new HideGunCommand());
	}

	@EventHandler
	public void serverStop(FMLServerStoppedEvent event) {
		subSystem.serverStop(event);
		if (event.getSide().isServer())
			//サーバーサイドの場合1回しか呼ばれないのでコンテキスト破棄
			ServerRenderContext.destroyContext();
	}

	// アイテム登録
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ModeDir = new File(event.getModConfigurationDirectory().getParentFile(), "/mods/");
		// パケットの初期設定
		PacketHandler.init();
		// ダメージの初期設定
		HideDamage.init();
		// ロガー保存
		LOGGER = event.getModLog();

		ProjectileData proj = new ProjectileData();
		ProjectileData proj2 = new ProjectileData();
		proj.put(ProjectileData.SoundShoot, Operator.SET, new Sound("Fuck", 40));
		DataView<ProjectileData> dataView = new DataView<>(ProjectileData.class, 1);
		dataView.setBase(proj);
		dataView.setModifier(0, proj2);
		System.out.println("\n\\n\\n\\n\\n===========================================================");

		System.out.println(dataView.getData(ProjectileData.SoundShoot).get(Sound.Name));
		;

		// エンティティ登録
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "entity_bullet"), EntityBullet.class,
				"entity_bullet", 1, MOD_ID, 512, 1, false);
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "entity_aabb"), EntityDebugAABB.class,
				"entity_aabb", 10, MOD_ID, 32, 20, false);

		if (FMLCommonHandler.instance().getSide().isClient()) {
			HideItemRender.registerLoader();
			// リソースローダーを追加
			List<IResourcePack> defaultResourcePacks = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class,
					Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
			defaultResourcePacks.add(new ResourceLoader());

			Minecraft.getMinecraft().refreshResources();

			Minecraft.getMinecraft().getFramebuffer().enableStencil();
			//HideBaseのフックを追加
			HideHook.initHookClient();
			System.out.println(defaultResourcePacks);

			ServerRenderContext.SUPPORT_CONTEXT = true;
		} else {
			//レンダリング用コンテキスト
			ServerRenderContext.initContext();
		}

		// パック読み込み
		PackLoader.load();

		HideBase.HideDirEntry.setChangeListener(PackLoader::reloadInGame);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		// エンティティのレンダー登録
		if (FMLCommonHandler.instance().getSide().isClient()) {
			initClient();
		}

		GltfLoader.test();
	}

	@SideOnly(Side.CLIENT)
	void initClient() {
		// キーバインド設定
		InputHandler.init();
		//レンダー登録
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ItemGun.INSTANCE, 0,
				new ModelResourceLocation("hidemod:gun", "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ItemMagazine.INSTANCE, 0,
				new ModelResourceLocation("hidemod:magazine", "inventory"));
		// Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item,
		// meta, location)
	}

	/** クライアントサイド限定 playerを取得 */
	@SideOnly(Side.CLIENT)
	public static EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().player;
	}

	/**Mod内ネットID*/
	private static int netID;
	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("HideMod");

	/**自動でIDを割り振る登録ラッパー*/
	public static <REQ extends IMessage, REPLY extends IMessage> void registerNetMsg(Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> requestMessageType, Side side) {
		NETWORK.registerMessage(messageHandler, requestMessageType, netID, side);
		netID++;
	}
}