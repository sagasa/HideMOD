package gamedata;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
import net.minecraft.nbt.NBTTagCompound;
import network.PacketShoot;
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

	private Supplier<NBTTagCompound> gunTag;

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

	public void setGun(GunData data, Supplier<NBTTagCompound> guntag) {
		originalData = data;
		gunTag = guntag;
		updateCustomize();
	}

	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		if (magazine.getLoadedNum() > 0 && !stopshoot && shootDelay <= 0 & shootNum <= 0) {
			return true;
		}
		return false;
	}

	// ==================クライアント側入力系========================
	private double X;
	private double Y;
	private double Z;
	private float Yaw;
	private float Pitch;
	private Entity Shooter;

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

	private int amount = 0;

	/** アップデート リロードチェック系 */
	private void update() {
		LoadedMagazine now = NBTWrapper.getGunLoadedMagazines(itemGun);
		if (now.getLoadedNum() > amount) {
			// 読み取って適応
			magazine = now;
			// System.out.println("Magazine更新");
		}
		amount = now.getLoadedNum();
	}

	// 射撃アップデート
	private long lastTime = Minecraft.getSystemTime();
	private boolean stopshoot = false;
	/** NBT保存 */
	private String useMagazine;
	/** NBT保存 */
	private LoadedMagazine magazine;
	/** NBT保存 */
	private GunFireMode fireMode;
	/** NBT保存 */
	private int shootDelay = 0;
	private int shootNum = 0;

	/** 銃のアップデート処理 トリガー関連 */
	public void gunUpdate(boolean trigger) {
		if (fireMode == GunFireMode.SEMIAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(shootDelay + 1f);
			shootDelay += RPMtoMillis(modifyData.RPM);
			stopshoot = true;
		} else if (fireMode == GunFireMode.FULLAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(shootDelay + 1f);
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		} else if (fireMode == GunFireMode.BURST && !stopshoot) {
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

		} else if (fireMode == GunFireMode.MINIGUN && !stopshoot && shootDelay <= 0 && trigger) {
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
		MagazineData bullet = magazine.useNextBullet();
		if (bullet != null) {
			boolean isADS = HideEntityDataManager.getADSState(Shooter) == 1;
			if (Shooter.world.isRemote) {
				// クライアントなら
				// シューターがプレイヤー以外ならエラー
				if (!(Shooter instanceof EntityPlayer)) {
					System.err.println("プレイヤー以外のクライアントからの発射メゾットの実行はできません");
				}
				RecoilHandler.addRecoil(modifyData);
				PacketHandler.INSTANCE
						.sendToServer(new PacketShoot(gundata, bullet, isADS, offset, X, Y, Z, Yaw, Pitch, uid));
			} else {
				shoot(gundata, bullet, Shooter, isADS, offset, X, Y, Z, Yaw, Pitch);
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
		return millis / 50;
	}
}
