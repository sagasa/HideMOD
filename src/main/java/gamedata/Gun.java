package gamedata;

import entity.EntityBullet;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.RecoilHandler;
import handler.SoundHandler;
import helper.NBTWrapper;
import item.ItemGun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import network.PacketShoot;
import types.guns.BulletData;
import types.guns.GunData;
import types.guns.GunFireMode;

/** 銃の制御系 NBTからアップデートで読み取り */
public class Gun {
	public GunData gundata;
	public LoadedMagazine magazine;

	private byte Mode;
	private static final byte GunItem = 0;
	private static final byte GunVehicle = 1;

	public ItemStack itemGun;
	private long uid;
	/** アイテムの銃から作成 */
	public Gun(ItemStack gun) {
		Mode = GunItem;
		itemGun = gun;
		gundata = ItemGun.getGunData(itemGun);
		magazine = NBTWrapper.getGunLoadedMagazines(itemGun);
		uid =  NBTWrapper.getHideID(itemGun);
	}

	// ============================================================================================
	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		if (magazine.getLoadedNum() > 0 && !stopShoot && shootDelay <= 0 & shootNum <= 0) return true;
		return false;
	}

	/**ItemGunから弾を消費する*/
	public static boolean useBullet(EntityPlayer player,long uid){
		for(ItemStack item : player.inventory.offHandInventory){
			if (getMagazineCondition(uid, item)) return true;
		}
		for(ItemStack item : player.inventory.mainInventory){
			if (getMagazineCondition(uid, item)) return true;
		}
		return false;
	}

	private static boolean getMagazineCondition(long uid, ItemStack item) {
		if(ItemGun.isGun(item)&&NBTWrapper.getHideID(item)==uid&&NBTWrapper.getGunLoadedMagazines(item).getLoadedNum()>0){
			LoadedMagazine newMagazine = NBTWrapper.getGunLoadedMagazines(item);
			newMagazine.useNextBullet();
			NBTWrapper.setGunLoadedMagazines(item, newMagazine);
			return true;
		}
		return false;
	}


	// ============================================================================================

	private double X;
	private double Y;
	private double Z;
	private float Yaw;
	private float Pitch;
	private Entity Shooter;

	private Float lastYaw = null;
	private Float lastPitch = null;

	/** 弾の出現点を設定 */
	public Gun setPos(double x, double y, double z) {
		X = x;
		Y = y;
		Z = z;
		return this;
	}

	/** 弾の向きを設定 */
	public Gun setRotate(float yaw, float pitch) {
		Yaw = yaw;
		Pitch = pitch;
		return this;
	}

	/** Tick補完用の1Tick前の向きを設定 gunUpdate毎ににクリアされる */
	public Gun setLastRotate(float yaw, float pitch) {
		lastYaw = yaw;
		lastPitch = pitch;
		return this;
	}
	//
	public void gunUpdate(Entity shooter, ItemStack item, boolean trigger) {
		Shooter = shooter;
		itemGun = item;
		update();
		gunUpdate(NBTWrapper.getGunFireMode(item), trigger);
	}

	private int amount = 0;

	/** アップデート リロードチェック系 */
	private void update() {
		// NBTの読み取り Itemモードなら増えた場合のみ適応
		if (Mode == GunItem) {
			LoadedMagazine now = NBTWrapper.getGunLoadedMagazines(itemGun);
			if (now.getLoadedNum() > amount) {
				// 読み取って適応
				magazine = now;
		//		System.out.println("Magazine更新");
			}
			amount = now.getLoadedNum();
		} else if (Mode == GunVehicle) {

		}
	}


	private boolean stopShoot = false;
	private float shootDelay = 0;
	private int shootNum = 0;

	/** 銃のアップデート処理 トリガー関連 */
	private void gunUpdate(GunFireMode mode, boolean trigger) {
		if (mode == GunFireMode.SEMIAUTO && !stopShoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(shootDelay + 1f);
			shootDelay += toTick(gundata.RPM);
			stopShoot = true;
		} else if (mode == GunFireMode.FULLAUTO && !stopShoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopShoot) {
				shoot(shootDelay + 1f);
				shootDelay += toTick(gundata.RPM);
			}
		} else if (mode == GunFireMode.BURST && !stopShoot) {
			// 射撃開始
			if (trigger && shootNum == -1 && shootDelay <= 0 && !stopShoot) {
				shootNum = gundata.BURST_BULLET_NUM;
			}
			while (shootNum > 0 && shootDelay <= 0 && !stopShoot) {
				shoot(shootDelay + 1f);
				shootDelay += toTick(gundata.BURST_RPM);
				;
				shootNum--;
			}
			if (shootNum == 0) {
				stopShoot = true;
				shootNum = -1;
				shootDelay += toTick(gundata.RPM);
			}
			if (stopShoot) {
				shootNum = -1;
			}

		} else if (mode == GunFireMode.MINIGUN && !stopShoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopShoot) {
				shoot(shootDelay + 1f);
				shootDelay += toTick(gundata.RPM);
			}
		}
		if (!trigger) {
			stopShoot = false;
		}

		if (0 < shootDelay) {
			shootDelay -= 1f;
		}
	}

	/** 射撃リクエスト */
	private void shoot(float offset) {
		BulletData bullet = magazine.useNextBullet();
		if (bullet != null) {
			boolean isADS = HideEntityDataManager.getADSState(Shooter) == 1;
			float yaw = lastYaw == null ? Yaw : lastYaw + (Yaw - lastYaw) * offset;
			float pitch = lastPitch == null ? Pitch : lastPitch + (Pitch - lastPitch) * offset;
			if (Shooter.world.isRemote) {
				// クライアントなら
				//シューターがプレイヤー以外ならエラー
				if(!(Shooter instanceof EntityPlayer)){
					System.err.println("プレイヤー以外のクライアントからの発射メソッドの実行はできません");
				}
				RecoilHandler.addRecoil(gundata);
				PacketHandler.INSTANCE
						.sendToServer(new PacketShoot(gundata, bullet, isADS, offset, X, Y, Z, yaw, pitch, uid));
			} else {
				shoot(gundata, bullet, Shooter, isADS, offset, X, Y, Z, yaw, pitch);
			}
		} else {
			stopShoot = true;
		}
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	public static void shoot(GunData gundata, BulletData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		for (int i = 0; i < bulletdata.SHOOT_NUM; i++) {
			SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
			EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, isADS, offset, x, y, z, yaw, pitch);
			shooter.world.spawnEntity(bullet);
		}
	}

	/** RPMをTickに変換 */
	private static float toTick(int rpm) {
		return 1200f / rpm;
	}
}
