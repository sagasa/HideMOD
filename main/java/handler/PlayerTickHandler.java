package handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class PlayerTickHandler {
	/**プレイヤーのTicl処理*/
	public static void PlayerTick(PlayerTickEvent event){
		if(event.phase == Phase.START){
			//サイドで処理を分ける
			if (event.side==Side.CLIENT){
				CientTick(event.player);
			}else if(event.side==Side.SERVER){
				ServerTick(event.player);
			}
			
		}
	}
	/**サウンド処理 描画処理 入力処理                  
	 * @param player */
	private static void CientTick(EntityPlayer player){
		//player.getCurrentEquippedItem()
	}
	/***/
	private static void ServerTick(EntityPlayer player){
		
	}
}
