package hideMod;

import java.util.EnumMap;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**射撃系用の高速な入力キャプチャ*/
public class InputWatcher implements Runnable{
	private static final InputBind[] fastUpdate = new InputBind[] {InputBind.GUN_AIM,InputBind.GUN_FIRE};

	private EnumMap<InputBind, Boolean> lastInput = new EnumMap<>(InputBind.class);
	private EnumMap<InputBind, Boolean> nowInput = new EnumMap<>(InputBind.class);

	public InputWatcher() {
		for (InputBind inputBind : fastUpdate) {
			nowInput.put(inputBind, false);
			lastInput.put(inputBind, false);
		}
	}

	@Override
	public void run() {
		lastInput.putAll(nowInput);
		for (InputBind input : fastUpdate) {
			nowInput.put(input, input.isButtonDown());
		}
		//射撃処理
	}

	public enum InputBind {
		GUN_FIRE(false, 0),
		GUN_AIM(false, 1),
		GUN_RELOAD(true, Keyboard.KEY_R), GUN_FIREMODE(true, Keyboard.KEY_V), GUN_USEBULLET(true,
				Keyboard.KEY_B), DEBUG(true, Keyboard.KEY_G);

		private boolean isKey;
		private int sysID;

		InputBind(boolean iskey, int sysid) {
			isKey = iskey;
			sysID = sysid;
		}

		public String getBindName() {
			return this.toString();
		}

		public boolean isButtonDown() {
			if (isKey)
				return Keyboard.isKeyDown(sysID);
			else
				return Mouse.isButtonDown(sysID);
		}

		public void setImputBind(boolean iskey,int sysid) {
			isKey = iskey;
			sysID = sysid;
		}
	}
}
