package hideMod.model;

import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 読み込んだままのモデル
 * 描画、アニメーションはRenderで */
@SideOnly(Side.CLIENT)
public class ModelGun extends ModelBase {

	public DisplayPart ModelBody;
	public DisplayPart ModelLeaver;
	public DisplayPart ModelDefaultBarrel;
	public DisplayPart ModelDefaultScope;
	public DisplayPart ModelDefaultMagazine;

	private static final String BodyName = "Body";
	private static final String MagazineName = "Magazine";
	private static final String BarrelName = "Barrel";
	private static final String LeaverName = "Leaver";

	public ModelGun(Map<String,ModelPart> model) {
		if(model.containsKey(BodyName)){
			ModelBody = (DisplayPart) model.get(BodyName);
		}else if(model.containsKey(MagazineName)){
			ModelDefaultMagazine = (DisplayPart) model.get(MagazineName);
		}else if(model.containsKey(BarrelName)){
			ModelDefaultBarrel = (DisplayPart) model.get(BarrelName);
		}else if(model.containsKey(LeaverName)){
			ModelLeaver = (DisplayPart) model.get(LeaverName);
		}
	}
}
