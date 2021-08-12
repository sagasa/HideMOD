package hide.common;

import handler.client.HideScope;
import handler.client.RenderHandler;
import hide.guns.data.HideEntityDataManager;
import hide.guns.gui.RecoilHandler;
import hide.model.gltf.GltfLoader;
import hide.ux.HideSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.StopTracking;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pack.PackData;

public class HideEventHandler {
	//============= レジスタ ================
	/* イニシャライズ */
	@SubscribeEvent
	public void onEvent(RegistryEvent.Register<Item> event) {
		// System.out.println("レジスター呼ばれた！！");
		PackData.registerItems(event);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(ModelRegistryEvent event) {
		PackData.registerModel();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerSound(RegistryEvent.Register<SoundEvent> event) {
		PackData.registerSound(event);
	}

	/** 型に応じてデータマネージャーを添付する */
	@SubscribeEvent
	public void onEvent(EntityEvent.EntityConstructing event) {
		HideEntityDataManager.onEntityInit(event);
	}

	// ========接続イベント=========
	@SubscribeEvent
	public void onEvent(PlayerLoggedInEvent event) {
		//	PackSync.syncPack();
	}

	@SubscribeEvent
	public void onEvent(LivingJumpEvent event) {

		if (event.getEntityLiving() instanceof EntityPlayer) {
			System.out.println("YEEEEEEEEEEEEE");
			System.out.println(event.getEntityLiving().motionY);
			//event.getEntityLiving().capabilities.setPlayerWalkSpeed(3f);
		}
		//	PackSync.syncPack();
	}

	//=== クライアント ===

	@SubscribeEvent
	public void onEvent(StartTracking event) {
		// System.out.println(event.target+"Start");
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(TickEvent.ClientTickEvent event) {
		if (event.phase == Phase.START) {
			HideSoundManager.update();
		}
	}

	@SubscribeEvent
	public void onEvent(StopTracking event) {
		// System.out.println(event.target+"Stop");
	}

	// ======アップデート=========
	@SubscribeEvent
	public void onEvent(TickEvent.ServerTickEvent event) {

	}
	// ======レンダー関連========

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderWorldLastEvent event) {
		HideScope.updateImage();
	}

	// partialTicks取得
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(TickEvent.RenderTickEvent event) {
		if (event.phase.equals(Phase.START)) {
			RenderHandler.setRenderTick(event.renderTickTime);
			RecoilHandler.updateRecoil(event.renderTickTime);
		}
	}

	// 手持ちアイテム描画
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderLivingEvent.Post<EntityLivingBase> event) {
		RenderHandler.RenderEntityEvent(event);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderHandEvent event) {
		RenderHandler.RenderHand(event);
	}

	// オーバーレイGUI表示
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderGameOverlayEvent event) {
		// System.out.println(event.target+"Stop");
		RenderHandler.writeGameOverlay(event);
	}

	// 額縁描画
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderItemInFrameEvent event) {
		// System.out.println(event.target+"Stop");
		//System.out.println(event);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onRenderTest(RenderWorldLastEvent event) {
		EntityPlayer p = Minecraft.getMinecraft().player;
		GlStateManager.translate(-p.posX, -p.posY, -p.posZ);
		GltfLoader.render();
		// System.out.println(event.target+"Stop");
		//System.out.println(event);
	}
	/*
	 * Fog削除できるぞ
	 *
	 * @SubscribeEvent
	 *
	 * @SideOnly(Side.CLIENT) public void onEvent(FogDensity event) {
	 * System.out.println(event.density); event.setCanceled(true); }
	 */
}
