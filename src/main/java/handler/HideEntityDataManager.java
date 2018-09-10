package handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

/** 銃火器の状態をサーバー・クライアント間で同期するためのマネージャー */
public class HideEntityDataManager {

	private static final DataParameter<Float> ADS_KEY = EntityDataManager.<Float> createKey(Entity.class,
			DataSerializers.FLOAT);
	private static final DataParameter<Float> RELOAD_KEY = EntityDataManager.<Float> createKey(Entity.class,
			DataSerializers.FLOAT);

	private static final Class[] AddStateClass = new Class[] { EntityLivingBase.class };

	public static void onEntityInit(EntityConstructing event) {
		Entity e = event.getEntity();
		for (Class clazz : AddStateClass) {
			if (clazz.isInstance(e)) {
				e.getDataManager().register(RELOAD_KEY, -1f);
				e.getDataManager().register(ADS_KEY, 0f);
			}
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
		if (dm.getAll().contains(ADS_KEY)) {
			return dm.get(ADS_KEY);
		}
		return 0;
	}

	/** ADSステートを書き込み 無ければfalse */
	public static boolean setADSState(Entity entity, float value) {
		EntityDataManager dm = entity.getDataManager();
		if (dm.getAll().contains(ADS_KEY)) {
			dm.set(ADS_KEY, value);
			return true;
		}
		return false;
	}
}
