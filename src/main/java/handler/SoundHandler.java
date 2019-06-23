package handler;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
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
							new HideEntitySound(soundName, finalVol, pitch, (float) x, (float) y, (float) z),
							finalDelay);
				}
			});

		}
	}

	//TODO サウンドカテゴリ追加したい…にゃあ
	public static class HideEntitySound extends PositionedSound implements ITickableSound {
		/**トラッキング対象 消えたら再生を終了*/
		final protected Entity entity;

		final protected Vec3d moveVec;

		protected boolean donePlaying = false;

		/** エンティティに追従する音
		 * @param e 追従先*/
		protected HideEntitySound(Sound sound, Entity e, SoundCategory categoryIn) {
			this(sound, e, Vec3d.ZERO, categoryIn);
		}

		/** エンティティに追従する音
		 * @param e 追従先
		 * @param move エンティティと音源の位置関係*/
		protected HideEntitySound(Sound sound, Entity e, Vec3d move, SoundCategory categoryIn) {
			super(new ResourceLocation(sound.NAME), categoryIn);
			entity = e;
			moveVec = move;
		}

		public HideEntitySound(String soundName, float finalVol, float pitch, float x, float y, float z) {
			super(new ResourceLocation(soundName), SoundCategory.PLAYERS);
			moveVec = Vec3d.ZERO;
			entity = null;
			xPosF = x;
			yPosF = y;
			zPosF = z;
			volume = finalVol;
			this.pitch = pitch;
		}

		@Override
		public void update() {
			if (entity == null)
				return;

			if (entity.isDead) {
				donePlaying = true;
				return;
			}
			//位置更新
			Vec3d location = entity.getPositionVector();
			if (moveVec != Vec3d.ZERO)
				location = location.add(moveVec.rotatePitch(entity.rotationPitch).rotateYaw(entity.rotationYaw));
			xPosF = (float) location.x;
			yPosF = (float) location.y;
			zPosF = (float) location.z;
		}

		@Override
		public boolean isDonePlaying() {
			return donePlaying;
		}
	}

	public static void broadcastSound(double x, double y, double z, Sound sound) {
		// TODO 自動生成されたメソッド・スタブ

	}
}
