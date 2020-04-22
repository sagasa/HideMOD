package guns;

import gamedata.LoadedMagazine;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.client.HideSoundManager;
import handler.client.RecoilHandler;
import helper.HideMath;
import helper.HideNBT;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketShoot;
import pack.PackData;
import types.base.GunFireMode;
import types.items.GunData;
import types.items.MagazineData;

public class ClientGun extends CommonGun {

	public ClientGun(EnumHand hand) {
		super(hand);
	}

	private double X;
	private double Y;
	private double Z;
	private float Yaw;
	private float Pitch;
	private double oldX;
	private double oldY;
	private double oldZ;
	private float oldYaw;
	private float oldPitch;

	/** 弾の出現点を設定 */
	public ClientGun setPos(double x, double y, double z) {
		//System.out.println(x+" "+y+" "+z + " "+System.currentTimeMillis()+" "+hand);
		oldX = X;
		oldY = Y;
		oldZ = Z;
		X = x;
		Y = y;
		Z = z;
		return this;
	}

	/** 弾の向きを設定 */
	public ClientGun setRotate(float yaw, float pitch) {
		oldYaw = Yaw;
		oldPitch = Pitch;
		Yaw = yaw;
		Pitch = pitch;
		return this;
	}

	private long lastTime = -1;
	private int shootDelay = 0;
	private int shootNum = 0;
	private boolean stopshoot = false;

	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		return magazine.getLoadedNum() > 0 && !stopshoot && shootDelay <= 0 & shootNum <= 0;
	}

	private float completionTick = 0;

	/** 50Hzのアップデート処理プレイヤー以外はこのメゾットを使わない */
	@SideOnly(Side.CLIENT)
	public void gunUpdate(boolean trigger, float completion) {
		completionTick = completion;

		if (!isGun())
			return;

		if (lastTime == -1)
			lastTime = Minecraft.getSystemTime();
		if (0 < shootDelay)
			shootDelay -= Minecraft.getSystemTime() - lastTime;
		lastTime = Minecraft.getSystemTime();
		if (!trigger)
			stopshoot = false;
		GunFireMode firemode = getFireMode();

		if (firemode == GunFireMode.SEMIAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(MillistoTick(shootDelay));
			shootDelay += RPMtoMillis(modifyData.RPM);
			stopshoot = true;
		} else if (firemode == GunFireMode.FULLAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		} else if (firemode == GunFireMode.BURST && !stopshoot) {
			// 射撃開始
			if (trigger && shootNum == -1 && shootDelay <= 0 && !stopshoot) {
				shootNum = modifyData.BURST_BULLET_NUM;
			}
			while (shootNum > 0 && shootDelay <= 0 && !stopshoot) {
				shoot(MillistoTick(shootDelay));
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

		} else if (firemode == GunFireMode.MINIGUN && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		}
	}

	/** 射撃リクエスト */
	private void shoot(float offset) {
		MagazineData bullet = magazine.useNextBullet();
		if (bullet != null) {
			// クライアントなら
			Minecraft mc = Minecraft.getMinecraft();
			boolean isADS = HideEntityDataManager.getADSState(mc.player) == 1;

			offset += completionTick;

			HideSoundManager.playSound(mc.player, 0, 0, 0, modifyData.SOUND_SHOOT);

			RecoilHandler.addRecoil(modifyData, hand);
			double x = HideMath.completion(oldX, X, offset);
			double y = HideMath.completion(oldY, Y, offset);
			double z = HideMath.completion(oldZ, Z, offset);
			float yaw = HideMath.completion(oldYaw, Yaw, offset);
			float pitch = HideMath.completion(oldPitch, Pitch, offset);

			PacketHandler.INSTANCE.sendToServer(
					new PacketShoot(isADS, offset, x, y, z, yaw, pitch, hand == EnumHand.MAIN_HAND));

		} else {
			stopshoot = true;
		}
	}

	public void setMagazine(LoadedMagazine magazine) {
		this.magazine = magazine;
	}

	public void setGun(NBTTagCompound gunTag) {
		if (gunTag == null) {
			gun = null;
			return;
		}
		System.out.println(NBTEquals(gunTag));
		if (!NBTEquals(gunTag)) {
			GunData data = PackData.getGunData(gunTag.getString(HideNBT.DATA_NAME));

			gun = data == null ? null : () -> gunTag;
			updateCustomize();
		}
	}

	@Override
	public void tickUpdate() {
		if (!isGun())
			return;

		//LoadedMagazine mag = HideNBT.getGunLoadedMagazines(gun.getGunTag());
		//if (magazine.getList().size() != mag.getList().size())
		//	magazine = mag;

	}

}