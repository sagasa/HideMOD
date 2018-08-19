package types.model;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** モデルの原型 */
public abstract class ModelBase {
	String Texture;

	float ScaleX = 1;
	float ScaleY = 1;
	float ScaleZ = 1;

}
