package handler.client;

import java.util.ArrayList;
import java.util.EnumMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import handler.PacketHandler;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketInput;

@SideOnly(Side.CLIENT)
public class InputHandler {

	private static final Logger log = LogManager.getLogger();

	public static final KeyBinding AIM = new KeyBinding("Key.aim", -99, "HideMod");
	public static final KeyBinding FIRE = new KeyBinding("Key.fire", -100, "HideMod");
	public static final KeyBinding RELOAD = new KeyBinding("Key.reload", Keyboard.KEY_R, "HideMod");
	public static final KeyBinding CHANGE_BULLET = new KeyBinding("Key.bullet", Keyboard.KEY_B, "HideMod");
	public static final KeyBinding CHANGE_FIREMODE = new KeyBinding("Key.firemode", Keyboard.KEY_V, "HideMod");
	public static final KeyBinding DEBUG = new KeyBinding("Key.debug", Keyboard.KEY_G, "HideMod");

	private static boolean isStart = false;

	private static float acceleration = 0F;
	private static float acceleration_changed_detector = 0F;
	private static float rotate = 0F;
	private static float rotate_changed_detector = 0F;

	private static EnumMap<InputBind, Boolean> oldKeys = new EnumMap<>(InputBind.class);
	private static EnumMap<InputBind, Boolean> newKeys = new EnumMap<>(InputBind.class);

	public static void init() {
		ClientRegistry.registerKeyBinding(AIM);
		ClientRegistry.registerKeyBinding(FIRE);
		ClientRegistry.registerKeyBinding(RELOAD);
		ClientRegistry.registerKeyBinding(CHANGE_BULLET);
		ClientRegistry.registerKeyBinding(CHANGE_FIREMODE);
		ClientRegistry.registerKeyBinding(DEBUG);
	}

	/** Tickアップデート */
	public static void tickUpdate() {
		InputWatcher.tickUpdate();
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null || !isStart) {
			return;
		}
		// キー入力の取得 押された変化を取得
		ArrayList<InputBind> pushKeys = new ArrayList<>();
		oldKeys.putAll(newKeys);
		for (InputBind bind : InputBind.values()) {
			newKeys.put(bind, bind.isButtonDown());
			if (newKeys.get(bind) && !oldKeys.get(bind)) {
				pushKeys.add(bind);
			}
		}
		//銃火器への操作
		ClientPlayerData data = HidePlayerData.getClientData(player);
		EquipMode em = data.gunManager.CurrentEquipMode;
		if (em != EquipMode.None) {
			//adsにかかる時間 0でADS不能
			int adsTick = 1;
			if (em.hasMain()) {
				adsTick += data.gunManager.getGunMain().getGunData().ADS_TICK;
			}
			if (em.hasOff()) {
				adsTick += data.gunManager.getGunOff().getGunData().ADS_TICK;
			}
			if (adsTick < 0)
				adsTick = 0;
			//ADS計算
			else if (adsTick < data.adsstate)
				data.adsstate = adsTick;
			if (InputHandler.AIM.isKeyDown()) {
				if (data.adsstate < adsTick)
					data.adsstate++;
			} else {
				if (0 < data.adsstate)
					data.adsstate--;
			}
			if (CHANGE_FIREMODE.isPressed()) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_MODE));
			}
			if (RELOAD.isPressed()) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_RELOAD));
			}
			if (CHANGE_BULLET.isPressed()) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_BULLET));
			}
			float oldADS = data.adsRes;
			data.adsRes = data.adsstate == 0 ? 0 : data.adsstate / (float) adsTick;
			if (oldADS != data.adsRes) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(data.adsRes));
			}

		}

		// 兵器に乗っているか
		if (!PlayerHandler.isOnEntityDrivable(player)) {
			// アイテムの銃の処理

		} else {
			// Drivable用入力操作
			if (player.movementInput.forwardKeyDown == player.movementInput.backKeyDown) {
				acceleration = 0F;
				// PacketHandler.INSTANCE.sendToServer(new
				// PacketInput(PacketInput.DRIVABLE_LEFT));
			} else if (player.movementInput.forwardKeyDown) {
				acceleration = 1F;
			} else if (player.movementInput.backKeyDown) {
				acceleration = -1F;
			}

			if (acceleration_changed_detector != acceleration) {
				//TODO	PacketHandler.INSTANCE.sendToServer(new PacketAcceleration(acceleration));
				if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
					EntityDrivable drivable = (EntityDrivable) player.getRidingEntity();
					drivable.setAcceleration(acceleration);
				}
				acceleration_changed_detector = acceleration;
			}

			if (player.movementInput.rightKeyDown == player.movementInput.leftKeyDown) {
				// PacketHandler.INSTANCE.sendToServer(new
				// PacketInput(PacketInput.DRIVABLE_RIGHT));
				rotate = 0F;
			} else if (player.movementInput.rightKeyDown) {
				rotate = 1F;
			} else if (player.movementInput.leftKeyDown) {
				rotate = -1F;
			}

			if (rotate_changed_detector != rotate) {
				//TODO	PacketHandler.INSTANCE.sendToServer(new PacketRotate(rotate));
				if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
					EntityDrivable drivable = (EntityDrivable) player.getRidingEntity();
					drivable.setRotate(rotate);
				}
			}

		}
	}

	static private InputWatcher Watcher;

	public static void startWatcher() {
		if (Watcher != null) {
			Watcher.stop = true;
		}
		Watcher = new InputWatcher();
		Watcher.setName("InputWatcher");
		Watcher.setDaemon(true);
		Watcher.start();
		Watcher.setUncaughtExceptionHandler((thread, throwable) -> {
			startWatcher();
			log.warn("InputThread was clash restarting", throwable);
		});
		isStart = true;
	}

	public static void stopWatcher() {
		isStart = false;
		Watcher.stop = true;
	}

	/** 入力を監視するデーモンスレッド */
	private static class InputWatcher extends Thread {
		/** Tickアップデート */
		public static void tickUpdate() {
			InputWatcher.lastTickMillis = Minecraft.getSystemTime();
		}

		// ===== 本体 =====

		/** trueになったらループを抜けて止める */
		private boolean stop = false;

		/** 最終アップデートの時点のSysTime */
		private static long lastTickMillis = 0;

		@Override
		public void run() {
			// 初期値
			try {
				while (!stop) {
					long time = Minecraft.getSystemTime();
					// 射撃処理
					boolean fire;
					if (0 <= FIRE.getKeyCode())
						fire = Keyboard.isKeyDown(FIRE.getKeyCode());
					else
						fire = Mouse.isButtonDown(FIRE.getKeyCode() + 100);
					EntityPlayerSP player = Minecraft.getMinecraft().player;
					if (player == null)
						continue;
					ClientPlayerData data = HidePlayerData.getClientData(player);
					data.gunManager.clientGunUpdate((Minecraft.getSystemTime() - lastTickMillis) / 50f, fire);

					time = Minecraft.getSystemTime() - time;
					time = Math.max(20 - time, 1);
					Thread.sleep(time);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	public enum InputBind {
		DRIVABLE_LEFT(true, Keyboard.KEY_A), DRIVABLE_RIGHT(true, Keyboard.KEY_D), DRIVABLE_FORWARD(true, Keyboard.KEY_W), DRIVABLE_BACK(true, Keyboard.KEY_S);

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
