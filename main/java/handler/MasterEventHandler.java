package handler;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

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
}
