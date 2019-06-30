package handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import network.PacketPlaySound;
import types.effect.Sound;

public class SoundHandler {
	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用
	 * @param exept */
	public static void broadcastSound(Entity e, double x, double y, double z, Sound sound, boolean excepting) {
		broadcastSound(e, x, y, z, sound, excepting, null);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用
	 * @param exept */
	public static void broadcastSound(Entity e, double x, double y, double z, Sound sound, boolean excepting,
			Byte cate) {
		broadcastSound(e.world, e.getEntityId(), sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH,
				sound.USE_DELAY,
				sound.USE_DECAY, excepting, cate);
	}

	/** 再生リクエストを送信 サーバーサイドで呼んでください 射撃音など遠距離まで聞こえる必要がある音に使用 */
	public static void broadcastSound(World world, int entityID, String soundName, double x, double y, double z,
			float range, float vol,
			float pitch, boolean useDelay, boolean useDecay, boolean excepting, Byte cate) {
		Entity e = world.getEntityByID(entityID);
		// 同じワールドのプレイヤーの距離を計算してパケットを送信
		for (EntityPlayer player : world.playerEntities) {
			double distance = new Vec3d(e.posX, e.posY, e.posZ)
					.distanceTo(new Vec3d(player.posX, player.posY, player.posZ));
			if ((!excepting || player.getEntityId() != entityID) && distance < range) {
				// パケット
				PacketHandler.INSTANCE.sendTo(
						new PacketPlaySound(entityID, cate, soundName, x, y, z, vol, pitch, range, useDelay, useDecay),
						(EntityPlayerMP) player);
			}
		}
	}

	/**カテゴリが設定された音のキャンセルリクエスト*/
	public static void bloadcastCancel(World world, int entity, byte cate) {
		for (EntityPlayer player : world.playerEntities)
			PacketHandler.INSTANCE.sendTo(new PacketPlaySound(entity, cate), (EntityPlayerMP) player);
	}
}
