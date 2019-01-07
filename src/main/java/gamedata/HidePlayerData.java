package gamedata;

import handler.PlayerHandler.EquipMode;
import net.minecraft.util.EnumHand;

public class HidePlayerData {
	public CommonPlayerData Common = new CommonPlayerData();
	public ServerPlayerData Server = new ServerPlayerData();

	public class CommonPlayerData {
		EquipMode equipMode;
	}

	public class ServerPlayerData {
		public boolean leftMouse = false;
		public boolean rightMouse;

		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFireMode = false;

		public boolean dualToggle = true;

		public int reloadState = -1;
		public boolean reloadAll = false;

		public int adsState = 0;
		public float adsRes = 0f;

		public long idMain = 0;
		public long idOff = 0;

		public boolean leftInputDown;
		public boolean rightInputDown;
		public boolean forwardInputDown;
		public boolean backInputDown;
	}
}
