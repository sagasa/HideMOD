package handler;

import org.lwjgl.input.Mouse;
import net.minecraftforge.client.event.MouseEvent;

import item.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import newwork.PacketGuns;
import newwork.PacketHandler;

public class PlayerHandler {
	//変数はクライアント側のものだけ
	private static boolean rightMouseHeld;
	private static boolean leftMouseHeld;

	private static int ShootDelay = 0;

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

	/** サウンド処理 描画処理 入力処理 */
	private static void CientTick(EntityPlayer player) {
		// player.getCurrentEquippedItem()
		// player.getCurrentEquippedItem().getTagCompound().get;

		if (leftMouseHeld) {
			PacketHandler.INSTANCE.sendToServer(new PacketGuns(player.rotationYaw, player.rotationPitch));
		}

	}
	/***/
	private static void ServerTick(EntityPlayer player){

	}

	/**マウスイベント*/
	public static void MouseEvent(MouseEvent event){
		//左クリックなら
		if (event.button == 0){
			leftMouseHeld = event.buttonstate;
		}
	}

	/**接続時にサーバーサイドで呼ばれる*/
	public static void PlayerJoin(PlayerLoggedInEvent event){

	}
	/**切断時にサーバーサイドで呼ばれる*/
	public static void PlayerLeft(PlayerLoggedOutEvent event){

	}
}
