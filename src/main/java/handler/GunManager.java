package handler;

import entity.EntityBullet;
import helper.NBTWrapper;
import hideMod.PackData;
import item.ItemGun;
import item.LoadedMagazine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import playerdata.GunState;
import playerdata.HidePlayerData.ServerPlayerData;
import types.BulletData;
import types.GunData;
import types.GunFireMode;

/** 射撃処理 Mod全体での共通のEntityBullet生成処理 */
public class GunManager {

	public static void shoot(GunData gundata, BulletData bulletdata, Entity shooter) {
		shoot(gundata, bulletdata, shooter, shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ,
				shooter.rotationYaw, shooter.rotationPitch);
	}

	/** 銃の射撃処理 */
	public static void shoot(Entity shooter, GunData gundata, LoadedMagazine[] magazines, GunFireMode mode,
			GunState state, double x, double y, double z, float yaw, float pitch) {
		if (mode == GunFireMode.SEMIAUTO && !state.stopshoot && state.shootDelay <= 0) {
			if(state.shootDelay<0){
				state.shootDelay = 0;
			}
			shoot(shooter, gundata, magazines, state, x, y, z, yaw, pitch);
			state.shootDelay += gundata.RATE_TICK;
			state.stopshoot = true;
		} else if (mode == GunFireMode.FULLAUTO && !state.stopshoot && state.shootDelay <= 0) {
			while(state.shootDelay<=0&&!state.stopshoot){
				shoot(shooter, gundata, magazines, state, x, y, z, yaw, pitch);
				state.shootDelay += gundata.RATE_TICK;
			}
		} else if (mode == GunFireMode.BURST && !state.stopshoot && state.shootDelay <= 0) {

		} else if (mode == GunFireMode.MINIGUN && !state.stopshoot && state.shootDelay <= 0) {

		}
	}

	/** 弾があったら射撃 無ければ射撃停止 */
	private static void shoot(Entity shooter, GunData gundata, LoadedMagazine[] magazines, GunState state, double x,
			double y, double z, float yaw, float pitch) {
		BulletData bullet = getNextBullet(magazines);
		if (bullet != null) {
			shoot(gundata, bullet, shooter, x, y, z, yaw, pitch,shooter.world.getWorldTime());
		} else {
			state.stopshoot = true;
		}
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(GunData gundata, BulletData bulletdata, Entity shooter, double x, double y, double z,
			float yaw, float pitch) {
		for (int i = 0; i < bulletdata.SHOOT_NUM; i++) {
			EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, x, y, z, yaw, pitch);
			shooter.world.spawnEntity(bullet);
		}
	}

	/** 弾を1つ消費する 消費した弾のBulletDataを返す */
	private static BulletData getNextBullet(LoadedMagazine[] loadedMagazines) {
		for (int i = 0; i < loadedMagazines.length; i++) {
			LoadedMagazine magazine = loadedMagazines[i];
			// 1つ消費する
			if (magazine != null && magazine.num > 0) {
				String name = magazine.name;
				magazine.num--;
				if (magazine.num <= 0) {
					magazine = null;
					// マガジン繰り上げ
					if (loadedMagazines.length > 1) {
						for (int j = 1; j < loadedMagazines.length; j++) {
							loadedMagazines[j - 1] = loadedMagazines[j];
						}
						loadedMagazines[loadedMagazines.length - 1] = null;
					}
				}
				return PackData.getBulletData(name);
			}
		}
		return null;
	}

}
