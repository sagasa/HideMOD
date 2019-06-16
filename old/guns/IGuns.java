package guns;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import types.base.GunFireMode;

public interface IGuns {
	/** リロード可能か */
	public boolean needReload();

	/** 発射可能な弾の総数 */
	public int getBulletAmount();

	/** 装填可能な弾の総数 */
	public int getMaxBulletAmount();

	/** 現在リロードできるマガジンの名前 */
	public String getUseMagazine();

	/** リロードするマガジンの名前をセット セットできればTrue */
	public boolean setUseMagazine(String name);

	/** 使用可能なマガジンのリストを取得 */
	public List<String> getUseMagazineList();

	/** 射撃モード */
	public GunFireMode getFireMode();

	/** 射撃モードをセット セットできればTrue */
	public boolean setFireMode(GunFireMode firemode);

	/** 射撃モードのリストを取得 */
	public List<GunFireMode> getFireModeList();

	/** リロードを始める 変化があったらTrue */
	public boolean reload(Container container);

	/** 射撃リクエスト サーバーサイドのみ */
	public void trigger(int time);
}
