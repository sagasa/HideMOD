package handler.client;

import java.awt.Rectangle;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import helper.HideMath;
import hide.core.HidePlayerDataManager;
import hide.guns.CommonGun;
import hide.guns.HideGunNBT;
import hide.guns.PlayerData.ClientPlayerData;
import hide.guns.PlayerData.EquipMode;
import hide.guns.data.LoadedMagazine.Magazine;
import hide.types.items.GunData;
import hide.types.items.ItemData;
import hide.types.items.MagazineData;
import items.ItemGun;
import items.ItemMagazine;
import model.AnimationType;
import model.HideModel;
import model.IRenderProperty.SelfProp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pack.PackData;

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
	public static void writeGameOverlay(RenderGameOverlayEvent.Pre event) {
		ScaledResolution scaledresolution = new ScaledResolution(mc);
		// System.out.println(scaledresolution.getScaledWidth()+"
		// "+scaledresolution.getScaledHeight()+" : "+mc.displayWidth+"
		// "+mc.displayHeight);
		int x = scaledresolution.getScaledWidth();
		int y = scaledresolution.getScaledHeight();
		ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
		if (event.isCancelable() && event.getType() == ElementType.CROSSHAIRS) {
			//	if (EquipMode.getEquipMode(data.gunMain, data.gunOff) != EquipMode.None)
			//		event.setCanceled(true);
			writeHitMarker(x, y);

			event.setCanceled(HideViewHandler.writeScope() || data.getAds(event.getPartialTicks()) > 0);
			writeGunInfo(x - 120, y - 65, event.getPartialTicks());

			float ads = data.getAds(event.getPartialTicks());
			if (ads == 0)
				HideViewHandler.clearADS();
			else {
				HideViewHandler.setADS(data.gunMain.getGunData().get(GunData.UseScope) ? data.gunMain.getGunData().get(GunData.ScopeName) : null,
						HideMath.completion(1, data.gunMain.getGunData().get(GunData.ScopeZoom), ads),
						data.gunMain.getGunData().get(GunData.ScopeSize));
			}

			/** スコープ */
			//HideScope.renderOnGUI();
		}
		if (!event.isCancelable() && event.getType() == ElementType.HOTBAR) {
			// 開始位置

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

	/** 画面右下に 残弾 射撃モード 使用する弾を描画
	 * @param partialTicks */
	private static void writeGunInfo(int x, int y, float partialTicks) {
		ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
		if (data == null)
			return;
		EquipMode em = data.CurrentEquipMode;
		if (em == EquipMode.Main) {
			writeGunInfo(x, y, data.gunMain, partialTicks);
		} else if (em == EquipMode.Off) {
			writeGunInfo(x, y, data.gunOff, partialTicks);
		} else if (em == EquipMode.Dual) {
			writeGunInfo(x, y, data.gunMain, partialTicks);
			writeGunInfo(x - 120, y, data.gunOff, partialTicks);
		} else if (em == EquipMode.OtherDual) {
			writeGunInfo(x, y, data.gunMain, partialTicks);
			writeGunInfo(x - 120, y, data.gunOff, partialTicks);
		}

	}

	static ItemStack stack = new ItemStack(ItemMagazine.INSTANCE);

	/** 銃のステータスGUI描画 */
	private static void writeGunInfo(int x, int y, CommonGun gun, float partialTicks) {
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
					HideGunNBT.setMagazineBulletNum(stack, magazine.num);
					mc.getRenderItem().renderItemIntoGUI(stack, x + 1 + offset, y + 1);
					mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x + 1 + offset, y + 1, null);
					GL11.glDisable(GL12.GL_RESCALE_NORMAL);
					RenderHelper.disableStandardItemLighting();
				}
			}
			offset += 16;
		}

		//リロード
		ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
		float reload = data.getReload(partialTicks);
		if (0.01f <= reload) {
			int left = x + 1 + offset;
			int top = y + 1;
			Gui.drawRect(left, top, (int) (left + 16 * reload), top + 16, 0xFFAAAAAA);
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

	static FloatBuffer fb = BufferUtils.createFloatBuffer(16);
	static Matrix4f moveFrom;
	static Matrix4f moveTo;

	static {
		moveTo = new Matrix4f();
		moveTo.rotate(1f, new Vector3f(1, 0, 0));
		moveFrom = new Matrix4f();
	}

	private static String name(FloatBuffer buf) {
		StringBuilder sb = new StringBuilder("FB = [");
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				sb.append(buf.get(i * 4 + j));
				sb.append(" ");
			}
			sb.append("\n");
		}
		sb.append("]");
		return sb.toString();
	}

	static int count = 0;

	/** プレイヤーハンドラを参照してヒットマーク描画 */
	static void writeHitMarker(int x, int y) {
		/*
				count++;
				GL11.glPushMatrix();


				//GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, fb);
				System.out.println(GL11.glGetInteger(GL11.GL_MATRIX_MODE) + " " + fb);

				Gui.drawRect(50, 50, 100, 100, 0xAAAAAAAA);

				Gui.drawRect(100, 50, 150, 100, 0xAA0000FF);

				GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, fb);
				System.out.println(name(fb));

				float f = count % 100 / 100f;
				Matrix4f.add(Matrix4f.scale(new Vector3f(f, f, f), moveTo, null), Matrix4f.scale(new Vector3f(1 - f, 1 - f, 1 - f), moveFrom, null), null).store(fb);
				fb.position(0);
				GL11.glMultMatrix(fb);

				GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, fb);
				System.out.println(name(fb));

				Gui.drawRect(150, 50, 200, 100, 0xAAFF00AA);

				GL11.glPopMatrix();
		//*/
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
	public static void RenderEntityEvent(RenderLivingEvent<EntityLivingBase> e) {

	}

	public static void makeDot() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glColor3f(1, 1, 1);
		GL11.glPushMatrix();
		GL11.glPointSize(10F);
		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex3f(0F, 0F, 0F);
		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	static SelfProp prop = new SelfProp();

	/** 自分の持ってる銃の描画 アニメーションとパーツの稼働はこのメゾットのみ */
	public static void RenderHand(RenderHandEvent event) {//*
		ItemStack item = mc.player.getHeldItemMainhand();
		if (ItemGun.isGun(item)) {
			HideModel model = PackData.getModel(ItemGun.getGunData(item).get(ItemData.ModelName));
			if (model != null) {

				if (mc.gameSettings.thirdPersonView != 0 || (HideViewHandler.isADS && HideViewHandler.isScope))//TODO モデルにサイトを付けたバージョンに対応しなきゃ
					return;

				int side = 1;
				if (mc.gameSettings.mainHand == EnumHandSide.LEFT)
					side = -1;

				prop.setEntity(mc.player);
				float progress = prop != null ? prop.getAnimationProp(AnimationType.ADS, event.getPartialTicks()) : 0;

				Vector3f sight = model.model.getNodePos(model.get(HideModel.SightPos));
				Vector3f hand = model.model.getNodePos(model.get(HideModel.HandPos));

				//hand.scale(-1);
				Vector3f.sub(sight, hand, hand);
				//Vector3f.add(hand, sight, hand);

				//System.out.println(prop.getAnimationProp(AnimationType.ADS, event.getPartialTicks()) + " " + event.getPartialTicks());
				Vector3f vec = new Vector3f(1, -1.0f, 0.6f * side);

				//hand.scale(progress);
				//hand.scale(-1);
				vec.scale(1 - progress);
				//Vector3f.add(vec, hand, vec);

				GlStateManager.pushMatrix();
				GlStateManager.rotate(90, 0, 1, 0);
				//GlStateManager.translate(0.2, 0.0, 0.0);
				GlStateManager.translate(vec.x, vec.y, vec.z);
				//GlStateManager.rotate(-5 * side, 0, 1, 0);

				mc.entityRenderer.enableLightmap();
				AbstractClientPlayer abstractclientplayer = mc.player;
				int light = mc.world.getCombinedLight(new BlockPos(abstractclientplayer.posX, abstractclientplayer.posY + abstractclientplayer.getEyeHeight(), abstractclientplayer.posZ), 0);
				float f = light & 65535;
				float f1 = light >> 16;
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);

				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				//GlStateManager.enableRescaleNormal();
				GlStateManager.alphaFunc(516, 0.1F);
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

				model.render(true, prop, event.getPartialTicks());

				//GlStateManager.disableRescaleNormal();
				GlStateManager.disableBlend();
				RenderHelper.disableStandardItemLighting();
				mc.entityRenderer.disableLightmap();

				GlStateManager.popMatrix();
			}
			// ((ItemGun)item.getItem()).Model.render(RenderTick,Minecraft.getMinecraft().thePlayer);
		} //*/
	}

	public static void renderPoly() {

	}
}
