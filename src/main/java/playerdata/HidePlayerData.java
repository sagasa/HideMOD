package playerdata;

import handler.PlayerHandler.EquipMode;
import net.minecraft.util.EnumHand;

public class HidePlayerData {
	public CommonPlayerData Common = new CommonPlayerData();
	public ServerPlayerData Server = new ServerPlayerData();

	public class CommonPlayerData {
		EquipMode equipMode;
	}

	public class ServerPlayerData {
		public boolean rightMouse = false;
		public boolean leftMouse = false;
		public boolean rightClick = false;
		public boolean leftClick = false;
		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFiremode = false;

		public GunState mainState = new GunState();
		public GunState offState = new GunState();
		public boolean dualToggle = true;

		public int reloadstate = -1;
		public boolean reloadall = false;

		public int adsstate = 0;
		public boolean ads = false;
		public long idMain = 0;
		public long idOff = 0;
	}
}
