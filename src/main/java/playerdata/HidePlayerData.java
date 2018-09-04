package playerdata;

import handler.PlayerHandler.EquipMode;

public class HidePlayerData {
	public CommonPlayerData Common = new CommonPlayerData();
	public ServerPlayerData Server = new ServerPlayerData();

	public class CommonPlayerData {
		EquipMode equipMode;
	}

	public class ServerPlayerData {
		public boolean rightMouse = false;
		public boolean leftMouse = false;
		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFiremode = false;

		public long idMain;
		public long idOff;

		public int reloadstate = -1;

		public boolean stopshoot = false;

		public int adsstate = 0;
		public boolean ads = false;
	}
}
