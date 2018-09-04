package handler;

import entity.EntityBullet;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import types.BulletData;
import types.GunData;

/**射撃処理 Mod全体での共通のEntityBullet生成処理*/
public class GunManager {

	public static void shoot(GunData gundata, BulletData bulletdata,Entity shooter){
		shoot(gundata, bulletdata, shooter, shooter.posX, shooter.posY+shooter.getEyeHeight(), shooter.posZ, shooter.rotationYaw, shooter.rotationPitch);
	}

	public static void shoot(GunData gundata, BulletData bulletdata,Entity shooter ,double x, double y, double z, float yaw, float pitch) {
		EntityBullet bullet = new EntityBullet(gundata,bulletdata,shooter,x,y,z,yaw,pitch);
		shooter.world.spawnEntity(bullet);
	}
}
