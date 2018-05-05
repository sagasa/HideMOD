package handler;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerEvent.StopTracking;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**イベントハンドラ ここから各種ハンドラに投げる*/
public class MasterEventHandler {

	/*プレイヤーイベント*/
	@SubscribeEvent
	public void onEvent(PlayerTickEvent event)	{
		PlayerHandler.PlayerTick(event);
	}
	@SubscribeEvent
	public void onEvent(PlayerLoggedInEvent event)	{
		System.out.println("LOGIN");
		PlayerHandler.PlayerJoin(event);
	}
	@SubscribeEvent
	public void onEvent(PlayerLoggedOutEvent event)	{
		System.out.println("LOGOUT");
		PlayerHandler.PlayerLeft(event);
	}
	@SubscribeEvent
	public void onEvent(MouseEvent event)	{
		PlayerHandler.MouseEvent(event);
	}
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onEvent(RenderPlayerEvent.Pre event)	{
		RenderHandler.RenderPlayerEvent(event);
	}
	@SubscribeEvent
	public void onEvent(StartTracking event)	{
	//	System.out.println(event.target+"Start");
	}
	@SubscribeEvent
	public void onEvent(StopTracking event)	{
	//	System.out.println(event.target+"Stop");
	}
	@SubscribeEvent
	public void onEvent(RenderGameOverlayEvent event)	{
	//	System.out.println(event.target+"Stop");
		RenderHandler.RenderTest();
	}
}
