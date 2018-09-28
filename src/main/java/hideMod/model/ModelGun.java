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
import types.model.DisplayPart;
import types.model.ModelGroup;
import types.model.ModelParameter;
import types.model.ModelPart;
import types.model.Polygon;
import types.model.VertexUV;

@SideOnly(Side.CLIENT)
public class ModelGun extends ModelBase {

	public DisplayPart ModelBody;
	public DisplayPart ModelLeaver;
	public DisplayPart ModelDefaultBarrel;
	public DisplayPart ModelDefaultScope;
	public DisplayPart ModelDefaultMagazine;
	
	public ModelGroup Body;
	public ModelGroup Leaver;
	public ModelGroup Barrel;
	public ModelGroup Magazine;
	
	private static final String BodyName = "Body";
	private static final String MagazineName = "Magazine";
	private static final String BarrelName = "Barrel";
	private static final String LeaverName = "Leaver";

	private CompiledScript RenderScript;

	public ModelGun(Map<String,ModelPart> model) {
		if(model.containsKey(BodyName)){
			Body = (DisplayPart) model.get(BodyName);
		}else if(model.containsKey(MagazineName)){
			Body = (DisplayPart) model.get(MagazineName);
		}else if(model.containsKey(BarrelName)){
			Body = (DisplayPart) model.get(BarrelName);
		}else if(model.containsKey(LeaverName)){
			Body = (DisplayPart) model.get(LeaverName);
		}
	}

	/**
	 * モデル内容 本体 マガジン スライド バレル 弾
	 */

	@Override
	public void render(double x, double y, double z, float yaw, float pitch, float scale) {

	}

	@Override
	protected void scriptInit(String script) throws ScriptException {
		scriptEngine.put("Body", Body);
		scriptEngine.put("Magazine", Magazine);
		scriptEngine.put("Barrel", Barrel);
		scriptEngine.put("Leaver", Leaver);
		scriptEngine.put("reload", 0f);
		scriptEngine.put("shoot", 0f);
		scriptEngine.put("eqip", 0f);
		Compilable compilingEngine = (Compilable) scriptEngine;
		RenderScript = compilingEngine.compile(script);
	}
}
