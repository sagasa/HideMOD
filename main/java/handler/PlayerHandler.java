package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import hideMod.loadPack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
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
import types.GunData;
import types.GunData.GunDataList;
import types.GunFireMode;

public class PlayerHandler {
	//変数はクライアント側のものだけ
	private static boolean rightMouseHeld;
	private static boolean leftMouseHeld;

	private static int fireNum = 0;
	private static boolean shooted = false;

	private static int ShootDelay = 0;

	private static HashMap<String,Boolean> oldKeys = new HashMap<String,Boolean>();
	private static HashMap<String,Boolean> newKeys = new HashMap<String,Boolean>();

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
		//持っているアイテムがHideModの銃なら
		ItemStack item = player.getCurrentEquippedItem();
		if (item != null&&item.hasTagCompound()&&item.getTagCompound().hasKey("isHideGun")){
			//gunData取得
			GunData data = loadPack.gunMap.get(item.getUnlocalizedName());
			if (leftMouseHeld) {
				//射撃処理
				if(ShootDelay <= 0){
					switch (GunFireMode.getFireMode(data.getDataString(GunDataList.FIRE_MODE))) {
					case BURST:

						break;
					case FULLAUTO:
						PacketHandler.INSTANCE.sendToServer(new PacketGuns(player.rotationYaw, player.rotationPitch));
						ShootDelay = data.getDataInt(GunDataList.RATE);
						break;
					case MINIGUN:
						break;
					case SEMIAUTO:
						//既に撃った後でなければ
						if (!shooted){
							PacketHandler.INSTANCE.sendToServer(new PacketGuns(player.rotationYaw, player.rotationPitch));
							ShootDelay = data.getDataInt(GunDataList.RATE);
							shooted = true;
						}
						break;
					}
				}
			}else{
				shooted = false;
			}

			//	String msg = player.getCurrentEquippedItem().getTagCompound().toString();
			//	player.addChatMessage(new ChatComponentText(msg));
		}
		if(ShootDelay > 0){
			ShootDelay--;
		}

		//キー入力の取得 押された変化を取得
		ArrayList<KeyBind> pushKeys = new ArrayList<KeyBind>();
		oldKeys.putAll(newKeys);
		for(KeyBind bind: KeyBind.values()){
			newKeys.put(bind.getBindName(), bind.getKeyDown());
			if (newKeys.get(bind.getBindName())&&!oldKeys.get(bind.getBindName())){
				pushKeys.add(bind);
			}
		}
		//各機能へのキーインプット入力
		//銃のモード切替
		if (pushKeys.contains(KeyBind.ChangeGunMode)){
			System.out.println("切り替え");
			WorldRenderer render =Tessellator.getInstance().getWorldRenderer();
			int width = 300;
		    int height = 200;
		    int depth = 100;
			GL11.glBegin(GL11.GL_QUADS);

	        //  OpenGL では頂点が左回りになっているのがポリゴンの表となる
	        //  今は表のみ表示する設定にしているので、頂点の方向を反対にすると裏側となり、表示されなくなる

			GL11.glColor3f(1.0f, 0.5f, 0.5f);            //  次に指定する座標に RGB で色を設定する
			GL11.glVertex3f(10, 100+10, 0);  //  1 つめの座標を指定する

			GL11.glColor3f(0.5f, 1.0f, 0.5f);
			GL11.glVertex3f(10, 100-10, 0);      // 2 つめの座標を指定する

	        GL11.glColor3f(0.5f, 0.5f, 1.0f);
	        GL11.glVertex3f(-10, 100-10, 0);                //    3 つめの座標を指定する

	        GL11.glColor3f(1.0f, 1.0f, 1.0f);
	        GL11.glVertex3f(- 10, 100+10, 0);        //    4 つめの座標を指定する

	        GL11.glEnd();

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
	/**クライアントサイドでのみ動作*/
	enum KeyBind{
		ChangeGunMode(Keyboard.KEY_F);

		HashMap<String,Integer> keyConfig = new HashMap<String,Integer>();

		KeyBind(int defaultKeyBind){
			keyConfig.put(this.toString(), defaultKeyBind);
		}
		public String getBindName(){
			return this.toString();
		}
		public boolean getKeyDown(){
			return Keyboard.isKeyDown(keyConfig.get(this.toString()));
		}
		public void setKeyBind(int keyCord){
			keyConfig.put(this.toString(), keyCord);
		}
	}
}
