package gamedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import entity.EntityBullet;
import gamedata.LoadedMagazine.Magazine;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.SoundHandler;
import handler.client.RecoilHandler;
import helper.NBTWrapper;
import hideMod.PackData;
import item.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketShoot;
import types.attachments.GunCustomizePart;
import types.base.GunFireMode;
import types.items.GunData;
import types.items.MagazineData;

/** 銃の制御系 */
public class Gun {

	private static final Logger LOGGER = LogManager.getLogger();

	// ===============クライアント,サーバー共通部分==================
	private GunData originalData = null;
	private List<GunCustomizePart> customize = new ArrayList<>();
	private GunData modifyData = null;

	private Supplier<NBTTagCompound> gunTag;

	public Gun() {

	}

	public Gun(GunData data, Supplier<NBTTagCompound> guntag) {
		setGun(data, guntag);
	}

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
		magazine = NBTWrapper.getGunLoadedMagazines(gunTag.get());
		shootDelay = NBTWrapper.getGunShootDelay(gunTag.get());
	}

	public void clearGun() {
		originalData = null;
		customize.clear();
		modifyData = null;
		shootDelay = 0;
		magazine = new LoadedMagazine();
	}

	public boolean isGun() {
		return originalData != null;
	}

	/** このTickで射撃可能かどうか */
	public boolean canShoot() {
		if (magazine.getLoadedNum() > 0 && !stopshoot && shootDelay <= 0 & shootNum <= 0) {
			return true;
		}
		return false;
	}

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

	public boolean idEquals(long id) {
		return NBTWrapper.getHideID(gunTag.get()) == id;
	}

	public boolean stateEquals(Gun gun) {
		return this.modifyData.ITEM_SHORTNAME.equals(gun.modifyData.ITEM_SHORTNAME)
				&& this.getFireMode() == gun.getFireMode();
	}

	public GunFireMode getFireMode() {
		return NBTWrapper.getGunFireMode(gunTag.get());
	}

	public GunData getGunData() {
		return modifyData;
	}

	public NBTTagCompound getGunTag() {
		return gunTag.get();
	}

	public String getGunUseingBullet() {
		return NBTWrapper.getGunUseingBullet(gunTag.get());
	}

	// 射撃アップデート

	/** NBT保存 */
	public LoadedMagazine magazine;

	private int amount = 0;

	/** tickイベントからのアップデート */
	public void tickUpdate(Side side) {
		if (!isGun())
			return;
		if (side == Side.CLIENT) {
			// magazine
			LoadedMagazine now = NBTWrapper.getGunLoadedMagazines(gunTag.get());
			// リロード検知
			if (now.getLoadedNum() > amount) {
				magazine = now;
				// System.out.println("Magazine更新");
			}
			amount = now.getLoadedNum();
		} else {
			NBTWrapper.setGunShootDelay(gunTag.get(), shootDelay);
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
		}
	}

	private Float completionTick = null;

	/** 銃のアップデート処理プレイヤー以外はこのメゾットを使わない */
	@SideOnly(Side.CLIENT)
	public void gunUpdate(boolean trigger, float completion) {
		completionTick = completion;
		gunUpdate(trigger);
	}

	/** NBT保存 */
	private int shootDelay = 0;
	private int shootNum = 0;
	private boolean stopshoot = false;
	private long lastTime = -1;

	/** 銃のアップデート処理 トリガー関連 */
	public void gunUpdate(boolean trigger) {
		if (!isGun())
			return;

		if (lastTime == -1)
			lastTime = Minecraft.getSystemTime();
		if (0 < shootDelay)
			shootDelay -= Minecraft.getSystemTime() - lastTime;
		lastTime = Minecraft.getSystemTime();
		if (!trigger)
			stopshoot = false;

		if (getFireMode() == GunFireMode.SEMIAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			if (shootDelay < 0) {
				shootDelay = 0;
			}
			shoot(MillistoTick(shootDelay));
			shootDelay += RPMtoMillis(modifyData.RPM);
			stopshoot = true;
		} else if (getFireMode() == GunFireMode.FULLAUTO && !stopshoot && shootDelay <= 0 && trigger) {
			while (shootDelay <= 0 && !stopshoot) {
				shoot(MillistoTick(shootDelay));
				shootDelay += RPMtoMillis(modifyData.RPM);
			}
		} else if (getFireMode() == GunFireMode.BURST && !stopshoot) {
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

		} else if (getFireMode() == GunFireMode.MINIGUN && !stopshoot && shootDelay <= 0 && trigger) {
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
			boolean isADS = HideEntityDataManager.getADSState(Shooter) == 1;
			if (Shooter.world.isRemote) {
				// シューターがプレイヤー以外ならエラー
				if (!(Shooter instanceof EntityPlayer)) {
					LOGGER.error("cant shoot from other entity at client");
					return;
				}
				RecoilHandler.addRecoil(modifyData);
				if (completionTick != null) {
					offset += completionTick;
					completionTick = null;
				}
				PacketHandler.INSTANCE.sendToServer(
						new PacketShoot(isADS, offset, X, Y, Z, Yaw, Pitch, NBTWrapper.getHideID(gunTag.get())));
			} else {
				shoot(modifyData, bullet, Shooter, isADS, offset, X, Y, Z, Yaw, Pitch);
			}
		} else {
			stopshoot = true;
		}
	}

	/** サーバーサイドで射撃パケットからの射撃処理 */
	public static void shoot(EntityPlayer player, long uid, float offset, boolean isADS, double x, double y, double z,
			float yaw, float pitch) {

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
		return millis / 50f;
	}

	/**
	 * リロード まだリロード処理が残ればtrue サーバーサイド
	 */
	public boolean reload(EntityPlayer player, boolean isexit) {
		if (!isGun())
			return false;
		// 排出
		if (isexit) {
			for (Magazine magazine : magazine.getList()) {
				if (PackData.getBulletData(magazine.name) != null) {
					player.addItemStackToInventory(ItemMagazine.makeMagazine(magazine.name, magazine.num));
				}
			}
			magazine.getList().clear();
		}
		// System.out.println("start" + magazines.getList());
		if (modifyData.RELOAD_ALL) {
			while (reload(player))
				;
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
			return false;
		} else {
			reload(player);
			// System.out.println("end" + magazines.getList());
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
			return reload(player);
		}
	}

	/**
	 * リロード処理 何かしたらtrue サーバーサイド
	 */
	private boolean reload(EntityPlayer player) {
		String magName = NBTWrapper.getGunUseingBullet(gunTag.get());
		int magSize = ItemMagazine.getBulletData(magName).MAGAZINE_SIZE;
		if (magazine.getList().size() < modifyData.LOAD_NUM) {
			int n = getMag(magName, magSize, player, ItemMagazine.getBulletData(magName).MAGAZINE_BREAK);
			if (n == 0) {
				return false;
			}
			magazine.getList().add(0, magazine.new Magazine(magName, n));
			return true;
		}
		for (Magazine mag : magazine.getList()) {
			if (mag.name.equals(magName) && mag.num < magSize) {
				int n = getMag(magName, magSize - mag.num, player, ItemMagazine.getBulletData(magName).MAGAZINE_BREAK);
				if (n == 0) {
					return false;
				}
				mag.num += n;
				return true;
			}
		}
		return false;
	}

	/** インベントリから指定の弾を回収 取得した数を返す */
	private int getMag(String name, int value, EntityPlayer player, boolean isBreak) {
		int c = value;
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, name)) {
				int n = NBTWrapper.getMagazineBulletNum(gunTag.get());
				if (n <= c) {
					if (item.getCount() > 0) {
						c -= n;
						item.setCount(item.getCount() - 1);
						if (!isBreak) {
							player.addItemStackToInventory(ItemMagazine.makeMagazine(name, 0));
						}
					}
					if (c == 0) {
						return value;
					}
				} else if (c < n) {
					item.setCount(item.getCount() - 1);
					player.addItemStackToInventory(ItemMagazine.makeMagazine(name, n - c));
					return value;
				}
			}
		}
		return value - c;

	}

	/** 次の射撃モードを取得 */
	public GunFireMode getNextFireMode() {
		if (!isGun())
			return null;
		GunFireMode now = NBTWrapper.getGunFireMode(gunTag.get());
		List<String> modes = Arrays.asList(modifyData.FIREMODE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		return GunFireMode.getFireMode(modes.get(index));
	}

	/** 次の使用する弾を取得 */
	public String getNextUseMagazine() {
		if (!isGun())
			return null;
		String now = NBTWrapper.getGunUseingBullet(gunTag.get());
		List<String> modes = Arrays.asList(modifyData.MAGAZINE_USE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		if (!ItemMagazine.isMagazineExist(modes.get(index))) {
			return now;
		}
		return modes.get(index);
	}

	/** 銃で使用中の弾薬の所持数を返す */
	public int getCanUseBulletNum(EntityPlayer player) {
		int num = 0;
		String bulletName = NBTWrapper.getGunUseingBullet(gunTag.get());
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, bulletName)) {
				num += NBTWrapper.getMagazineBulletNum(item) * item.getCount();
			}
		}
		return num;
	}
}
