package playerdata;

import handler.PlayerHandler.EquipMode;

public class HidePlayerData {
	public ModelState model = new ModelState();
	public Server input = new Server();

	public class ModelState {
		EquipMode equipMode;
	}

	public class Server {
		public boolean rightMouse = false;
		public boolean leftMouse = false;
		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFiremode = false;
		
		public int adsstate = 0;
		public boolean ads = false;
	}
}
