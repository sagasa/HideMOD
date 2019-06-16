package handler;

import java.util.ArrayList;

import hideMod.sound.HideSound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketPlaySound;
import types.effect.Sound;

/** サーバーからクライアントへサウンドを流すハンドラ */
public class SoundHandler {
	public static final SoundHandler INSTANCE = new SoundHandler();
	/** 1ブロック当たり何tickかかるか */
	private static final float SOUND_SPEAD = 0.2f;

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(String soundName, Entity e, float range, float vol, float pitch,
			boolean useSoundDelay, boolean useDecay) {
		broadcastSound(e.world, soundName, e.posX, e.posY, e.posZ, range, vol, pitch, useSoundDelay, useDecay);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(World w, double x, double y, double z, Sound sound) {
		broadcastSound(w, sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH, sound.USE_DELAY, sound.USE_DECAY);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(World w, String soundName, double x, double y, double z, float range, float vol,
			float pitch, boolean useSoundDelay, boolean useDecay) {
		// 同じワールドのプレイヤーの距離を計算してパケットを送信
		for (EntityPlayer player : (ArrayList<EntityPlayer>) w.playerEntities) {
			double distance = new Vec3d(x, y, z).distanceTo(new Vec3d(player.posX, player.posY, player.posZ));
			if (distance < range) {
				float playerVol = vol;
				if (useDecay) {
					playerVol = playerVol * (float) (1 - (distance / range));
				}
				int Delay = 0;
				if (useSoundDelay) {
					Delay = (int) (distance * SOUND_SPEAD);
				}
				// TODO そのうちドップラー効果でも
				// パケット
				PacketHandler.INSTANCE.sendTo(new PacketPlaySound(soundName, x, y, z, playerVol, pitch, Delay),
						(EntityPlayerMP) player);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public static void playSound(double x, double y, double z, Sound sound) {
		playSound(sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH, sound.USE_DELAY, sound.USE_DECAY);
	}

	@SideOnly(Side.CLIENT)
	public static void playSound(String soundName, double x, double y, double z, float range, float vol, float pitch,
			boolean useSoundDelay, boolean useDecay) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		double distance = new Vec3d(x, y, z).distanceTo(new Vec3d(player.posX, player.posY, player.posZ));
		if (distance < range) {
			float playerVol = vol;
			if (useDecay) {
				playerVol = playerVol * (float) (1 - (distance / range));
			}
			final float finalVol = playerVol;
			int Delay = 0;
			if (useSoundDelay) {
				Delay = (int) (distance * SOUND_SPEAD);
			}
			final int finalDelay = Delay;
			//同期
			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				public void run() {
					Minecraft.getMinecraft().getSoundHandler().playDelayedSound(
							new HideSound(soundName, finalVol, pitch, (float) x, (float) y, (float) z), finalDelay);
				}
			});

		}
	}
}
