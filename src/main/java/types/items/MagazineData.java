package types.items;

import types.base.DataBase;
import types.effect.Explosion;
import types.effect.Sound;
import types.projectile.BulletData;
import types.projectile.ProjectileData;

public class MagazineData extends ItemData{
	/** 装弾数 : int型 **/
	public int MAGAZINE_SIZE = 10;

	/**リロード時にマガジンが破棄されるか : boolean型**/
	public boolean MAGAZINE_BREAK = true;

	/**内容*/
	public ProjectileData PROJECTILE = new BulletData();
}
