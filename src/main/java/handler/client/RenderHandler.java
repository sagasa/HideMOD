package handler.client;

import java.awt.Rectangle;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import gamedata.LoadedMagazine.Magazine;
import guns.GunController;
import handler.PlayerHandler.EquipMode;
import helper.HideNBT;
import hidemod.HideMod;
import items.ItemGun;
import items.ItemMagazine;
import model.HideModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pack.PackData;
import types.items.MagazineData;

@SideOnly(Side.CLIENT)
public class RenderHandler {

	/** ヒットマーク 色は描画時に */
	static final ResourceLocation HitMarker = new ResourceLocation("hidemod", "gui/hitmarker.png");
	/** 銃のステータス表示の背景 */
	static final ResourceLocation GunInfoGUI = new ResourceLocation("hidemod", "gui/guninfo.png");

	/** float型のより詳細なTick */
	static float RenderTick;

	static Minecraft mc = FMLClientHandler.instance().getClient();

	public static void setRenderTick(float renderTickTime) {
		RenderTick = renderTickTime;
	}

	/** オーバーレイGUI */
	public static void writeGameOverlay(RenderGameOverlayEvent event) {
		ScaledResolution scaledresolution = new ScaledResolution(mc);
		// System.out.println(scaledresolution.getScaledWidth()+"
		// "+scaledresolution.getScaledHeight()+" : "+mc.displayWidth+"
		// "+mc.displayHeight);
		int x = scaledresolution.getScaledWidth();
		int y = scaledresolution.getScaledHeight();

		if (event.isCancelable() && event.getType() == ElementType.CROSSHAIRS) {
			ClientPlayerData data = HidePlayerData.getClientData(HideMod.getPlayer());
			if (EquipMode.getEquipMode(data.gunMain, data.gunOff) != EquipMode.None)
				event.setCanceled(true);
			writeHitMarker(x, y);
			writeScope(x, y);
		}
		if (!event.isCancelable() && event.getType() == ElementType.HOTBAR) {
			// 開始位置
			writeGunInfo(x - 120, y - 65);
		}
		//
		/*
		 * RenderHelper.enableGUIStandardItemLighting(); GL11.glColor4f(1.0F, 1.0F,
		 * 1.0F, 1.0F); GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		 * Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(new
		 * ItemStack(Item.getByNameOrId("hidemod:gun_ar"),1) , i, j);
		 * RenderHelper.disableStandardItemLighting();
		 */

		// System.out.println("render");
	}

	/** スコープ */
	private static void writeScope(int x, int y) {
		if (HideScope.Scope == null) {
			// HideScope.setScope(8f, 0.8f, new ScopeMask());
		}
		HideScope.renderOnGUI();// TODO そのうち統合
	}

	/** 画面右下に 残弾 射撃モード 使用する弾を描画 */
	private static void writeGunInfo(int x, int y) {
		ClientPlayerData data = HidePlayerData.getClientData(HideMod.getPlayer());
		if (data == null)
			return;
		EquipMode em = EquipMode.getEquipMode(data.gunMain, data.gunOff);
		if (em == EquipMode.Main) {
			writeGunInfo(x, y, data.gunMain);
		} else if (em == EquipMode.Off) {
			writeGunInfo(x, y, data.gunOff);
		} else if (em == EquipMode.Dual) {
			writeGunInfo(x, y, data.gunMain);
			writeGunInfo(x - 120, y, data.gunOff);
		} else if (em == EquipMode.OtherDual) {
			writeGunInfo(x, y, data.gunMain);
			writeGunInfo(x - 120, y, data.gunOff);
		}

	}

	static ItemStack stack = new ItemStack(ItemMagazine.INSTANCE);

	/** 銃のステータスGUI描画 */
	private static void writeGunInfo(int x, int y, GunController gun) {
		if (gun == null || !gun.isGun()) {
			return;
		}
		// インフォの枠
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		Tessellator tessellator = Tessellator.getInstance();
		Rectangle size = new Rectangle(x, y, 115, 60);
		int Zlevel = 0;
		GL11.glPushMatrix();
		mc.renderEngine.bindTexture(GunInfoGUI);
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(7, DefaultVertexFormats.POSITION_TEX);
		buf.pos(size.x, size.y, Zlevel).tex(0, 0).endVertex();
		buf.pos(size.x, size.y + size.height, Zlevel).tex(0, 1).endVertex();
		buf.pos(size.x + size.width, size.y + size.height, Zlevel).tex(1, 1).endVertex();
		buf.pos(size.x + size.width, size.y, Zlevel).tex(1, 0).endVertex();
		tessellator.draw();
		GL11.glPopMatrix();
		GlStateManager.disableAlpha();

		// マガジン
		int offset = 0;
		for (Magazine magazine : gun.magazine.getList()) {
			if (magazine != null) {
				MagazineData data = PackData.getBulletData(magazine.name);
				if (data != null) {
					RenderHelper.enableGUIStandardItemLighting();
					GL11.glEnable(GL12.GL_RESCALE_NORMAL);
					// 表示用アイテムスタック
					ItemMagazine.makeMagazineNBT(stack, data);
					HideNBT.setMagazineBulletNum(stack, magazine.num);
					mc.getRenderItem().renderItemIntoGUI(stack, x + 1 + offset, y + 1);
					mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x + 1 + offset, y + 1, null);
					GL11.glDisable(GL12.GL_RESCALE_NORMAL);
					RenderHelper.disableStandardItemLighting();
				}
			}
			offset += 16;
		}
		// 射撃モードを描画
		mc.fontRenderer.drawString(gun.getFireMode().toString().toUpperCase(), x + 40, y + 39, 0xFFFFFF);
		// 残弾
		float fontSize = 1.8f;
		GlStateManager.scale(fontSize, fontSize, fontSize);
		mc.fontRenderer.drawString(gun.magazine.getLoadedNum() + "/" + gun.getCanUseBulletNum(),
				(x + 5) / fontSize, (y + 21) / fontSize, 0xFFFFFF, false);
		GlStateManager.scale(1 / fontSize, 1 / fontSize, 1 / fontSize);
		// 使用する弾
		mc.fontRenderer.drawString(ItemMagazine.getMagazineName(gun.getUseMagazine()), x + 40, y + 50, 0xFFFFFF);

		GlStateManager.disableBlend();
	}

	/** プレイヤーハンドラを参照してヒットマーク描画 */
	static void writeHitMarker(int x, int y) {
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		mc.renderEngine.bindTexture(HitMarker);

		if (HideViewHandler.HitMarker_H) {
			GlStateManager.color(1f, 0.0f, 0.0f,
					(float) Math.max(Math.min(0.4, HideViewHandler.HitMarkerTime / 10), 0f));
		} else {
			GlStateManager.color(1f, 1f, 1f, (float) Math.max(Math.min(0.4, HideViewHandler.HitMarkerTime / 10), 0f));
		}
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(7, DefaultVertexFormats.POSITION_TEX);
		buf.pos(x / 2 - 6d, y / 2 + 7d, 0d).tex(0D / 16D, 9D / 16D).endVertex();
		buf.pos(x / 2 + 7d, y / 2 + 7d, 0d).tex(9D / 16D, 9D / 16D).endVertex();
		buf.pos(x / 2 + 7d, y / 2 - 6d, 0d).tex(9D / 16D, 0D / 16D).endVertex();
		buf.pos(x / 2 - 6d, y / 2 - 6d, 0d).tex(0D / 16D, 0D / 16D).endVertex();
		tessellator.draw();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		GlStateManager.disableAlpha();
		GlStateManager.disableBlend();

		// mc.ingameGUI.drawTexturedModalRect(100, 100, 0, 0, 100, 100);
	}

	static ModelPlayer model = new ModelPlayer(0, false);

	/** 自分以外の持ってる銃の描画 */
	public static void RenderEntityEvent(Post event) {
		model.isChild = false;
		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.scale(-0.9F, -0.9F, 0.9F);
		float f = 0.0625F;
		GlStateManager.translate(2.0F, -1.501F, 0.0F);

		EntityLivingBase player = event.getEntity();

		model.render(HideMod.getPlayer(), player.limbSwing, player.limbSwingAmount, 0f, player.rotationYaw,
				player.rotationPitch, 0.0625F);

		GlStateManager.popMatrix();

		ItemStack item = mc.player.getHeldItemMainhand();
		if (ItemGun.isGun(item)) {
			HideModel model = PackData.getModel("default_modelstg44");
			if (model != null)
				model.render();
			// ((ItemGun)item.getItem()).Model.render(RenderTick,Minecraft.getMinecraft().thePlayer);
		}
	}

	/** 自分の持ってる銃の描画 アニメーションとパーツの稼働はこのメゾットのみ */
	public static void RenderHand(RenderHandEvent event) {
		ItemStack item = mc.player.getHeldItemMainhand();
		if (ItemGun.isGun(item)) {
			HideModel model = PackData.getModel("default_modelstg44");
			if (model != null) {
				if (mc.gameSettings.thirdPersonView != 0)
					return;
				int side = 1;
				if (mc.gameSettings.mainHand == EnumHandSide.LEFT)
					side = -1;
				GlStateManager.pushMatrix();
				GlStateManager.rotate(90, 0, 1, 0);
				GlStateManager.translate(1, -1.0, 0.6 * side);
				GlStateManager.rotate(-5 * side, 0, 1, 0);

				model.render();
				GlStateManager.popMatrix();
			}
			// ((ItemGun)item.getItem()).Model.render(RenderTick,Minecraft.getMinecraft().thePlayer);
		}
	}

}
