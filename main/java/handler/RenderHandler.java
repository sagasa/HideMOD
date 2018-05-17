package handler;

import java.awt.Rectangle;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import item.ItemMagazine;
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
import types.guns.LoadedMagazine;

@SideOnly(Side.CLIENT)
public class RenderHandler {

	/**ヒットマーク 色は描画時に*/
	static final ResourceLocation HitMarker = new ResourceLocation("hidemod", "gui/hitMarker.png");
	/**銃のステータス表示の背景*/
	static final ResourceLocation GunInfoGUI = new ResourceLocation("hidemod", "gui/gunInfo.png");

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

		if(event.isCancelable() && event.type == ElementType.CROSSHAIRS){
			writeHitMarker(x, y);
		}
		if(!event.isCancelable() && event.type == ElementType.HOTBAR){
			writeGunInfo(x,y);
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

	/**ホットバーの上に残弾 射撃モード 使用する弾を描画*/
	private static void writeGunInfo(int x, int y) {
		if(PlayerHandler.loadedMagazines!=null){
			int offset = 0;
			for(LoadedMagazine magazine: PlayerHandler.loadedMagazines){
				if(magazine!=null){
					RenderHelper.enableGUIStandardItemLighting();
					GL11.glEnable(GL12.GL_RESCALE_NORMAL);
					//表示用アイテムスタック
					ItemStack stack = ItemMagazine.makeMagazine(magazine.name, magazine.num);

					mc.getRenderItem().renderItemIntoGUI(stack,  x / 2 + 16 + offset, y - 65);
					mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj,stack ,  x / 2 + 16 + offset, y - 65, null);
					//マガジンが少しでも減っているなら
					if(ItemMagazine.getMagazineSize(stack)>ItemMagazine.getBulletNum(stack)){
						String s = ItemMagazine.getBulletNum(stack)+"/"+ItemMagazine.getMagazineSize(stack);
						mc.fontRendererObj.drawString(s, x / 2 + 32 + offset, y - 59, 0xFFFFFF);
						offset += mc.fontRendererObj.getStringWidth(s);
					}
					GL11.glDisable(GL12.GL_RESCALE_NORMAL);
					RenderHelper.disableStandardItemLighting();

				}
				offset+=16;
			}
			//射撃モード
			if(PlayerHandler.fireMode!=null){
				//インフォの枠
				GlStateManager.enableAlpha();
				GlStateManager.enableBlend();
				Tessellator tessellator = Tessellator.getInstance();
				Rectangle size = new Rectangle(x-120, y-60, 115, 55);
				int Zlevel = -100;
				mc.renderEngine.bindTexture(GunInfoGUI);
				tessellator.getWorldRenderer().startDrawingQuads();
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y, Zlevel, 0, 0);
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y+size.height, Zlevel,0, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y+size.height, Zlevel, 1, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y, Zlevel,1, 0);
				tessellator.draw();
				GlStateManager.disableAlpha();

				//射撃モードを描画
				size = new Rectangle(x-118, y-17, 48, 12);
				mc.renderEngine.bindTexture(new ResourceLocation("hidemod", "gui/fireMode_"+PlayerHandler.fireMode.toString()+".png"));
				tessellator.getWorldRenderer().startDrawingQuads();
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y, Zlevel, 0, 0);
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y+size.height, Zlevel, 0, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y+size.height, Zlevel, 1, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y, Zlevel,1, 0);
				tessellator.draw();

				GlStateManager.disableBlend();
			}
		}
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
