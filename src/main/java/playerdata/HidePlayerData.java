package playerdata;

import handler.PlayerHandler.EquipMode;

public class HidePlayerData {
	public ModelState model = new ModelState();
	public Input input = new Input();

	public class ModelState {
		EquipMode equipMode;
	}

	public class Input {
		public boolean rightMouse = false;
		public boolean leftMouse = false;
		public boolean reload = false;
		public boolean changeAmmo = false;
		public boolean changeFiremode = false;
	}
}
