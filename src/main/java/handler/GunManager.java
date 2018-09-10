package handler;

import entity.EntityBullet;
import gamedata.GunState;
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
	public static void gunUpdateDefaultPos(Entity shooter, GunData gundata, NBTTagCompound hideTag, GunFireMode mode,
			GunState state, boolean isADS, boolean trigger, float lastyaw, float lastpitch) {
		gunUpdate(shooter, gundata, hideTag, mode, state, isADS, trigger, shooter.posX,
				shooter.posY + shooter.getEyeHeight(), shooter.posZ, shooter.rotationYaw, shooter.rotationPitch,
				lastyaw, lastpitch);
	}

	/** 銃の射撃処理 */
	public static void gunUpdate(Entity shooter, GunData gundata, NBTTagCompound hideTag, GunFireMode mode,
			GunState state, boolean isADS, boolean trigger, double x, double y, double z, float yaw, float pitch,
			float lastyaw, float lastpitch) {
		if (mode == GunFireMode.SEMIAUTO && !state.stopshoot && state.shootDelay <= 0 && trigger) {
			if (state.shootDelay < 0) {
				state.shootDelay = 0;
			}
			shoot(shooter, gundata, hideTag, state, isADS, x, y, z, yaw, pitch, lastyaw, lastpitch);
			state.shootDelay += toTick(gundata.RPM);
			state.stopshoot = true;
		} else if (mode == GunFireMode.FULLAUTO && !state.stopshoot && state.shootDelay <= 0 && trigger) {
			while (state.shootDelay <= 0 && !state.stopshoot) {
				shoot(shooter, gundata, hideTag, state, isADS, x, y, z, yaw, pitch, lastyaw, lastpitch);
				state.shootDelay += toTick(gundata.RPM);
			}
		} else if (mode == GunFireMode.BURST && !state.stopshoot) {
			// 射撃開始
			if (trigger && state.shootNum == -1 && state.shootDelay <= 0 && !state.stopshoot) {
				state.shootNum = gundata.BURST_BULLET_NUM;
			}
			while (state.shootNum > 0 && state.shootDelay <= 0 && !state.stopshoot) {
				shoot(shooter, gundata, hideTag, state, isADS, x, y, z, yaw, pitch, lastyaw, lastpitch);
				state.shootDelay += toTick(gundata.BURST_RPM);
				;
				state.shootNum--;
			}
			if (state.shootNum == 0) {
				state.stopshoot = true;
				state.shootNum = -1;
				state.shootDelay += toTick(gundata.RPM);
			}
			if (state.stopshoot) {
				state.shootNum = -1;
			}

		} else if (mode == GunFireMode.MINIGUN && !state.stopshoot && state.shootDelay <= 0 && trigger) {
			while (state.shootDelay <= 0 && !state.stopshoot) {
				shoot(shooter, gundata, hideTag, state, isADS, x, y, z, yaw, pitch, lastyaw, lastpitch);
				state.shootDelay += toTick(gundata.RPM);
			}
		}
		if (!trigger) {
			state.stopshoot = false;
		}
	}

	/** 弾があったら射撃 無ければ射撃停止 */
	private static void shoot(Entity shooter, GunData gundata, NBTTagCompound hideTag, GunState state, boolean isADS,
			double x, double y, double z, float yaw, float pitch, float lastyaw, float lastpitch) {
		BulletData bullet = NBTWrapper.getNextBullet(hideTag);
		if (bullet != null) {
			// System.out.println(state.shootDelay+1f);// TODO
			shoot(gundata, bullet, shooter, x, y, z, getValue(lastyaw, yaw, state.shootDelay + 1f),
					getValue(lastpitch, pitch, state.shootDelay + 1f), state.shootDelay + 1f, isADS);
		} else {
			state.stopshoot = true;
		}
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(GunData gundata, BulletData bulletdata, Entity shooter, double x, double y, double z,
			float yaw, float pitch, float offset, boolean isADS) {
		for (int i = 0; i < bulletdata.SHOOT_NUM; i++) {
			if(shooter.world.isRemote){
				//クライアントなら
				PacketHandler.INSTANCE.sendToServer(new PacketShoot(gundata, bulletdata, shooter, x, y, z, yaw, pitch, offset, isADS,shooter.world.getTotalWorldTime()));
			}else{
				//サーバーなら
				SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
				EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, x, y, z, yaw, pitch, offset, isADS);
				shooter.world.spawnEntity(bullet);
			}
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

	/** 2つの値と0~1のfloatから間の値を返す */
	private static float getValue(float min, float max, float coe) {
		return min + (max - min) * coe;
	}

	/** RPMをTickに変換 */
	private static float toTick(int rpm) {
		return 1200f / rpm;
	}
}
