package handler;

import entity.EntityBullet;
import gamedata.Gun;
import gamedata.LoadedMagazine;
import gamedata.HidePlayerData.ServerPlayerData;
import helper.NBTWrapper;
import hideMod.PackData;
import item.ItemGun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import newwork.PacketShoot;
import types.BulletData;
import types.GunData;
import types.GunFireMode;

/** 射撃処理 Mod全体での共通のEntityBullet生成処理 射撃時の処理はクライアントに集約 */
public class GunManager {
	public static void gunUpdateDefaultPos(Entity shooter, Gun gun, GunFireMode mode, boolean isADS, boolean trigger) {
		gunUpdate(shooter, gun, mode, isADS, trigger, shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ,
				shooter.rotationYaw, shooter.rotationPitch);
	}

	// (max - min) * coe
	/** 銃の射撃処理 */
	public static void gunUpdate(Entity shooter, Gun gun, GunFireMode mode, boolean isADS, boolean trigger, double x,
			double y, double z, float yaw, float pitch) {
		if (mode == GunFireMode.SEMIAUTO && !gun.stopshoot && gun.shootDelay <= 0 && trigger) {
			if (gun.shootDelay < 0) {
				gun.shootDelay = 0;
			}
			shoot(shooter, gun, isADS, x, y, z, yaw, pitch);
			gun.shootDelay += toTick(gun.gundata.RPM);
			gun.stopshoot = true;
		} else if (mode == GunFireMode.FULLAUTO && !gun.stopshoot && gun.shootDelay <= 0 && trigger) {
			while (gun.shootDelay <= 0 && !gun.stopshoot) {
				shoot(shooter, gun, isADS, x, y, z, yaw, pitch);
				gun.shootDelay += toTick(gun.gundata.RPM);
			}
		} else if (mode == GunFireMode.BURST && !gun.stopshoot) {
			// 射撃開始
			if (trigger && gun.shootNum == -1 && gun.shootDelay <= 0 && !gun.stopshoot) {
				gun.shootNum = gun.gundata.BURST_BULLET_NUM;
			}
			while (gun.shootNum > 0 && gun.shootDelay <= 0 && !gun.stopshoot) {
				shoot(shooter, gun, isADS, x, y, z, yaw, pitch);
				gun.shootDelay += toTick(gun.gundata.BURST_RPM);
				;
				gun.shootNum--;
			}
			if (gun.shootNum == 0) {
				gun.stopshoot = true;
				gun.shootNum = -1;
				gun.shootDelay += toTick(gun.gundata.RPM);
			}
			if (gun.stopshoot) {
				gun.shootNum = -1;
			}

		} else if (mode == GunFireMode.MINIGUN && !gun.stopshoot && gun.shootDelay <= 0 && trigger) {
			while (gun.shootDelay <= 0 && !gun.stopshoot) {
				shoot(shooter, gun, isADS, x, y, z, yaw, pitch);
				gun.shootDelay += toTick(gun.gundata.RPM);
			}
		}
		if (!trigger) {
			gun.stopshoot = false;
		}
	}

	/** 弾があったら射撃 無ければ射撃停止 */
	private static void shoot(Entity shooter, Gun gun, boolean isADS, double x, double y, double z, float yaw,
			float pitch) {
		BulletData bullet = gun.magazine.useNextBullet();
		if (bullet != null) {
			if (shooter.world.isRemote) {
				// クライアントなら
				PacketHandler.INSTANCE.sendToServer(
						new PacketShoot(gun.gundata, bullet, shooter, x, y, z, yaw, pitch, gun.shootDelay + 1f, isADS,0));//TODO
			} else {
				shoot(gun.gundata, bullet, shooter, x, y, z, yaw, pitch, gun.shootDelay + 1f, isADS);
			}
		} else {
			gun.stopshoot = true;
		}
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	public static void shoot(GunData gundata, BulletData bulletdata, Entity shooter, double x, double y, double z,
			float yaw, float pitch, float offset, boolean isADS) {
		for (int i = 0; i < bulletdata.SHOOT_NUM; i++) {
			SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
			EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, x, y, z, yaw, pitch, offset, isADS);
			shooter.world.spawnEntity(bullet);
		}
	}

	/** RPMをTickに変換 */
	private static float toTick(int rpm) {
		return 1200f / rpm;
	}
}
