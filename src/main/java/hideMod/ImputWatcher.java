package hideMod;

import org.lwjgl.input.Mouse;

public class ImputWatcher implements Runnable{
	@Override
	public void run() {
		Mouse.isButtonDown(0);
	}
}
