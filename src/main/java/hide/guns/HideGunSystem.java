package hide.guns;

import handler.client.InputHandler;
import hide.core.HidePlayerDataManager;
import hide.core.HideSubSystem.IHideSubSystem;
import hide.guns.PlayerData.ClientPlayerData;
import hide.guns.PlayerData.ServerPlayerData;
import hide.guns.network.PacketHit;
import hide.guns.network.PacketShoot;
import hide.guns.network.PacketSyncMag;
import hidemod.HideMod;
import items.ItemGun;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HideGunSystem implements IHideSubSystem {

	@Override
	public void init(Side side) {
		HideMod.registerNetMsg(PacketSyncMag.class, PacketSyncMag.class, Side.CLIENT);
		HideMod.registerNetMsg(PacketShoot.class, PacketShoot.class, Side.SERVER);
		HideMod.registerNetMsg(PacketHit.class, PacketHit.class, Side.CLIENT);

		HidePlayerDataManager.register(ServerPlayerData.class, Side.SERVER);
		if (side == Side.CLIENT)
			HidePlayerDataManager.register(ClientPlayerData.class, Side.CLIENT);
	}



	/* プレイヤーイベント */
	@SubscribeEvent
	public void onEvent(PlayerTickEvent event) {
		PlayerData.PlayerTick(event);
	}

	/** クライアント側でのワールド読み込み時に入力監視スレッドを立ち上げる */
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(ClientConnectedToServerEvent event) {
		InputHandler.startWatcher();
	}

	/** 入力監視スレッドを停止する */
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(ClientDisconnectionFromServerEvent event) {
		InputHandler.stopWatcher();
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(TickEvent.ClientTickEvent event) {
		if (event.phase == Phase.START) {
			InputHandler.tickUpdate();
		}
	}

	// 銃で破壊できないように
	@SubscribeEvent
	public void onEvent(BreakEvent event) {
		if (event.isCancelable() && ItemGun.isGun(event.getPlayer().inventory.getCurrentItem())) {
			event.setCanceled(true);
		}
	}

	//- インタラクト --
	@SubscribeEvent()
	public void rightClick(RightClickBlock event) {
		//ブロックインタラクト
		if (ItemGun.isGun(event.getEntityPlayer().inventory.getCurrentItem()) && !event.getEntityPlayer().isSneaking()) {
			Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
			// コンテナ
			if (block instanceof BlockContainer) {
				event.setCanceled(true);
			}
		}
	}
}
