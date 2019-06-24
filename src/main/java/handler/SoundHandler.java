package handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import gamedata.HideEntitySound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketPlaySound;
import types.effect.Sound;

/** サーバーからクライアントへサウンドを流すハンドラ */
public class SoundHandler {
	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(Entity e, double x, double y, double z, Sound sound) {
		broadcastSound(e.world, e.getEntityId(), sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH,
				sound.USE_DELAY,
				sound.USE_DECAY);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(World world, int entityID, String soundName, double x, double y, double z,
			float range, float vol,
			float pitch, boolean useDelay, boolean useDecay) {
		Entity e = world.getEntityByID(entityID);
		// 同じワールドのプレイヤーの距離を計算してパケットを送信
		for (EntityPlayer player : world.playerEntities) {
			double distance = new Vec3d(e.posX, e.posY, e.posZ)
					.distanceTo(new Vec3d(player.posX, player.posY, player.posZ));
			if (distance < range) {
				// パケット
				PacketHandler.INSTANCE.sendTo(
						new PacketPlaySound(entityID, soundName, x, y, z, vol, pitch, pitch, useDelay, useDecay),
						(EntityPlayerMP) player);
			}
		}
	}

	//============= クライアントサイドでの再生メゾット ==============

	@SideOnly(Side.CLIENT)
	public static HideEntitySound playSound(Entity entity, double x, double y, double z, Sound sound) {
		return playSound(entity, sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH, sound.USE_DELAY,
				sound.USE_DECAY);
	}

	@SideOnly(Side.CLIENT)
	public static HideEntitySound playSound(Entity entity, String soundName, double x, double y, double z, float range,
			float vol,
			float pitch,
			boolean useSoundDelay, boolean useDecay) {
		HideEntitySound sound = new HideEntitySound(entity, soundName, x, y, z, vol, pitch, range, useSoundDelay,
				useDecay,
				SoundCategory.PLAYERS);
		if (sound.getDistance() < range) {
			//同期
			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				public void run() {
					playSound(sound);
				}
			});
			return sound;
		} else
			return null;
	}

	private static Map<HideEntitySound, Integer> delayedSounds = new HashMap<>();
	private static int time;

	/**tickアップデート*/
	public static void update() {
		time++;
		Iterator<Entry<HideEntitySound, Integer>> itr = delayedSounds.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<HideEntitySound, Integer> entry1 = itr.next();
			if (time >= entry1.getValue()) {
				Minecraft.getMinecraft().getSoundHandler().playSound(entry1.getKey());
				itr.remove();
			}
		}
	}

	public static boolean isSoundPlaying(HideEntitySound sound) {
		return delayedSounds.containsKey(sound) ||
				Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound);
	}

	/**キャンセルに対応したディレイ付き再生*/
	public static void playSound(HideEntitySound sound) {
		int delay = sound.getDelay();
		if (delay > 0) {
			delayedSounds.put(sound, delay + time);
		} else {
			Minecraft.getMinecraft().getSoundHandler().playSound(sound);
		}
	}

	public static void stopSound(HideEntitySound sound) {
		if (delayedSounds.containsKey(sound))
			delayedSounds.remove(sound);
		else
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
	}
}