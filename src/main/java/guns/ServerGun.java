package guns;

import gamedata.LoadedMagazine;
import helper.HideNBT;
import net.minecraft.util.EnumHand;

public class ServerGun extends CommonGun {



	public ServerGun(EnumHand hand) {
		super(hand);

	}

	public void saveAndClear() {
		// セーブ
		if (isGun()) {
			HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
			System.out.println("last shoot  " + lastShootTime + " " + (System.currentTimeMillis() - lastShootTime));
			int shootdelay = (int) MillistoTick(
					(int) (RPMtoMillis(modifyData.RPM) - (System.currentTimeMillis() - lastShootTime)));
			shootdelay = Math.max(0, shootdelay);
			HideNBT.setGunShootDelay(gun.getGunTag(), shootdelay);
			System.out.println("Save" + shootdelay);
		}
		// 削除
		gun = null;
		modifyData = null;
		magazine = new LoadedMagazine();
		lastShootTime = 0;
		stopReload();
	}

	@Override
	public void tickUpdate() {
		if (!isGun())
			return;
		//	System.out.println("magazineState " + magazine);/*

		// リロードタイマー
		if (reloadProgress > 0) {
			//	HideEntityDataManager.setADSState(Shooter, modifyData.RELOAD_TICK);
			reloadProgress--;
		} else if (reloadProgress == 0) {
			reloadProgress = -1;
			log.debug("reload timer end, start reload");
			reload();
		}
		//TODO HideNBT.setGunShootDelay(gun.getGunTag(), shootDelay);
		HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);

	}
}
