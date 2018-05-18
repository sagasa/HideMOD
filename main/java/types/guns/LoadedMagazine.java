package types.guns;

import item.ItemMagazine;
import types.BulletData.BulletDataList;

/**装填済みのマガジン管理用*/
public class LoadedMagazine{
	public String name;
	public int num;
	public LoadedMagazine(String bulletName,int bulletNum) {
		name = bulletName;
		num = bulletNum;
	}
	@Override
	public String toString() {
		return super.toString()+name+" "+num;
	}
	/**今の残弾を返す*/
	public static int getLoadedNum(LoadedMagazine[] magazines){
		int num = 0;
		for (LoadedMagazine magazine : magazines) {
			if(magazine != null){
				num += magazine.num;
			}
		}
		return num;
	}
}