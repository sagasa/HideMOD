package hideMod;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import entity.EntityBullet;
import entity.render.RenderBullet;
import gamedata.HideDamage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.creativetab.CreativeTabs;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.client.model.obj.OBJModel.MaterialLibrary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.RegistryManager;
import handler.HideEventHandler;
import handler.PacketHandler;
import io.PackLoader;
import io.ResourceLoader;

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
					Minecraft.getMinecraft(), "defaultResourcePacks","field_110449_ao");
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
		// Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item,
		// meta, location)

	}
}