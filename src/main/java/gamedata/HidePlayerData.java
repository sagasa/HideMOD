package gamedata;

import handler.PlayerHandler.EquipMode;

public class HidePlayerData {
	public CommonPlayerData Common = new CommonPlayerData();
	public ServerPlayerData Server = new ServerPlayerData();

	public class CommonPlayerData {
		EquipMode equipMode;
	}

	public Gun gunMain = null;
	public Gun gunOff = null;

	public class ServerPlayerData {
		public boolean leftMouse = false;
		public boolean rightMouse;

		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFireMode = false;

		public boolean dualToggle = true;

		public int reloadstate = -1;
		public boolean reloadall = false;

		public int adsstate = 0;
		public float adsRes = 0f;
		public boolean reloadAll;
		public int reloadState;


	}
}
