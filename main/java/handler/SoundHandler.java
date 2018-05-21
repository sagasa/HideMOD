package handler;

import java.awt.List;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundList;
import net.minecraft.client.audio.SoundListSerializer;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import newwork.PacketPlaySound;

/**サーバーからクライアントへサウンドを流すハンドラ*/
public class SoundHandler {
	public static final SoundHandler INSTANCE = new SoundHandler();
	/** 1ブロック当たり何tickかかるか */
	private static final float SOUND_SPEAD = 0.2f;

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(String soundName, Entity e, float range, float vol, float pitch,
			boolean useSoundDelay, boolean useDecay) {
		broadcastSound(e.worldObj, soundName, e.posX, e.posY, e.posZ, range, vol, pitch, useSoundDelay, useDecay, e.getEntityId());
	}
	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	private static void broadcastSound(World w, String soundName, double x, double y, double z, float range, float vol,
			float pitch, boolean useSoundDelay, boolean useDecay) {
		broadcastSound(w, soundName, x, y, z, range, vol, pitch, useSoundDelay, useDecay, -1);
	}
	
	private static void broadcastSound(World w, String soundName, double x, double y, double z, float range, float vol,
			float pitch, boolean useSoundDelay, boolean useDecay, int entityID) {
		// 同じワールドのプレイヤーの距離を計算してパケットを送信
		for (EntityPlayer player : (ArrayList<EntityPlayer>) w.playerEntities) {
			double distance = new Vec3(x, y, z).distanceTo(new Vec3(player.posX, player.posY, player.posZ));
			if (distance < range) {
				if (useDecay) {
					vol = vol * (float) (1 - (distance / range));
				}
				int Delay = 0;
				if (useSoundDelay) {
					Delay = (int) (distance * SOUND_SPEAD);
				}
				// TODO そのうちドップラー効果でも
				// パケット
				PacketHandler.INSTANCE.sendTo(new PacketPlaySound(soundName, x, y, z, vol, pitch, Delay,entityID),
						(EntityPlayerMP) player);
			}
		}
		//Minecraft.getMinecraft().getSoundHandler().
		WorldClient
	}

	/** クライアントサイドのタスクリストに追加 */
	public static void addSoundTask(String soundName, double x, double y, double z, float vol, float pitch, int delay,int entityID) {
		SoundTask.add(INSTANCE.new SoundTask(soundName, x, y, z, vol, pitch, delay,entityID));
	}

	/** クライアントサイドでの再生リスト */
	private static ArrayList<SoundTask> SoundTask = new ArrayList<SoundTask>();

	/** 再生リストを更新して再生 */
	public static void ClientUpdate() {
		for (SoundTask task : SoundTask) {
			// 再生
			if (task.Delay == 0) {
				World worldObj = Minecraft.getMinecraft().theWorld;
				//Entityが取得できたなら
				if(worldObj.getEntityByID(task.EntityID)!=null){
					worldObj.playSoundAtEntity(worldObj.getEntityByID(task.EntityID), task.Name, task.Vol, task.Pitch);
				}else{
					EntityPlayer player = Minecraft.getMinecraft().thePlayer;
					// 距離を調整する
					task.Normalize(player.posX, player.posY, player.posZ);
					worldObj.playSound(task.X, task.Y, task.Z, task.Name, task.Vol, task.Pitch, false);
				}
				SoundTask.remove(task);
			}
			// アップデート
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
		int EntityID;

		public SoundTask(String soundName, double x, double y, double z, float vol, float pitch, int delay,int entityID) {
			Name = soundName;
			X = x;
			Y = y;
			Z = z;
			Vol = vol;
			Pitch = pitch;
			Delay = delay;
			EntityID = entityID;
		}

		/** 距離を調整する */
		void Normalize(double x, double y, double z) {
			Vec3 nomalVec = new Vec3(X - x, Y - y, Z - z).normalize();
			X = nomalVec.xCoord + x;
			Y = nomalVec.yCoord + y;
			Z = nomalVec.zCoord + z;
		}

		void update() {
			Delay--;
		}
	}
}
