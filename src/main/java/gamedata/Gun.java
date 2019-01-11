package gamedata;

import java.util.List;

import entity.EntityBullet;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.RecoilHandler;
import handler.SoundHandler;
import helper.NBTWrapper;
import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import newwork.PacketShoot;
import types.attachments.GunCustomizePart;
import types.base.GunFireMode;
import types.items.GunData;
import types.items.MagazineData;
import types.projectile.BulletData;

/** 銃の制御系 */
public class Gun {
	// ===============クライアント,サーバー共通部分==================
	private GunData originalData = null;
	private List<GunCustomizePart> customize = null;
	private GunData modifyData = null;

	/** カスタムとオリジナルから修正版のGunDataを作成 */
	private void updateCustomize() {
		if (customize != null && originalData != null) {
			modifyData = (GunData) originalData.clone();
			customize.forEach(part -> {
				modifyData.multiplyFloat(part.FLOAT_DIA_MAP);
				modifyData.addFloat(part.FLOAT_ADD_MAP);
				modifyData.setFloat(part.FLOAT_SET_MAP);
				modifyData.setString(part.STRING_SET_MAP);
			});
		}
	}

	public void setGun(GunData data) {
		originalData = data;
		updateCustomize();
	}

	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		if (magazine.getLoadedNum() > 0 && !stopshoot && shootDelay <= 0 & shootNum <= 0) {
			return true;
		}
		return false;
	}

	/** ItemGunから弾を消費する */
	public static boolean useBullet(EntityPlayer player, long uid) {
		for (ItemStack item : player.inventory.offHandInventory) {
			if (ItemGun.isGun(item) && NBTWrapper.getHideID(item) == uid
					&& NBTWrapper.getGunLoadedMagazines(item).getLoadedNum() > 0) {
				LoadedMagazine newmagazine = NBTWrapper.getGunLoadedMagazines(item);
				newmagazine.useNextBullet();
				NBTWrapper.setGunLoadedMagazines(item, newmagazine);
				return true;
			}
		}
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemGun.isGun(item) && NBTWrapper.getHideID(item) == uid
					&& NBTWrapper.getGunLoadedMagazines(item).getLoadedNum() > 0) {
				LoadedMagazine newmagazine = NBTWrapper.getGunLoadedMagazines(item);
				newmagazine.useNextBullet();
				NBTWrapper.setGunLoadedMagazines(item, newmagazine);
				return true;
			}
		}
		return false;
	}

	// ============================================================================================

	// ==================クライアント側入力系========================
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
				// System.out.println("Magazine更新");
			}
			amount = now.getLoadedNum();
		} else if (Mode == GunVehicle) {

		}
	}

	//射撃アップデート
	private long lastTime = Minecraft.getSystemTime();
	private boolean stopshoot = false;
	private int shootDelay = 0;
	private int shootNum = 0;

	/** 銃のアップデート処理 トリガー関連 */
	private void gunUpdate(GunFireMode mode, boolean trigger) {
		if (mode == GunFireMode.SEMIAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(shootDelay + 1f);
			shootDelay += RPMtoMillis(modifyData.RPM);
			stopshoot = true;
		} else if (mode == GunFireMode.FULLAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(shootDelay + 1f);
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		} else if (mode == GunFireMode.BURST && !stopshoot) {
			// 射撃開始
			if (trigger && shootNum == -1 && shootDelay <= 0 && !stopshoot) {
				shootNum = modifyData.BURST_BULLET_NUM;
			}
			while (shootNum > 0 && shootDelay <= 0 && !stopshoot) {
				shoot(shootDelay + 1f);
				shootDelay += RPMtoMillis(modifyData.BURST_RPM);
				shootNum--;
			}
			if (shootNum == 0) {
				stopshoot = true;
				shootNum = -1;
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
			if (stopshoot) {
				shootNum = -1;
			}

		} else if (mode == GunFireMode.MINIGUN && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(shootDelay + 1f);
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		}
		if (!trigger) {
			stopshoot = false;
		}

		if (0 < shootDelay) {
			shootDelay -= Minecraft.getSystemTime() - lastTime;
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
				// シューターがプレイヤー以外ならエラー
				if (!(Shooter instanceof EntityPlayer)) {
					System.err.println("プレイヤー以外のクライアントからの発射メゾットの実行はできません");
				}
				RecoilHandler.addRecoil(gundata);
				PacketHandler.INSTANCE
						.sendToServer(new PacketShoot(gundata, bullet, isADS, offset, X, Y, Z, yaw, pitch, uid));
			} else {
				shoot(gundata, bullet, Shooter, isADS, offset, X, Y, Z, yaw, pitch);
			}
		} else {
			stopshoot = true;
		}
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	public static void shoot(GunData gundata, MagazineData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		if (bulletdata.BULLET != null) {
			for (int i = 0; i < bulletdata.BULLET.SHOOT_NUM; i++) {
				SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
				EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, isADS, offset, x, y, z, yaw,
						pitch);
				shooter.world.spawnEntity(bullet);
			}
		}
	}

	/** RPMをミリ秒に変換 */
	private static int RPMtoMillis(int rpm) {
		return 60000 / rpm;
	}
	/** ミリ秒をTickに変換 */
	private static float MillistoTick(int millis) {
		return millis/50;
	}
}
