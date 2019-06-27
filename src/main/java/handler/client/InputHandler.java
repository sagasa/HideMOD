package handler.client;

import java.util.ArrayList;
import java.util.EnumMap;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import guns.GunController;
import handler.PacketHandler;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketInput;
import types.base.GunFireMode;

@SideOnly(Side.CLIENT)
public class InputHandler {

	private static float acceleration = 0F;
	private static float acceleration_changed_detector = 0F;
	private static float rotate = 0F;
	private static float rotate_changed_detector = 0F;

	private static EnumMap<InputBind, Boolean> oldKeys = new EnumMap<>(InputBind.class);
	private static EnumMap<InputBind, Boolean> newKeys = new EnumMap<>(InputBind.class);

	/** Tickアップデート */
	public static void tickUpdate() {
		InputWatcher.tickUpdate();
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if(player==null) {
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
		// 兵器に乗っているか
		if (!PlayerHandler.isOnEntityDrivable(player)) {
			// アイテムの銃の処理
			if (pushKeys.contains(InputBind.GUN_FIREMODE)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_MODE));
			} else if (pushKeys.contains(InputBind.GUN_RELOAD)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_RELOAD));
			} else if (pushKeys.contains(InputBind.GUN_USEBULLET)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_BULLET));
			}
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

	private static boolean dualToggle = false;
	private static boolean lastTrigger = false;

	public static void clientGunUpdate(float completion) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null)
			return;
		boolean trigger = player.isDead ? false : InputBind.GUN_FIRE.isButtonDown();
		ClientPlayerData data = HidePlayerData.getClientData(player);
		GunController gunMain = data.gunMain;
		GunController gunOff = data.gunOff;

		EquipMode em = EquipMode.getEquipMode(gunMain,gunOff);
		// 射撃処理
		if (em == EquipMode.Main) {
			gunMain.gunUpdate(trigger, completion);

		} else if (em == EquipMode.Off) {
			gunOff.gunUpdate(trigger, completion);

		} else if (em == EquipMode.OtherDual) {
			gunMain.gunUpdate(trigger, completion);
			gunOff.gunUpdate(trigger, completion);
		} else if (em == EquipMode.Dual) {

			boolean mainTrigger = false;
			boolean offTrigger = false;
			GunFireMode mode = GunFireMode.FULLAUTO;// TODO
			if (mode == GunFireMode.BURST || mode == GunFireMode.SEMIAUTO) {
				if (trigger != lastTrigger && trigger) {
					if ((dualToggle || !gunOff.canShoot()) && gunMain.canShoot()) {
						mainTrigger = true;
						dualToggle = false;
					} else if ((!dualToggle || !gunMain.canShoot()) && gunOff.canShoot()) {
						offTrigger = true;
						dualToggle = true;
					}
				}
			} else {
				mainTrigger = offTrigger = trigger;
			}
			gunMain.gunUpdate(mainTrigger, completion);
			gunOff.gunUpdate(offTrigger, completion);
		}
		lastTrigger = trigger;

		// 銃ののぞき込み処理
		String scope;

		/*
		 *
		 * if (scope != null) { boolean ads_res = false; int adsTick = 0; if
		 * (em.hasMain()) { adsTick += ItemGun.getGunData(main).ADS_TICK; } if
		 * (em.hasOff()) { adsTick += ItemGun.getGunData(off).ADS_TICK; } // クリックされているなら
		 * if (rightMouseHold) { if (adsState < adsTick) { adsState++; } else if
		 * (adsState > adsTick) { adsState = adsTick; } } else if (0 < adsState) {
		 * adsState--; } // ノータイムか if (adsTick <= 0) { ads_res = rightMouseHold; } else
		 * { ads_res = adsState == adsTick; } // 適応 if (ads_res) { if (!isADS) {
		 * setADS(scope, dia); } } else { if (isADS) { clearADS(); } } //
		 */
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
	}

	public static void stopWatcher() {
		Watcher.stop = true;
	}

	/** 入力を監視するデーモンスレッド */
	private static class InputWatcher extends Thread {
		private static final InputBind[] fastUpdate = new InputBind[] { InputBind.GUN_AIM, InputBind.GUN_FIRE };

		private EnumMap<InputBind, Boolean> lastInput = new EnumMap<>(InputBind.class);
		private EnumMap<InputBind, Boolean> nowInput = new EnumMap<>(InputBind.class);

		/** Tickアップデート */
		public static void tickUpdate() {
			InputWatcher.lastTickMillis = Minecraft.getSystemTime();
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
					clientGunUpdate((Minecraft.getSystemTime() - lastTickMillis) / 50f);

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
		GUN_FIRE(false, 0), GUN_AIM(false, 1), GUN_RELOAD(true, Keyboard.KEY_R), GUN_FIREMODE(true,
				Keyboard.KEY_V), GUN_USEBULLET(true, Keyboard.KEY_B), DEBUG(true, Keyboard.KEY_G), DRIVABLE_LEFT(true,
						Keyboard.KEY_A), DRIVABLE_RIGHT(true, Keyboard.KEY_D), DRIVABLE_FORWARD(true,
								Keyboard.KEY_W), DRIVABLE_BACK(true, Keyboard.KEY_S);

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
