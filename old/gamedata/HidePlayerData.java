package gamedata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import guns.Gun;
import net.minecraft.entity.player.EntityPlayer;
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
		else
			return getServerData(player);
	}

	public static class CommonPlayerData {
		/** MainとOffからID一致を返す */
		public Gun getGun(long id) {
			if (gunMain.idEquals(id)) {
				return gunMain;
			} else if (gunOff.idEquals(id)) {
				return gunOff;
			}
			return null;
		}

		public Gun gunMain = new Gun();
		public Gun gunOff = new Gun();
	}

	public static class ServerPlayerData extends CommonPlayerData {
		public boolean leftMouse = false;
		public boolean rightMouse;

		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFireMode = false;

		public boolean dualToggle = true;

		public int reloadState = -1;
		public boolean reloadAll = false;

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
