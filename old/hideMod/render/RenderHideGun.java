package hideMod.render;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import hideMod.model.ModelGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class RenderHideGun extends RenderHideModel {
	private static final ScriptEngineManager EngineManager = new ScriptEngineManager();
	protected ScriptEngine scriptEngine = EngineManager.getEngineByName("JavaScript");

	private CompiledScript RenderScript;

	public DisplayGroup Body;
	public DisplayGroup Leaver;
	public DisplayGroup Barrel;
	public DisplayGroup Magazine;

	public RenderHideGun(ModelGun model) {
		Model = model;
	}

	private ResourceLocation Textur;
	private ModelGun Model;

	/** テクスチャの画像を指定 */
	public void setTexture(ResourceLocation texture) {
		Textur = texture;
	}

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

	/** モデルを描画 */
	public void render(double x, double y, double z, float yaw, float pitch, float scale) {
		GlStateManager.pushMatrix();
		// GlStateManager.disableCull();
		// 位置を合わせる
		GlStateManager.translate(x, y, z);
		// 角度を視線に合わせる
		GlStateManager.rotate(pitch, (float) Math.cos(Math.toRadians(yaw)), 0.0F,
				(float) Math.sin(Math.toRadians(yaw)));
		GlStateManager.rotate(yaw + 90, 0.0F, -1.0F, 0.0F);
		//スケール調整
		GlStateManager.scale(scale, scale, scale);
		// スクリプト実行
		try {
			RenderScript.eval();
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		// GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	public void render(float partialTicks, EntityPlayerSP player) {
		Minecraft.getMinecraft().renderEngine.bindTexture(Textur);
		GlStateManager.translate(0, player.getEyeHeight(), 0);
		float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
		float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
		render(player.posX, player.posY, player.posZ, yaw, pitch, 1);
	}
}
