package handler.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import gamedata.HideEntitySound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.effect.Sound;

@SideOnly(Side.CLIENT)
public class HideSoundManager {
	public enum HideSoundType {
		GUN_RELOAD, GUN_SHOOT;
	}

	private final Entity entity;
	private final double X, Y, Z;

	private Map<HideSoundType, HideEntitySound> sounds = new HashMap<>();

	public HideSoundManager(Entity e, double x, double y, double z) {
		entity = e;
		X = x;
		Y = y;
		Z = z;
	}

	private void updateSound() {
		Iterator<Entry<HideSoundType, HideEntitySound>> itr = sounds.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<HideSoundType, HideEntitySound> entry = itr.next();
			if (!isSoundPlaying(entry.getValue()))
				itr.remove();
		}
	}

	public void playSound(Sound sound, HideSoundType type) {
		updateSound();
		stopSound(type);
		sounds.put(type, playSound(entity, X, Y, Z, sound));
	}

	public void stopSound(HideSoundType type) {
		if (sounds.containsKey(type)) {
			stopSound(sounds.get(type));
			sounds.remove(type);
		}
	}

	//============= クライアントサイドでの再生メゾット ==============

	public static HideEntitySound playSound(Entity entity, double x, double y, double z, Sound sound) {
		return playSound(entity, sound.NAME, x, y, z, sound.RANGE, sound.VOL, sound.PITCH, sound.USE_DELAY,
				sound.USE_DECAY);
	}

	public static HideEntitySound playSound(Entity entity, String soundName, double x, double y, double z, float range,
			float vol,
			float pitch,
			boolean useSoundDelay, boolean useDecay) {
		HideEntitySound sound = new HideEntitySound(entity, soundName, x, y, z, vol, pitch, range, useSoundDelay,
				useDecay,
				SoundCategory.PLAYERS);
		if (sound.getDistance() < range) {
			//同期
			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				public void run() {
					playSound(sound);
				}
			});
			return sound;
		} else
			return null;
	}

	private static Map<HideEntitySound, Integer> delayedSounds = new HashMap<>();
	private static int time;

	/**tickアップデート*/
	public static void update() {
		time++;
		Iterator<Entry<HideEntitySound, Integer>> itr = delayedSounds.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<HideEntitySound, Integer> entry1 = itr.next();
			if (time >= entry1.getValue()) {
				Minecraft.getMinecraft().getSoundHandler().playSound(entry1.getKey());
				itr.remove();
			}
		}
	}

	public static boolean isSoundPlaying(HideEntitySound sound) {
		return delayedSounds.containsKey(sound) ||
				Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound);
	}

	/**キャンセルに対応したディレイ付き再生*/
	public static void playSound(HideEntitySound sound) {
		int delay = sound.getDelay();
		if (delay > 0) {
			delayedSounds.put(sound, delay + time);
		} else {
			Minecraft.getMinecraft().getSoundHandler().playSound(sound);
		}
	}

	public static void stopSound(HideEntitySound sound) {
		if (delayedSounds.containsKey(sound))
			delayedSounds.remove(sound);
		else
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
	}
}
