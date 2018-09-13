package gamedata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import hideMod.PackData;
import types.BulletData;

/** 装填済みのマガジン管理用 変更通知機能付き */
public class LoadedMagazine {
	private List<Magazine> magazineList = new ArrayList<>();

	/** 単体のマガジン */
	public class Magazine {
		public String name;
		public int num;

		public Magazine(String name, int num) {
			this.name = name;
			this.num = num;
		}

		@Override
		public String toString() {
			return super.toString() + "[name=" + name + ",num=" + num + "]";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof Magazine && name.equals(((Magazine) obj).name)
					&& num == ((Magazine) obj).num) {
				return true;
			}
			return false;
		}
	}

	/** 次に撃つ弾を取得 */
	public BulletData getNextBullet() {
		Magazine mag = getNextMagazine();
		if (mag == null) {
			return null;
		} else {
			return PackData.getBulletData(mag.name);
		}
	}

	/** 次に撃つ弾を取得 消費する */
	public BulletData useNextBullet() {
		Magazine mag = getNextMagazine();
		if (mag == null) {
			return null;
		} else {
			mag.num--;
			if(magazineList.size()>0&&mag.num<=0&&PackData.getBulletData(mag.name).MAGAZINE_BREAK){
				magazineList.remove(0);
			}
			return PackData.getBulletData(mag.name);
		}
	}
	/**初めの弾が入ったマガジンを返す*/
	private Magazine getNextMagazine() {
		if (magazineList.size() > 0 && magazineList.get(0).num > 0) {
			if (PackData.getBulletData(magazineList.get(0).name) == null) {
				magazineList.remove(0);
				return getNextMagazine();
			}
			return magazineList.get(0);
		}else if(magazineList.size()>0&&magazineList.get(0).num <=0&&PackData.getBulletData( magazineList.get(0).name).MAGAZINE_BREAK){
	//		System.out.println("Size "+ magazineList.size());
			magazineList.remove(0);
		}
		return null;
	}

	public void addMagazinetoFast(@Nonnull Magazine mag) {
		magazineList.add(0, mag);
	}

	public void addMagazinetoLast(@Nonnull Magazine mag) {
		magazineList.add(mag);
	}

	public List<Magazine> getList() {
		return magazineList;
	}

	@Override
	public String toString() {
		return super.toString() + magazineList;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof LoadedMagazine && magazineList.equals(((LoadedMagazine) obj).magazineList)) {
			return true;
		}
		return false;
	}

	/** 今の残弾を返す */
	public int getLoadedNum() {
		int num = 0;
		for (Magazine magazine : magazineList) {
			if (magazine != null) {
				num += magazine.num;
			}
		}
		return num;
	}
}