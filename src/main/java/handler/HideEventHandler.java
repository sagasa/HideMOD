package handler;

import gamedata.HidePlayerData;
import handler.client.HideItemRender;
import handler.client.HideScope;
import handler.client.HideSoundManager;
import handler.client.InputHandler;
import handler.client.RecoilHandler;
import handler.client.RenderHandler;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.StopTracking;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
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

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerModel(ModelBakeEvent e) {
		HideItemRender.register(e);
	}

	/** 型に応じてデータマネージャーを添付する */
	@SubscribeEvent
	public void onEvent(EntityEvent.EntityConstructing event) {
		HideEntityDataManager.onEntityInit(event);
	}

	/* プレイヤーイベント */
	@SubscribeEvent
	public void onEvent(PlayerTickEvent event) {
		PlayerHandler.PlayerTick(event);
	}

	// ========接続イベント=========
	@SubscribeEvent
	public void onEvent(PlayerLoggedInEvent event) {
		//	PackSync.syncPack();
	}

	@SubscribeEvent
	public void onEvent(PlayerLoggedOutEvent event) {
		//切断時に銃の制御系を初期化
		HidePlayerData.getServerData(event.player).gunMain.saveAndClear();
		HidePlayerData.getServerData(event.player).gunOff.saveAndClear();
	}

	/** クライアント側でのワールド読み込み時に入力監視スレッドを立ち上げる */
	@SubscribeEvent
	public void onEvent(ClientConnectedToServerEvent event) {
		InputHandler.startWatcher();
	}

	/** 入力監視スレッドを停止する */
	@SubscribeEvent
	public void onEvent(ClientDisconnectionFromServerEvent event) {
		InputHandler.stopWatcher();
		//切断時に銃の制御系を初期化
		HidePlayerData.getClientData(Minecraft.getMinecraft().player).gunMain.saveAndClear();
		HidePlayerData.getClientData(Minecraft.getMinecraft().player).gunOff.saveAndClear();
	}

	@SubscribeEvent
	public void onEvent(StartTracking event) {
		// System.out.println(event.target+"Start");
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(TickEvent.ClientTickEvent event) {
		if (event.phase == Phase.START) {
			InputHandler.tickUpdate();
			HideSoundManager.update();
		}
	}

	@SubscribeEvent
	public void onEvent(StopTracking event) {
		// System.out.println(event.target+"Stop");
	}

	// 銃で破壊できないように
	@SubscribeEvent
	public void onEvent(BreakEvent event) {
		if (event.isCancelable() && ItemGun.isGun(event.getPlayer().inventory.getCurrentItem())) {
			event.setCanceled(true);
		}
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
	/*
	 * Fog削除できるぞ
	 *
	 * @SubscribeEvent
	 *
	 * @SideOnly(Side.CLIENT) public void onEvent(FogDensity event) {
	 * System.out.println(event.density); event.setCanceled(true); }
	 */
}
