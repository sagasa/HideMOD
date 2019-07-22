package hidemod;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import entity.EntityBullet;
import handler.HideEventHandler;
import handler.PacketHandler;
import helper.HideDamage;
import items.ItemGun;
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
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pack.PackLoader;
import pack.ResourceLoader;

@Mod(modid = HideMod.MOD_ID, name = HideMod.MOD_NAME, version = HideMod.MOD_VERSION, dependencies = HideMod.MOD_DEPENDENCIES, acceptedMinecraftVersions = HideMod.MOD_ACCEPTED_MC_VERSIONS, useMetadata = true)

/** メインクラス */
public class HideMod {
	/** ModId文字列 */
	public static final String MOD_ID = "hidemod";
	/** MOD名称 */
	public static final String MOD_NAME = "HideMod";
	/** MODのバージョン */
	public static final String MOD_VERSION = "α";
	/** 早紀に読み込まれるべき前提MODをバージョン込みで指定 */
	public static final String MOD_DEPENDENCIES = "required:forge@[14.23.4.2705,);";
	/** 起動出来るMinecraft本体のバージョン。記法はMavenのVersion Range Specificationを検索すること。 */
	public static final String MOD_ACCEPTED_MC_VERSIONS = "[1.12,1.12.2]";

	private static File ModeDir;
	public static File getModDir() {
		return ModeDir;
	}

	private static final Logger LOGGER = LogManager.getLogger();

	/* イニシャライズ */
	@EventHandler
	public void construct(FMLConstructionEvent event) {
		MinecraftForge.EVENT_BUS.register(new HideEventHandler());
	}
	//

	// アイテム登録
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ModeDir = new File(event.getModConfigurationDirectory().getParentFile(), "/mods/");
		// パック読み込み
		PackLoader.load(event);
		// パケットの初期設定
		PacketHandler.init();

		// ダメージの初期設定
		HideDamage.init();
		// エンティティ登録
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "entity_bullet"), EntityBullet.class,
				"entity_bullet", 1, MOD_ID, 512, 1, false);

		if (FMLCommonHandler.instance().getSide().isClient()) {
			// リソースローダーを追加
			List<IResourcePack> defaultResourcePacks = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class,
					Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
			defaultResourcePacks.add(new ResourceLoader());

			Minecraft.getMinecraft().refreshResources();
			System.out.println(defaultResourcePacks);
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		// エンティティのレンダー登録
		if (FMLCommonHandler.instance().getSide().isClient()) {
			RegistryRenders();
		}

	}

	@SideOnly(Side.CLIENT)
	void RegistryRenders() {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ItemGun.INSTANCE, 0,
				new ModelResourceLocation("hidemod:gun", "inventory"));
		// Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item,
		// meta, location)
	}

	/** クライアントサイド限定 playerを取得 */
	@SideOnly(Side.CLIENT)
	public static EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().player;
	}
}