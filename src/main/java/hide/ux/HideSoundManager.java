package hide.ux;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import hide.types.effects.Sound;
import hide.types.util.DataView.ViewCache;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HideSoundManager {
	public enum HideSoundType {
		GUN_RELOAD, GUN_SHOOT;
	}

	private final Entity entity;
	private final double X, Y, Z;

	private Map<HideSoundType, HideEntitySound> sounds = new ConcurrentHashMap<>();

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

	public void playSound(ViewCache<Sound> sound, HideSoundType type) {
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

	public static HideEntitySound playSound(Entity entity, double x, double y, double z, ViewCache<Sound> sound) {
		return playSound(entity, sound.get(Sound.Name), x, y, z, sound.get(Sound.Range), sound.get(Sound.Volume), sound.get(Sound.Pitch), sound.get(Sound.UseDelay),
				sound.get(Sound.UseDecay));
	}

	public static HideEntitySound playSound(Entity entity, String soundName, double x, double y, double z, float range,
			float vol,
			float pitch,
			boolean useSoundDelay, boolean useDecay) {
		HideEntitySound sound = new HideEntitySound(entity, soundName, x, y, z, vol, pitch, range, useSoundDelay,
				useDecay,
				SoundCategory.PLAYERS);
		playSound(sound);
		return sound;
	}

	public static void playSound(HideEntitySound sound, byte cate) {
		cateSoundMap.put(makeKey(sound.getEntity().getEntityId(), cate), sound);
		playSound(sound);
	}

	public static void stopSound(int entity, byte cate) {
		long key = makeKey(entity, cate);
		stopSound(cateSoundMap.get(key));
		cateSoundMap.remove(key);
	}

	private static long makeKey(int entityID, byte cate) {
		return (((long) cate) << 32) + entityID;
	}

	private static Map<Long, HideEntitySound> cateSoundMap = new ConcurrentHashMap<>();

	private static Map<HideEntitySound, Integer> delayedSounds = new ConcurrentHashMap<>();
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
		Iterator<Entry<Long, HideEntitySound>> itrcate = cateSoundMap.entrySet().iterator();
		while (itrcate.hasNext())
			if (itrcate.next().getValue().isDonePlaying())
				itrcate.remove();
	}

	public static boolean isSoundPlaying(HideEntitySound sound) {
		return delayedSounds.containsKey(sound) ||
				Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound);
	}

	/**キャンセルに対応したディレイ付き再生*/
	public static HideEntitySound playSound(HideEntitySound sound) {
		int delay = sound.getDelay();
		if (delay > 0) {
			delayedSounds.put(sound, delay + time);
		} else {
			delayedSounds.put(sound, delay + time);
			//Minecraft.getMinecraft().getSoundHandler().playSound(sound);
		}
		return sound;
	}

	public static void stopSound(HideEntitySound sound) {
		if (delayedSounds.containsKey(sound))
			delayedSounds.remove(sound);
		else
			Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
	}
}
