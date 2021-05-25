package hide.guns;

import java.util.Iterator;

import hide.guns.data.HideEntityDataManager;
import hide.guns.data.LoadedMagazine.Magazine;
import hide.guns.entiry.EntityBullet;
import hide.guns.network.PacketSyncMag;
import hide.types.items.GunData;
import hide.types.items.MagazineData;
import hide.types.util.DataView;
import hide.ux.SoundHandler;
import hidemod.HideMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import pack.PackData;

public class ServerGun extends CommonGun {

	protected EntityPlayerMP owner;

	public ServerGun(EnumHand hand) {
		super(hand);
	}

	public void setOwner(EntityPlayerMP player) {
		owner = player;
	}

	@Override
	public boolean updateTag(NBTTagCompound gunTag) {
		//持ち替えていない＋銃のマガジンが変化したなら
		boolean magChange = gun != null && gunTag != null && !HideGunNBT.getGunLoadedMagazines(gunTag).magEquals(HideGunNBT.getGunLoadedMagazines(gun));
		boolean res = super.updateTag(gunTag);

		if (!res && magChange) {
			//		HideNBT.setGunLoadedMagazines(gun, magazine);
			magazine = HideGunNBT.getGunLoadedMagazines(gun);
			HideMod.NETWORK.sendTo(new PacketSyncMag(magazine, hand), owner);
		}
		return res;
	}

	@Override
	protected void updateData() {
		lastShootTime = 0;
		if (isGun()) {
			magazine = HideGunNBT.getGunLoadedMagazines(gun);

			int shootdelay = HideGunNBT.getGunShootDelay(gun);
			if (shootdelay != 0)
				lastShootTime = System.currentTimeMillis() - RPMtoMillis(dataView.get(GunData.RPM)) + shootdelay;
		}
		stopReload();
	}

	/** リロードの時間保存 */
	protected int reloadTime = -1;
	/** リロードの状況保存0でリロード完了処理 */
	protected int reloadProgress = -1;

	@Override
	public void tickUpdate() {
		if (!isGun())
			return;
		//	System.out.println("magazineState " + magazine);/*

		// リロードタイマー
		if (reloadProgress > 0) {
			reloadProgress--;
			HideEntityDataManager.setReloadState(owner, reloadProgress / (float) reloadTime);
		} else if (reloadProgress == 0) {
			reloadProgress = -1;
			HideEntityDataManager.setReloadState(owner, -1);
			log.debug("reload timer end, start reload");
			reload();
		}
		//ShootDelayが残っていれば
		if (lastShootTime != 0) {
			int shootdelay = (int) (RPMtoMillis(dataView.get(GunData.RPM)) - (System.currentTimeMillis() - lastShootTime));
			//残りが0になったら消しとく
			if (0 < shootdelay) {
				HideGunNBT.setGunShootDelay(gun, shootdelay);
			} else {
				HideGunNBT.setGunShootDelay(gun, 0);
				lastShootTime = 0;
			}
		}
		//TODO
		HideGunNBT.setGunLoadedMagazines(gun, magazine);
	}

	/** 保存時のShootDelay補完用 */
	protected long lastShootTime = 0;

	/** サーバーサイド */
	public void shoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch) {
		shoot(dataView, magazine.getNextBullet(), owner, isADS, offset, x, y, z, yaw, pitch);
		stopReload();

		World world = owner.world;
		if (world instanceof WorldServer) {
			WorldServer worldserver = (WorldServer) world;
			//worldserver.spawnParticle(EnumParticleTypes.BARRIER, true, x, y, z, 5, 0.0, 0.0, 0.0, 1.0);
		}

		lastShootTime = System.currentTimeMillis();
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(DataView<GunData> dataView, MagazineData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		if (bulletdata != null && bulletdata.BULLETDATA != null) {
			SoundHandler.broadcastSound(shooter, 0, 0, 0, dataView.get(GunData.SoundShoot), true);
			for (int i = 0; i < bulletdata.BULLETDATA.SHOOT_NUM; i++) {
				EntityBullet bullet = new EntityBullet(dataView.getView(), shooter, isADS, offset, x, y, z, yaw,
						pitch);
				if (!bullet.isDead)
					shooter.world.spawnEntity(bullet);
			}
		}
	}

	protected void stopReload() {
		if (reloadProgress != -1) {
			reloadProgress = -1;
			SoundHandler.bloadcastCancel(owner.world, owner.getEntityId(), SOUND_RELOAD);
		}
	}

	/**
	 * リロード まだリロード処理が残ればtrue サーバーサイド
	 */

	/** リロードの必要があるかかチェック */
	public boolean needReload() {

		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < dataView.get(GunData.LoadSize)) {
			return true;
		}
		Magazine maxMag = magazineHolder.getMaxMagazine(getUseMagazines());

		if (maxMag.num == 0)
			return false;
		MagazineData maxData = PackData.getBulletData(maxMag.name);
		float maxLoad = maxMag.num / (float) maxData.MAGAZINE_SIZE;
		Iterator<Magazine> itr = magazine.getList().iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			MagazineData magData = PackData.getBulletData(mag.name);
			if (magData == null || mag.num < magData.MAGAZINE_SIZE) {
				//それ以上のマガジンがあるかチェック
				float minLoad = mag.num / (float) magData.MAGAZINE_SIZE;
				if (minLoad < maxLoad)
					return true;
			}
		}
		return false;
	}

	/**オプションを考慮したマガジン排出処理*/
	private void exitMagazine(Magazine mag) {
		MagazineData magData = PackData.getBulletData(mag.name);
		if (magData != null && (0 < mag.num || !magData.MAGAZINE_BREAK))
			magazineHolder.addMagazine(mag.name, mag.num);
	}

	private int prevAddReloadTime = 0;

	/**
	 * プレリロード マガジンを外す
	 */
	public boolean preReload(int addReloadTime) {
		// ReloadAllの場合リロード可能なマガジンをすべて取り外す
		// ReloadAll以外+アンロードが許可されている+空スロットがない場合同じ種類で1番少ないマガジンを取り外す
		// リロードカウントを始める
		if (!isGun() || !needReload())
			return false;
		//リロード中ならdsq
		if (reloadProgress != -1) {
			unload();
			return false;
		}

		//空のスロットがない+アンロードが許可されていないなら止める
		magazine.removeEmpty();
		if (magazine.getList().size() >= dataView.get(GunData.LoadSize) && !dataView.get(GunData.UnloadInReload)) {
			return false;
		}
		//リロードできる弾があるか
		if (magazineHolder.getBulletCount(getUseMagazines()) == 0) {
			return false;
		}
		//リロード開始
		// 音
		SoundHandler.broadcastSound(owner, 0, 0, 0,
				dataView.get(GunData.SoundReload), false, SOUND_RELOAD);
		reloadTime = dataView.get(GunData.ReloadTick) + addReloadTime;
		reloadProgress = reloadTime;
		//複数回リロード用に追加時間を保存
		prevAddReloadTime = addReloadTime;
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < dataView.get(GunData.LoadSize) && !dataView.get(GunData.ReloadAll)) {
			return true;
		}

		//最小のマガジンを検出
		float min = 1f;
		Magazine minMag = null;

		for (int i = magazine.getList().size() - 1; 0 <= i; i--) {
			Magazine mag = magazine.getList().get(i);
			MagazineData magData = PackData.getBulletData(mag.name);
			//存在しないマガジンなら排出
			if (magData == null) {
				magazine.getList().remove(i);
			}
			//reloadAllなら問答無用で排出
			else if (dataView.get(GunData.ReloadAll) && mag.num < magData.MAGAZINE_SIZE) {
				exitMagazine(mag);
				magazine.getList().remove(i);
			} else {
				float dia = mag.num / (float) magData.MAGAZINE_SIZE;
				if (dia < min) {
					min = dia;
					minMag = mag;
				}
			}
		}

		if (!dataView.get(GunData.ReloadAll) && minMag != null) {
			magazine.getList().remove(minMag);
			exitMagazine(minMag);
		}
		HideMod.NETWORK.sendTo(new PacketSyncMag(magazine, hand), owner);
		return true;
	}

	protected void reload() {
		if (magazine.getList().size() >= dataView.get(GunData.LoadSize)) {
			log.info(magazine.getList());
			log.info("reload stop! magazine is full");
			return;
		}

		Magazine mag = magazineHolder.useMagazine(getUseMagazines());
		if (mag.num > 0) {
			magazine.addMagazinetoLast(mag);
			// 全リロードの場合ループ
			if (dataView.get(GunData.ReloadAll))
				reload();
			else
				preReload(prevAddReloadTime);
		}
		HideMod.NETWORK.sendTo(new PacketSyncMag(magazine, hand), owner);
	}

	/** 弾排出 */
	public void unload() {
		for (Magazine mag : magazine.getList()) {
			System.out.println("reload exit " + magazine);
			if (PackData.getBulletData(mag.name) != null) {
				magazineHolder.addMagazine(mag.name, mag.num);
			}
		}
		magazine.getList().clear();
	}
}
