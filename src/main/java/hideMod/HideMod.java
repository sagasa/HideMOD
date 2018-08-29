package hideMod;

import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
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
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import handler.HideEventHandler;

@Mod(modid = HideMod.MOD_ID,
        name = HideMod.MOD_NAME,
        version = HideMod.MOD_VERSION,
        dependencies = HideMod.MOD_DEPENDENCIES,
        acceptedMinecraftVersions = HideMod.MOD_ACCEPTED_MC_VERSIONS,
        useMetadata = true)

/**メインクラス*/
public class HideMod {
    /** ModId文字列 */
    public static final String MOD_ID = "hidemod";
    /** MOD名称 */
    public static final String MOD_NAME = "HideMod";
    /** MODのバージョン */
    public static final String MOD_VERSION = "α";
    /** 早紀に読み込まれるべき前提MODをバージョン込みで指定 */
    public static final String MOD_DEPENDENCIES = "required-after:Forge@[1.8-11.14.0.1239,)";
    /** 起動出来るMinecraft本体のバージョン。記法はMavenのVersion Range Specificationを検索すること。 */
    public static final String MOD_ACCEPTED_MC_VERSIONS = "[1.8,1.8.9]";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	//イベントハンドラ登録
    	MinecraftForge.EVENT_BUS.register(new HideEventHandler());

    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    	//エンティティのレンダー登録
    	if(FMLCommonHandler.instance().getSide().isClient()) {
    		RegistryRenders();
    	}



    }
    @SideOnly(Side.CLIENT)
    void RegistryRenders(){
    //	Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, meta, location);


    }

    /**ログ出力 試験用*/
    public static void log(Object String){
    	System.out.println("[HideMod] " + String.toString());

    }
}