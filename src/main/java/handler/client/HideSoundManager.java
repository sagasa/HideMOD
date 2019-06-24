package handler.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import gamedata.HideEntitySound;
import handler.SoundHandler;
import net.minecraft.entity.Entity;
import types.effect.Sound;

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
			if (!SoundHandler.isSoundPlaying(entry.getValue()))
				itr.remove();
		}
	}

	public void playSound(Sound sound, HideSoundType type) {
		updateSound();
		stopSound(type);
		sounds.put(type, SoundHandler.playSound(entity, X, Y, Z, sound));
	}

	public void stopSound(HideSoundType type) {
		if (sounds.containsKey(type)) {
			SoundHandler.stopSound(sounds.get(type));
			sounds.remove(type);
		}
	}
}
