package handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

/** 銃火器の状態をサーバー・クライアント間で同期するためのマネージャー */
public class HideEntityDataManager {

	private static final DataParameter<Float> ADS_KEY = EntityDataManager.<Float> createKey(EntityPlayer.class,
			DataSerializers.FLOAT);
	private static final DataParameter<Float> RELOAD_KEY = EntityDataManager.<Float> createKey(EntityPlayer.class,
			DataSerializers.FLOAT);

	public static void onEntityInit(EntityConstructing event) {
		Entity e = event.getEntity();
		if (e instanceof EntityPlayer) {
			System.out.println("DataManager書き込みたい");
			e.getDataManager().register(RELOAD_KEY, -1f);
			e.getDataManager().register(ADS_KEY, 0f);

		}
	}

	/** リロードステートを取得 無ければ初期値ー1 */
	public static float getReloadState(Entity entity) {
		EntityDataManager dm = entity.getDataManager();
		if (dm.getAll().contains(RELOAD_KEY)) {
			return dm.get(RELOAD_KEY);
		}
		return -1;
	}

	/** リロードステートを書き込み 無ければfalse */
	public static boolean setReloadState(Entity entity, float value) {
		EntityDataManager dm = entity.getDataManager();
		if (dm.getAll().contains(RELOAD_KEY)) {
			dm.set(RELOAD_KEY, value);
			return true;
		}
		return false;
	}

	/** ADSステートを取得 無ければ初期値0 */
	public static float getADSState(Entity entity) {
		EntityDataManager dm = entity.getDataManager();
		return dm.get(ADS_KEY);
	}

	/** ADSステートを書き込み 無ければfalse */
	public static boolean setADSState(Entity entity, float value) {
		EntityDataManager dm = entity.getDataManager();
		dm.set(ADS_KEY, value);
		if (dm.getAll().contains(ADS_KEY)) {
			dm.set(ADS_KEY, value);
			return true;
		}
		return false;
	}
}
