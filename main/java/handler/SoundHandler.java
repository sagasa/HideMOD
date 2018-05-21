package handler;

import java.awt.List;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import newwork.PacketPlaySound;

public class SoundHandler {
	public static final SoundHandler INSTANCE = new SoundHandler();
	/**1ブロック当たり何tickかかるか*/
	private static final float SOUND_SPEAD = 0.2f;
	
	/**再生リクエストを送信 サーバーサイドで呼んでください*/
	public static void broadcastSound(World w,String soundName, double x, double y, double z, float range, float vol,
			float pitch, boolean useSoundDelay, boolean useDecay) {
		//同じワールドのプレイヤーの距離を計算してパケットを送信
		for(EntityPlayer player:(ArrayList<EntityPlayer>)w.playerEntities){
			double distance = new Vec3(x, y, z).distanceTo(new Vec3(player.posX, player.posY, player.posZ));
			if(distance < range){
				if(useDecay){
					vol = vol*(float)(1-(distance/range));
				}
				int Delay = 0;
				if(useSoundDelay){
					Delay = (int) (distance*SOUND_SPEAD);
				}
				//TODO そのうちドップラー効果でも
				//パケット
				PacketHandler.INSTANCE.sendTo(new PacketPlaySound(soundName, x, y, z, vol, pitch, Delay), (EntityPlayerMP) player);
			}
		}
	}

	/** クライアントサイドのタスクリストに追加 */
	public static void addSoundTask(String soundName, double x, double y, double z, float vol, float pitch,
			int delay) {
		SoundTask.add(INSTANCE.new SoundTask(soundName, x, y, z, vol, pitch, delay));
	}

	/** クライアントサイドでの再生リスト */
	private static ArrayList<SoundTask> SoundTask = new ArrayList<SoundTask>();
	/**再生リストを更新して再生*/
	public static void ClientUpdate() {
		for(SoundTask task:SoundTask){
			//再生
			if(task.Delay == 0){
				World worldObj = Minecraft.getMinecraft().theWorld;
				EntityPlayer player = Minecraft.getMinecraft().thePlayer;
				//距離を調整する
				
				worldObj.playSound(player.posX+task.X, player.posY+task.Y, player.posZ+task.Z, task.Name, task.Vol, task.Pitch, false);
				SoundTask.remove(task);
			}
			//アップデート
			task.update();
		}
	}

	/** タスクリスト用 */
	class SoundTask {
		String Name;
		double X;
		double Y;
		double Z;
		float Vol;
		float Pitch;
		int Delay;

		public SoundTask(String soundName, double x, double y, double z, float vol, float pitch, int delay) {
			Name = soundName;
			X = x;
			Y = y;
			Z = z;
			Vol = vol;
			Pitch = pitch;
			Delay = delay;
		}
		/**距離を調整する*/
		
		void update() {
			Delay--;
		}
	}
}
