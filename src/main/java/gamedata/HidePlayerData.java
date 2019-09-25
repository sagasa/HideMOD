package gamedata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import guns.GunController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * プレイヤーに紐付けされる変数s サーバーとクライアントを分けてある
 */
public class HidePlayerData {
	private static Map<UUID, ClientPlayerData> ClientDataMap = new HashMap<>();
	private static Map<UUID, ServerPlayerData> ServerDataMap = new HashMap<>();

	@SideOnly(Side.CLIENT)
	/** プレイヤーデータを取得 無かったらPut */
	public static ClientPlayerData getClientData(EntityPlayer player) {
		ClientPlayerData data = ClientDataMap.get(player.getUniqueID());
		if (data == null) {
			data = new ClientPlayerData();
			ClientDataMap.put(player.getUniqueID(), data);
		}
		return data;
	}

	/** プレイヤーデータを取得 無かったらPut */
	public static ServerPlayerData getServerData(EntityPlayer player) {
		ServerPlayerData data = ServerDataMap.get(player.getUniqueID());
		if (data == null) {
			data = new ServerPlayerData();
			ServerDataMap.put(player.getUniqueID(), data);
		}
		return data;
	}

	public static CommonPlayerData getData(EntityPlayer player, Side side) {
		if (side == Side.CLIENT)
			return getClientData(player);
		return getServerData(player);
	}

	public static class CommonPlayerData {
		/** MainとOffからID一致を返す */
		public GunController getGun(long id) {
			if (gunMain.idEquals(id)) {
				return gunMain;
			} else if (gunOff.idEquals(id)) {
				return gunOff;
			}
			return null;
		}

		public GunController gunMain = new GunController(EnumHand.MAIN_HAND);
		public GunController gunOff = new GunController(EnumHand.OFF_HAND);
	}

	public static class ServerPlayerData extends CommonPlayerData {
		public boolean leftMouse = false;
		public boolean rightMouse;
		/**サーバー側で処理*/
		public boolean reload = false;
		/**サーバー側で処理*/
		public boolean changeAmmo = false;
		/**サーバー側で処理*/
		public boolean changeFireMode = false;
		/**アニメーションのためのサーバー処理*/
		public boolean ads = false;

		public boolean dualToggle = true;

		public int adsstate = 0;
		public float adsRes = 0f;
	}

	public static class ClientPlayerData extends CommonPlayerData {
		public ClientPlayerData() {
			gunMain.setClientMode(true);
			gunOff.setClientMode(true);
		}
	}
}
