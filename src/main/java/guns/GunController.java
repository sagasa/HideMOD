package guns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import entity.EntityBullet;
import gamedata.HidePlayerData;
import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import handler.HideEntityDataManager;
import handler.PacketHandler;
import handler.SoundHandler;
import handler.client.RecoilHandler;
import helper.HideNBT;
import items.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
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

	public ShootPoints shootPoint = ShootPoints.DefaultShootPoint;
	public IGuns gun;
	public EnumHand hand;

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

	public void setGun(IGuns gun) {
		System.out.println("change " + HideNBT.getHideID(gun.getGunTag()));
		saveAndClear();
		this.gun = gun;
		uid = HideNBT.getHideID(gun.getGunTag());
		updateCustomize();
		magazine = HideNBT.getGunLoadedMagazines(gun.getGunTag());
		shootDelay = HideNBT.getGunShootDelay(gun.getGunTag());
	}

	public void saveAndClear() {
		// セーブ
		if (isGun() && !onClient) {
			HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
			int shootdelay = (int) MillistoTick(
					(int) (RPMtoMillis(modifyData.RPM) - (Minecraft.getSystemTime() - lastShootTime)));
			shootdelay = Math.max(0, shootdelay);
			HideNBT.setGunShootDelay(gun.getGunTag(), shootdelay);
			System.out.println(shootdelay);
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
	}

	public boolean isGun() {
		return gun != null;
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
	public GunController setPos(double x, double y, double z) {
		X = x;
		Y = y;
		Z = z;
		return this;
	}

	/** 弾の向きを設定 */
	public GunController setRotate(float yaw, float pitch) {
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
		if (side == Side.CLIENT) {
			// magazine
			LoadedMagazine now = HideNBT.getGunLoadedMagazines(gun.getGunTag());
			// リロード検知
			if (now.getLoadedNum() > amount) {
				magazine = now;
				System.out.println("Magazine更新");
			}
			amount = now.getLoadedNum();
		} else {
			// リロードタイマー
			if (reload > 0) {
				reload--;
			} else if (reload == 0) {
				reload = -1;

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
			boolean isADS = Shooter == null ? false : HideEntityDataManager.getADSState(Shooter) == 1;
			if (Shooter.world.isRemote) {
				// シューターがプレイヤー以外ならエラー
				if (!(Shooter instanceof EntityPlayer)) {
					LOGGER.error("cant shoot from other entity at client");
					return;
				}
				RecoilHandler.addRecoil(modifyData, hand);
				if (completionTick != null) {
					offset += completionTick;
					completionTick = null;
				}
				PacketHandler.INSTANCE.sendToServer(
						new PacketShoot(isADS, offset, X, Y, Z, Yaw, Pitch, HideNBT.getHideID(gun.getGunTag())));
			} else {
				shoot(modifyData, bullet, Shooter, isADS, offset, X, Y, Z, Yaw, Pitch);
			}
		} else {
			stopshoot = true;
		}
	}

	/** サーバーサイドでパケットからの射撃処理 */
	public static void shoot(EntityPlayer player, long uid, float offset, boolean isADS, double x, double y, double z,
			float yaw, float pitch) {
		GunController gun = HidePlayerData.getServerData(player).getGun(uid);
		if (gun == null) {
			LOGGER.warn("cant make bullet by cant find gun: player = " + player.getName());
		}
		gun.setPos(x, y, z);
		gun.setRotate(yaw, pitch);
		gun.setShooter(player);
		gun.shoot(isADS, offset);
		gun.magazine.useNextBullet();
		// System.out.println("offset at shoot" + offset);
	}

	/** サーバーサイド */
	public void shoot(boolean isADS, float offset) {
		shoot(modifyData, magazine.getNextBullet(), Shooter, isADS, offset, X, Y, Z, Yaw, Pitch);
		lastShootTime = Minecraft.getSystemTime();
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(GunData gundata, MagazineData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
		if (bulletdata != null && bulletdata.BULLETDATA != null) {
			SoundHandler.broadcastSound(shooter.world, x, y, z, gundata.SOUND_SHOOT);
			for (int i = 0; i < bulletdata.BULLETDATA.SHOOT_NUM; i++) {
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

	/** 弾排出 */
	public void unload(EntityPlayer player) {
		for (Magazine magazine : magazine.getList()) {
			System.out.println("reload exit " + magazine);
			if (PackData.getBulletData(magazine.name) != null) {
				player.addItemStackToInventory(ItemMagazine.makeMagazine(magazine.name, magazine.num));
			}
		}
		magazine.getList().clear();
	}

	/**
	 * リロード まだリロード処理が残ればtrue サーバーサイド
	 */
	public void reload() {

		if (!isGun())
			return;
		System.out.println("reloadReq ");
		// 空きがなければ停止
		if (magazine.getList().size() >= modifyData.LOAD_NUM) {
			return;
		}

		System.out.println("SAVE");
		HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
	}

	/** リロードの必要があるかかチェック */
	public boolean needReload() {
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM) {
			return true;
		}

		MagazineData magData = ItemMagazine.getMagazineData(getUseMagazine());
		for (int i = 0; i < magazine.getList().size(); i++) {
			Magazine mag = magazine.getList().get(i);
			// リロード可能なマガジンなら
			if ((mag.num < magData.MAGAZINE_SIZE && mag.name.equals(getUseMagazine())) || mag.num == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * プレリロード マガジンを外す
	 */
	public boolean preReload(Entity e, Container inv, int addReloadTime) {
		// ReloadAllの場合リロード可能なマガジンをすべて取り外す
		// ReloadAll以外ので空スロットがない場合同じ種類で1番少ないマガジンを取り外す
		// リロードカウントを始める
		if (!isGun() || !needReload())
			return false;
		System.out.println("unload ");
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM) {
			if (!modifyData.RELOAD_ALL) {
				return false;
			}
		}
		MagazineData magData = ItemMagazine.getMagazineData(getUseMagazine());
		int c = magData.MAGAZINE_SIZE;
		int index = -1;
		List<Magazine> list = new ArrayList<>();
		for (int i = 0; i < magazine.getList().size(); i++) {
			Magazine mag = magazine.getList().get(i);
			// リロード可能なマガジンなら
			if ((mag.num < magData.MAGAZINE_SIZE && mag.name.equals(getUseMagazine())) || mag.num == 0) {
				if (modifyData.RELOAD_ALL) {
					list.add(mag);
				} else if (mag.num < c) {
					c = mag.num;
					index = i;
				}
			}
		}

		if (modifyData.RELOAD_ALL) {
			list.forEach(mag -> {
				if (!PackData.getBulletData(mag.name).MAGAZINE_BREAK || mag.num > 0)
					;
			});
			magazine.getList().removeAll(list);
		} else if (index != -1) {
			Magazine mag = magazine.getList().get(index);
			// player.addItemStackToInventory(ItemMagazine.makeMagazine(mag.name, mag.num));
			magazine.getList().remove(index);
		}
		return true;
	}

	public boolean reload(Container container) {
		ItemStack maxitem = getMagazine(container);
		if (maxitem != null) {
			Magazine mag = magazine.new Magazine(getUseMagazine(), HideNBT.getMagazineBulletNum(maxitem));
			magazine.addMagazinetoLast(mag);
			maxitem.setCount(maxitem.getCount() - 1);
			//	System.out.println("add  " + mag);

			// 全リロードの場合ループ
			if (modifyData.RELOAD_ALL)
				return reload(container);

			System.out.println("SAVE");
			HideNBT.setGunLoadedMagazines(gun.getGunTag(), magazine);
		}
		return false;
	}

	/**コンテナから利用するアイテムスタックを取得*/
	private ItemStack getMagazine(Container container) {
		//リロード可能かの確認
		if (magazine.getList().size() >= modifyData.LOAD_NUM) {
			return null;
		}
		String name = getUseMagazine();
		int c = 0;
		ItemStack maxitem = null;
		for (ItemStack item : container.inventoryItemStacks) {
			if (ItemMagazine.isMagazine(item, name)) {
				int bulletNum = HideNBT.getMagazineBulletNum(item);
				//	System.out.println("find mag " + item + " " + bulletNum);
				if (bulletNum > c) {
					maxitem = item;
					c = bulletNum;
					if (bulletNum >= PackData.getBulletData(name).MAGAZINE_SIZE)
						break;
				}
			}
		}
		return maxitem;
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
	public int getCanUseBulletNum(EntityPlayer player) {
		int num = 0;
		String bulletName = HideNBT.getGunUseingBullet(gun.getGunTag());
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, bulletName)) {
				num += HideNBT.getMagazineBulletNum(item) * item.getCount();
			}
		}
		return num;
	}

	public int getBulletAmount() {
		int num = 0;
		for (Magazine mag : magazine.getList()) {
			num += mag.num;
		}
		return num;
	}

	public int getMaxBulletAmount() {
		return modifyData.LOAD_NUM * PackData.getBulletData(getUseMagazine()).MAGAZINE_SIZE;
	}

	public String getUseMagazine() {
		return HideNBT.getGunUseingBullet(gun.getGunTag());
	}

	public boolean setUseMagazine(String name) {
		if (!getUseMagazineList().contains(name))
			return false;
		HideNBT.setGunUseingBullet(gun.getGunTag(), name);
		return true;
	}

	public List<String> getUseMagazineList() {
		return Arrays.asList(modifyData.MAGAZINE_USE);
	}

	public GunFireMode getFireMode() {
		return HideNBT.getGunFireMode(gun.getGunTag());
	}

	public boolean setFireMode(GunFireMode firemode) {
		HideNBT.getGunFireMode(gun.getGunTag());
		return true;
	}

	public List<GunFireMode> getFireModeList() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	public void trigger(int time) {
		// TODO 自動生成されたメソッド・スタブ

	}

	public void setGun(GunData gunData, Supplier<NBTTagCompound> gunTag) {
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
			});
		else
			saveAndClear();
	}
}
