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
		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFiremode = false;

		public boolean getStopshoot(EnumHand hand) {
			if (hand == EnumHand.MAIN_HAND) {
				return stopshootMain;
			} else {
				return stopshootOff;
			}
		}
		
		public int reloadstate = -1;

		public int adsstate = 0;
		public boolean ads = false;
	}
}
