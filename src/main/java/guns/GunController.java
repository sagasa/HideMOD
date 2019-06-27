package guns;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import entity.EntityBullet;
import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.SoundHandler;
import handler.client.RecoilHandler;
import helper.HideMath;
import helper.HideNBT;
import items.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketShoot;
import pack.PackData;
import types.base.GunFireMode;
import types.items.GunData;
import types.items.MagazineData;

/** 銃の制御系
 * TickUpdateを要求 */
public class GunController {

	private static final Logger LOGGER = LogManager.getLogger();
	/**ロード可能な弾薬のどれかをロードする*/
	public static final String LOAD_ANY = "ANY";

	private ShootPoints shootPoint = ShootPoints.DefaultShootPoint;
	private IGuns gun;
	private IMagazineHolder magazineHolder;
	private EnumHand hand;

	// ===============クライアント,サーバー共通部分==================
	private GunData modifyData = null;

	/** リロードの状況保存0でリロード完了処理 */
	private int reload = -1;

	public GunController(EnumHand hand) {
		this.hand = hand;
	}

	private boolean onClient = false;

	/** クライアント側で動作する場合NBTの書き込みを行わないモードに */
	public GunController setClientMode(boolean mode) {
		onClient = mode;
		return this;
	}

	/** カスタムとオリジナルから修正版のGunDataを作成
	 * カスタムパーツが見つからなければスキップ*/
	private void updateCustomize() {
		System.out.println("updateCastomize");
		if (isGun()) {
			modifyData = (GunData) gun.getGunData().clone();
			HideNBT.getGunAttachments(gun.getGunTag()).stream().map(str -> PackData.getAttachmentData(str))
					.filter(data -> data != null).forEach(part -> {
						modifyData.multiplyFloat(part.FLOAT_DIA_MAP);
						modifyData.addFloat(part.FLOAT_ADD_MAP);
						modifyData.setFloat(part.FLOAT_SET_MAP);
						modifyData.setString(part.STRING_SET_MAP);
					});
		}
	}

	/** ID保持 */
	private long uid = 0;

	public void setGun(IGuns gun, IMagazineHolder mag) {
		System.out.println("change " + HideNBT.getHideID(gun.getGunTag()));
		saveAndClear();
		this.gun = gun;
		this.magazineHolder = mag;
		uid = HideNBT.getHideID(gun.getGunTag());
		updateCustomize();
		magazine = HideNBT.getGunLoadedMagazines(gun.getGunTag());
		shootDelay = HideNBT.getGunShootDelay(gun.getGunTag());
		lastTime = lastShootTime = Minecraft.getSystemTime();
		System.out.println("load " + shootDelay);
	}

	public void saveAndClear() {
		// セーブ
		if (isGun() && !onClient) {
			HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
			int shootdelay = (int) MillistoTick(
					(int) (RPMtoMillis(modifyData.RPM) - (System.currentTimeMillis() - lastShootTime)));
			shootdelay = Math.max(0, shootdelay);
			HideNBT.setGunShootDelay(gun.getGunTag(), shootdelay);
			System.out.println("Save" + shootdelay);
		}
		//リコイル停止
		if (Shooter instanceof EntityPlayer && onClient) {
			RecoilHandler.clearRecoil(hand);
		}
		// 削除
		uid = 0;
		gun = null;
		modifyData = null;
		shootDelay = 0;
		magazine = new LoadedMagazine();
		amount = 0;
		completionTick = 0f;
		lastTime = 0;
		lastShootTime = 0;
		reload = -1;
	}

	public boolean isGun() {
		return gun != null && magazineHolder != null;
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
	private double oldX;
	private double oldY;
	private double oldZ;
	private float oldYaw;
	private float oldPitch;
	private Entity Shooter;

	/** 弾の出現点を設定 */
	public GunController setPos(double x, double y, double z) {
		System.out.println(x+" "+y+" "+z + " "+System.currentTimeMillis()+" "+hand);
		oldX = X;
		oldY = Y;
		oldZ = Z;
		X = x;
		Y = y;
		Z = z;
		return this;
	}

	/** 弾の向きを設定 */
	public GunController setRotate(float yaw, float pitch) {
		oldYaw = Yaw;
		oldPitch = Pitch;
		Yaw = yaw;
		Pitch = pitch;
		return this;
	}

	/** シューターを設定 */
	public GunController setShooter(Entity shooter) {
		Shooter = shooter;
		return this;
	}

	public boolean idEquals(long id) {
		return isGun() && uid == id;
	}

	public boolean stateEquals(GunController gun) {
		return this.modifyData.ITEM_SHORTNAME.equals(gun.modifyData.ITEM_SHORTNAME)
				&& this.getFireMode() == gun.getFireMode();
	}

	public GunData getGunData() {
		return modifyData;
	}

	public NBTTagCompound getGunTag() {
		return gun.getGunTag();
	}

	// 射撃アップデート

	/** NBT保存 */
	public LoadedMagazine magazine;

	private int amount = 0;

	public void tickUpdate(Side side) {
		if (!isGun())
			return;
		//	System.out.println("magazineState " + magazine);
		if (side == Side.CLIENT) {
			LoadedMagazine mag = HideNBT.getGunLoadedMagazines(gun.getGunTag());
			if (magazine.getList().size() != mag.getList().size())
				magazine = mag;
		} else {
			// リロードタイマー
			if (reload > 0) {
				reload--;
			} else if (reload == 0) {
				reload = -1;
				reload();
			}
			HideNBT.setGunShootDelay(gun.getGunTag(), shootDelay);
			HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
		}
	}

	private Float completionTick = null;

	/** 50Hzのアップデート処理プレイヤー以外はこのメゾットを使わない */
	@SideOnly(Side.CLIENT)
	public void gunUpdate(boolean trigger, float completion) {
		completionTick = completion;
		gunUpdate(trigger);
	}

	/** 保存時のShootDelay補完用 */
	private long lastShootTime = 0;
	private long lastTime = -1;

	/** NBT保存 */
	private int shootDelay = 0;
	private int shootNum = 0;
	private boolean stopshoot = false;

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
			boolean isADS = Shooter == null ? false : HideEntityDataManager.getADSState(Shooter) == 1;
			if (Shooter.world.isRemote) {
				// シューターがプレイヤー以外ならエラー
				if (!(Shooter instanceof EntityPlayer)) {
					LOGGER.error("cant shoot from other entity at client");
					return;
				}
				System.out.println("shoot comp = "+completionTick + "  offset = "+offset);
				if (completionTick != null) {
					offset += completionTick;
					completionTick = null;
				}
				//	RecoilHandler.addRecoil(modifyData, hand);
				double x = HideMath.completion(X, oldX, offset);
				double y = HideMath.completion(Y, oldY, offset);
				double z = HideMath.completion(Z, oldZ, offset);


				Shooter.world.spawnParticle(EnumParticleTypes.HEART, X, Y, Z, 0, 0, 0, 0);
				PacketHandler.INSTANCE.sendToServer(
						new PacketShoot(isADS, offset, x, y, z, Yaw, Pitch, HideNBT.getHideID(gun.getGunTag())));
			} else {
				shoot(modifyData, bullet, Shooter, isADS, offset, X, Y, Z, Yaw, Pitch);
			}
		} else {
			stopshoot = true;
		}
	}

	/** サーバーサイド */
	public void shoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch) {
		shoot(modifyData, magazine.getNextBullet(), Shooter, isADS, offset, x, y, z, yaw, pitch);
		reload = -1;
		lastShootTime = System.currentTimeMillis();
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(GunData gundata, MagazineData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		if (bulletdata != null && bulletdata.BULLETDATA != null) {
			SoundHandler.broadcastSound(shooter, 0, 0, 0, gundata.SOUND_SHOOT);
			for (int i = 0; i < bulletdata.BULLETDATA.SHOOT_NUM; i++) {
				EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, isADS, offset, x, y, z, yaw,
						pitch);
				shooter.world.spawnEntity(bullet);
			}
		}
	}

	/** RPMをミリ秒に変換 */
	private static int RPMtoMillis(float rPM) {
		return (int) (60000 / rPM);
	}

	/** ミリ秒をTickに変換 */
	private static float MillistoTick(int millis) {
		return millis / 50f;
	}

	/**
	 * リロード まだリロード処理が残ればtrue サーバーサイド
	 */

	/** リロードの必要があるかかチェック */
	public boolean needReload() {
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM) {
			return true;
		}
		Iterator<Magazine> itr = magazine.getList().iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			if (mag.num < PackData.getBulletData(mag.name).MAGAZINE_SIZE) {
				return true;
			}
		}
		return false;
	}

	/**オプションを考慮したマガジン排出処理*/
	private void exitMagazine(Magazine mag) {
		if (0 < mag.num || !PackData.getBulletData(mag.name).MAGAZINE_BREAK)
			magazineHolder.addMagazine(mag.name, mag.num);
	}

	/**
	 * プレリロード マガジンを外す
	 */
	public boolean preReload(int addReloadTime) {
		// ReloadAllの場合リロード可能なマガジンをすべて取り外す
		// ReloadAll以外+アンロードが許可されている+空スロットがない場合同じ種類で1番少ないマガジンを取り外す
		// リロードカウントを始める
		if (!isGun() || !needReload())
			return false;
		//リロード中なら
		if (reload != -1) {
			unload();
			return false;
		}

		//空のスロットがない+アンロードが許可されていないなら止める
		magazine.removeEmpty();
		if (magazine.getList().size() >= modifyData.LOAD_NUM && !modifyData.UNLOAD_IN_RELOADING) {
			return false;
		}
		//リロード開始
		// 音
		SoundHandler.broadcastSound(Shooter, X, Y, Z,
				gun.getGunData().SOUND_RELOAD);
		reload = modifyData.RELOAD_TICK + addReloadTime;

		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM && !modifyData.RELOAD_ALL) {
			return true;
		}

		MagazineData useMag = ItemMagazine.getMagazineData(getUseMagazine());
		float min = 1f;
		Magazine minMag = null;
		Iterator<Magazine> itr = magazine.getList().iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			//reloadAllなら問答無用で排出
			if (modifyData.RELOAD_ALL && mag.num < PackData.getBulletData(mag.name).MAGAZINE_SIZE) {
				exitMagazine(mag);
				itr.remove();
			} else {
				float dia = mag.num / PackData.getBulletData(mag.name).MAGAZINE_SIZE;
				if (dia < min) {
					min = dia;
					minMag = mag;
				}
			}
		}
		if (!modifyData.RELOAD_ALL && minMag != null) {
			magazine.getList().remove(minMag);
			exitMagazine(minMag);
		}
		return true;
	}

	private void reload() {
		if (magazine.getList().size() >= modifyData.LOAD_NUM)
			return;
		int count = magazineHolder.useMagazine(getUseMagazine());
		if (count > 0) {
			magazine.addMagazinetoLast(new Magazine(getUseMagazine(), count));
			// 全リロードの場合ループ
			if (modifyData.RELOAD_ALL)
				reload();
		}
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

	/**
	 * インベントリにマガジンを追加 入りきらない場合ドロップ ホットバーに追加しない
	 */
	private void addMagazineToInventory(String name, int num, Container inv, Entity entity) {
		// プレイヤーの場合はホットバーに入れないように

		for (int i = 0; i < inv.inventoryItemStacks.size(); i++) {
			ItemStack item = inv.inventoryItemStacks.get(i);
			if (ItemMagazine.isMagazine(item, name, num) && item.getCount() < item.getMaxStackSize()) {
				item.setCount(item.getCount() + 1);
				inv.detectAndSendChanges();
				return;
			}
		}
		int i = 0;
		// プレイヤーの場合ホットバーを除外
		if (entity instanceof EntityPlayer)
			i = 9;
		for (; i < inv.inventoryItemStacks.size(); i++) {
			ItemStack item = inv.inventoryItemStacks.get(i);
			if (item == ItemStack.EMPTY) {
				inv.inventoryItemStacks.set(i, ItemMagazine.makeMagazine(name, num));
				inv.detectAndSendChanges();
				return;
			}
		}
		entity.entityDropItem(ItemMagazine.makeMagazine(name, num), 1);
	}

	public void saveToNBT() {
		HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
	}

	/** 次の射撃モードを取得 */
	public GunFireMode getNextFireMode() {
		if (!isGun())
			return null;
		GunFireMode now = HideNBT.getGunFireMode(gun.getGunTag());
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
		String now = HideNBT.getGunUseingBullet(gun.getGunTag());
		List<String> modes = getUseMagazineList();
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
	public int getCanUseBulletNum() {
		return isGun() ? magazineHolder.getBulletCount() : 0;
	}

	public int getMaxBulletAmount() {
		return modifyData.LOAD_NUM * PackData.getBulletData(getUseMagazine()).MAGAZINE_SIZE;
	}

	public String getUseMagazine() {
		return HideNBT.getGunUseingBullet(gun.getGunTag());
	}

	public List<String> getUseMagazineList() {
		return Arrays.asList(modifyData.MAGAZINE_USE);
	}

	public GunFireMode getFireMode() {
		return HideNBT.getGunFireMode(gun.getGunTag());
	}

	public List<GunFireMode> getFireModeList() {
		return Arrays.asList(modifyData.FIREMODE).stream().map(str -> GunFireMode.getFireMode(str))
				.collect(Collectors.toList());
	}

	public void setGun(GunData gunData, Supplier<NBTTagCompound> gunTag, EntityPlayer player) {
		if (gunData != null)
			setGun(new IGuns() {
				@Override
				public NBTTagCompound getGunTag() {
					return gunTag.get();
				}

				@Override
				public GunData getGunData() {
					return gunData;
				}
			}, new IMagazineHolder() {
				@Override
				public int useMagazine(String name) {
					InventoryPlayer inv = player.inventory;
					int count = 0;
					int index = -1;
					for (int i = 0; i < inv.mainInventory.size(); i++) {
						ItemStack item = inv.mainInventory.get(i);
						if (ItemMagazine.isMagazine(item, name)) {
							int bulletNum = HideNBT.getMagazineBulletNum(item);
							//	System.out.println("find mag " + item + " " + bulletNum);
							if (bulletNum > count) {
								index = i;
								count = bulletNum;
								if (bulletNum >= PackData.getBulletData(name).MAGAZINE_SIZE)
									break;
							}
						}
					}
					if (count > 0) {
						ItemStack item = inv.mainInventory.get(index);
						if (item.getCount() > 1)
							item.setCount(item.getCount() - 1);
						else
							inv.mainInventory.set(index, ItemStack.EMPTY);
					}
					return count;
				}

				@Override
				public int getBulletCount() {
					int num = 0;
					String bulletName = HideNBT.getGunUseingBullet(gun.getGunTag());
					for (ItemStack item : player.inventory.mainInventory) {
						if (ItemMagazine.isMagazine(item, bulletName)) {
							num += HideNBT.getMagazineBulletNum(item) * item.getCount();
						}
					}
					return num;
				}

				@Override
				public void addMagazine(String name, int amount) {
					if (!player.inventory.addItemStackToInventory(ItemMagazine.makeMagazine(name, amount)))
						player.dropItem(ItemMagazine.makeMagazine(name, amount), true);
				}
			});
		else
			saveAndClear();
	}

	/**銃の元にできるインターフェース*/
	public interface IGuns {
		public GunData getGunData();

		public NBTTagCompound getGunTag();
	}

	public interface IMagazineHolder {
		/**指定された弾の数*/
		public int getBulletCount();

		/**指定されたマガジンの中で1番残弾が多いものを消費してその残弾数を返す*/
		public int useMagazine(String name);

		public void addMagazine(String name, int amount);
	}
}

//=================== Mobから利用するためのスタティックなコントローラー ========================= //TODO
