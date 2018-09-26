package hideMod.model;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** モデルの原型 */
public abstract class ModelBase {
	private static final ScriptEngineManager EngineManager = new ScriptEngineManager();
	protected ScriptEngine scriptEngine = EngineManager.getEngineByName("JavaScript");

	String Texture;

	float ScaleX = 1;
	float ScaleY = 1;
	float ScaleZ = 1;

	/**スクリプトのイニシャライズ
	 * @throws ScriptException */
	abstract protected void scriptInit(String name) throws ScriptException;

	/**最終的な描画処理*/
	abstract public void render(double x,double y,double z,float yaw,float pitch,float scale);
}
