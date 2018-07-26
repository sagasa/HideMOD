package handler;

import java.awt.Rectangle;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import hideMod.model.GunModel;
import item.ItemGun;
import item.ItemMagazine;
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
			//開始位置
			writeGunInfo(x-120, y-65);
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
			//射撃モード
			if(PlayerHandler.fireMode!=null){
				//インフォの枠
				GlStateManager.enableAlpha();
				GlStateManager.enableBlend();
				Tessellator tessellator = Tessellator.getInstance();
				Rectangle size = new Rectangle(x, y, 115, 60);
				int Zlevel = 0;
				mc.renderEngine.bindTexture(GunInfoGUI);
				tessellator.getWorldRenderer().startDrawingQuads();
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y, Zlevel, 0, 0);
				tessellator.getWorldRenderer().addVertexWithUV(size.x, size.y+size.height, Zlevel,0, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y+size.height, Zlevel, 1, 1);
				tessellator.getWorldRenderer().addVertexWithUV(size.x+size.width, size.y, Zlevel,1, 0);
				tessellator.draw();
				GlStateManager.disableAlpha();

				//マガジン
				int offset = 0;
				for(LoadedMagazine magazine: PlayerHandler.loadedMagazines){
					if(magazine!=null){
						RenderHelper.enableGUIStandardItemLighting();
						GL11.glEnable(GL12.GL_RESCALE_NORMAL);
						//表示用アイテムスタック
						ItemStack stack = ItemMagazine.makeMagazine(magazine.name, magazine.num);
						mc.getRenderItem().renderItemIntoGUI(stack,  x + 1 + offset, y +1);
						mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj,stack ,  x +1 + offset, y +1, null);
						GL11.glDisable(GL12.GL_RESCALE_NORMAL);
						RenderHelper.disableStandardItemLighting();

					}
					offset+=16;
				}
				//射撃モードを描画
				mc.fontRendererObj.drawString(PlayerHandler.fireMode.toString().toUpperCase(), x+40, y+39, 0xFFFFFF);
				//残弾
				float fontSize = 1.8f;
				GlStateManager.scale(fontSize, fontSize, fontSize);
				mc.fontRendererObj.drawString(LoadedMagazine.getLoadedNum(PlayerHandler.loadedMagazines)+"/"+PlayerHandler.getCanLoadMagazineNum(Minecraft.getMinecraft().thePlayer), (x+5)/fontSize, (y+21)/fontSize, 0xFFFFFF, false);
				GlStateManager.scale(1/fontSize, 1/fontSize, 1/fontSize);
				//使用する弾
				mc.fontRendererObj.drawString(ItemMagazine.getBulletData(PlayerHandler.UsingBulletName).ITEM_INFO.NAME_DISPLAY, x+40, y+50, 0xFFFFFF);

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
			GlStateManager.color(1f, 0.0f, 0.0f,(float) Math.max(Math.min(0.4, PlayerHandler.HitMarkerTime/10), 0f));
		}else{
			GlStateManager.color(1f, 1f, 1f,(float) Math.max(Math.min(0.4, PlayerHandler.HitMarkerTime/10), 0f));
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
		ItemStack item = mc.thePlayer.getCurrentEquippedItem();
		if(ItemGun.isGun(item)){
			((ItemGun)item.getItem()).Model.render(RenderTick,Minecraft.getMinecraft().thePlayer);
		}
	}

}
