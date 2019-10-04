package handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

/** 銃火器の状態をサーバー・クライアント間で同期するためのマネージャー */
public class HideEntityDataManager {

	private static final DataParameter<Float> ADS_KEY = EntityDataManager.<Float> createKey(EntityPlayer.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> RELOAD_KEY = EntityDataManager.<Float> createKey(EntityPlayer.class, DataSerializers.FLOAT);

	public static void onEntityInit(EntityConstructing event) {
		Entity e = event.getEntity();
		if (e instanceof EntityPlayer) {
			e.getDataManager().register(RELOAD_KEY, -1f);
			e.getDataManager().register(ADS_KEY, 0f);
		}
	}

	/** リロードステートを取得 無ければ初期値ー1 */
	public static float getReloadState(EntityLivingBase entity) {
		EntityDataManager dm = entity.getDataManager();
		return dm.get(RELOAD_KEY);
	}

	/** リロードステートを書き込み 無ければfalse */
	public static void setReloadState(EntityLivingBase entity, float value) {
		EntityDataManager dm = entity.getDataManager();
		dm.set(RELOAD_KEY, value);
	}

	/** ADSステートを取得 無ければ初期値0 */
	public static float getADSState(EntityLivingBase entity) {
		EntityDataManager dm = entity.getDataManager();
		return dm.get(ADS_KEY);
	}

	/** ADSステートを書き込み 無ければfalse */
	public static void setADSState(EntityLivingBase entity, float value) {
		EntityDataManager dm = entity.getDataManager();
		dm.set(ADS_KEY, value);
	}
}
