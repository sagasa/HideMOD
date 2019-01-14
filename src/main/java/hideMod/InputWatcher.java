package hideMod;

import java.util.EnumMap;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import handler.PacketHandler;
import handler.PlayerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketSync;

/**
 * 射撃系用の高速な入力キャプチャ 現状の機能 : PlayerHandlerのGunにアップデートをかける
 */
@SideOnly(Side.CLIENT)
public class InputWatcher extends Thread {
	private static final InputBind[] fastUpdate = new InputBind[] { InputBind.GUN_AIM, InputBind.GUN_FIRE };

	private EnumMap<InputBind, Boolean> lastInput = new EnumMap<>(InputBind.class);
	private EnumMap<InputBind, Boolean> nowInput = new EnumMap<>(InputBind.class);

	// ====== スタティック =====
	static private InputWatcher Watcher;

	public static void startWatcher() {
		if (Watcher != null) {
			Watcher.stop = true;
		}
		Watcher = new InputWatcher();
		Watcher.setName("InputWatcher");
		Watcher.setDaemon(true);
		Watcher.start();
	}

	public static void stopWatcher() {
		Watcher.stop = true;
	}

	// ===== 本体 =====

	/** trueになったらループを抜けて止める */
	private boolean stop = false;

	public InputWatcher() {
		for (InputBind inputBind : fastUpdate) {
			nowInput.put(inputBind, false);
			lastInput.put(inputBind, false);
		}
	}

	/** 最終アップデートの時点のSysTime */
	private static long lastTickMillis = 0;
	/** 最終アップデート時のTotalWorldTime */
	private static long lastTick = 0;

	/** Tickアップデート */
	public static void tickUpdate() {
		lastTickMillis = Minecraft.getSystemTime();
	}

	long lastTime = 0;
	int count = 0;

	@Override
	public void run() {
		// 初期値
		try {
			while (!stop) {
				long time = Minecraft.getSystemTime();
				lastInput.putAll(nowInput);
				for (InputBind input : fastUpdate) {
					nowInput.put(input, input.isButtonDown());
				}
				// 射撃処理
				PlayerHandler.clientGunUpdate(nowInput.get(InputBind.GUN_FIRE),
						(Minecraft.getSystemTime() - lastTickMillis) / 50f);

				time = Minecraft.getSystemTime() - time;
				time = Math.max(20 - time, 1);

				if (count == 10) {
					System.out.println((10000 / (Minecraft.getSystemTime() - lastTime)));
					lastTime = Minecraft.getSystemTime();
					count = 0;
				} else {
					count++;
				}

				Thread.sleep(time);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public enum InputBind {
		GUN_FIRE(false, 0), GUN_AIM(false, 1), GUN_RELOAD(true, Keyboard.KEY_R), GUN_FIREMODE(true,
				Keyboard.KEY_V), GUN_USEBULLET(true, Keyboard.KEY_B), DEBUG(true, Keyboard.KEY_G);

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

		public void setImputBind(boolean iskey, int sysid) {
			isKey = iskey;
			sysID = sysid;
		}
	}
}
