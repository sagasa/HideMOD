package handler;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import item.model.GunModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderPlayerEvent.Pre;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderHandler {

	/**ヒットマーク 色は描画時に*/
	static final ResourceLocation HitMarker = new ResourceLocation("hidemod", "gui/hitMarker.png");

	/**float型のより詳細なTick*/
	static float RenderTick;

	static GunModel model = new GunModel();

	static Minecraft mc = FMLClientHandler.instance().getClient();

	public static void setRenderTick(float renderTickTime) {
		RenderTick = renderTickTime;
	}

	/**オーバーレイGUI*/
	public static void writeGameOverlay(RenderGameOverlayEvent event){
		ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		//System.out.println(scaledresolution.getScaledWidth()+" "+scaledresolution.getScaledHeight()+" : "+mc.displayWidth+" "+mc.displayHeight);
		int x = scaledresolution.getScaledWidth();
		int y = scaledresolution.getScaledHeight();

		if(event.isCancelable() && event.type == ElementType.CROSSHAIRS)
		{
			writeHitMarker(x, y);
		}

		//
/*
		RenderHelper.enableGUIStandardItemLighting();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(new ItemStack(Item.getByNameOrId("hidemod:gun_ar"),1) , i, j);
		RenderHelper.disableStandardItemLighting();*/


		//System.out.println("render");
	}

	/**プレイヤーハンドラを参照してヒットマーク描画*/
	static void writeHitMarker(int x, int y){
		mc.renderEngine.bindTexture(HitMarker);
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		if(PlayerHandler.HitMarkerTime_H>0){
			GlStateManager.color(1f, 0.0f, 0.0f,Math.max(PlayerHandler.HitMarkerTime/10, 0f));
		}else{
			GlStateManager.color(1f, 1f, 1f,Math.max(PlayerHandler.HitMarkerTime/10, 0f));
		}

		Tessellator tessellator = Tessellator.getInstance();
		tessellator.getWorldRenderer().startDrawingQuads();
		tessellator.getWorldRenderer().addVertexWithUV(x / 2 - 6d, y / 2 + 7d, 0d, 0D / 16D, 9D / 16D);
		tessellator.getWorldRenderer().addVertexWithUV(x / 2 + 7d, y / 2 + 7d, 0d, 9D / 16D, 9D / 16D);
		tessellator.getWorldRenderer().addVertexWithUV(x / 2 + 7d, y / 2 - 6d, 0d, 9D / 16D, 0D / 16D);
		tessellator.getWorldRenderer().addVertexWithUV(x / 2 - 6d, y / 2 - 6d, 0d, 0D / 16D, 0D / 16D);
		tessellator.draw();

		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.disableAlpha();
		GlStateManager.disableBlend();
	}

	/**自分以外の持ってる銃の描画*/
	public static void RenderEntityEvent(Post event) {

	}

	/**自分の持ってる銃の描画 アニメーションとパーツの稼働はこのメゾットのみ*/
	public static void RenderHand(RenderHandEvent event) {
		//model.render(RenderTick,Minecraft.getMinecraft().thePlayer);
	}

}
