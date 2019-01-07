package handler;

import hideMod.PackData;
import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.StopTracking;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HideEventHandler {
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

	/**型に応じてデータマネージャーを添付する*/
	@SubscribeEvent
	public void onEvent(EntityEvent.EntityConstructing event) {
		HideEntityDataManager.onEntityInit(event);
	}

	/* プレイヤーイベント */
	@SubscribeEvent
	public void onEvent(PlayerTickEvent event) {
		PlayerHandler.PlayerTick(event);
	}

	@SubscribeEvent
	public void onEvent(PlayerLoggedInEvent event) {
		System.out.println("LOGIN");
		PlayerHandler.PlayerJoin(event);
	}

	@SubscribeEvent
	public void onEvent(PlayerLoggedOutEvent event) {
		System.out.println("LOGOUT");
		PlayerHandler.PlayerLeft(event);
	}

	@SubscribeEvent
	public void onEvent(MouseEvent event) {
		PlayerHandler.MouseEvent(event);
	}

	@SubscribeEvent
	public void onEvent(StartTracking event) {
		// System.out.println(event.target+"Start");
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

	@SubscribeEvent
	public void onEvent(TickEvent.ClientTickEvent event) {

	}
	// ======レンダー関連========

	// partialTicks取得
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(TickEvent.RenderTickEvent event) {
		if (event.phase.equals(Phase.START)) {
			RenderHandler.setRenderTick(event.renderTickTime);
		}
	}

	// 手持ちアイテム描画
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderLivingEvent.Post event) {
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
		// System.out.println(event.type);
	}

	// 額縁描画
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onEvent(RenderItemInFrameEvent event) {
		// System.out.println(event.target+"Stop");
		System.out.println(event);
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
