package guns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import handler.PlayerHandler;
import handler.SoundHandler;
import handler.client.RecoilHandler;
import helper.NBTWrapper;
import hideMod.PackData;
import item.ItemMagazine;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemAir;
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
public class Gun implements IGuns {

	private static final Logger LOGGER = LogManager.getLogger();

	public ShootPoints shootPoint = ShootPoints.DefaultShootPoint;
	// ===============クライアント,サーバー共通部分==================
	private GunData originalData = null;
	private List<GunCustomizePart> customize = new ArrayList<>();
	private GunData modifyData = null;

	private Supplier<NBTTagCompound> gunTag = null;

	/** リロードの状況保存0でリロード完了処理 */
	private int reload = -1;

	public Gun() {

	}

	public Gun(GunData data, Supplier<NBTTagCompound> guntag) {
		setGun(data, guntag);
	}

	private boolean onClient = false;

	/** クライアント側で動作する場合NBTの書き込みを行わないモードに */
	public Gun setClientMode(boolean mode) {
		onClient = mode;
		return this;
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

	/** ID保持 */
	private long uid = 0;

	public void setGun(GunData data, Supplier<NBTTagCompound> guntag) {
		System.out.println("change " + NBTWrapper.getHideID(guntag.get()));
		saveAndClear();
		originalData = data;
		gunTag = guntag;
		uid = NBTWrapper.getHideID(gunTag.get());
		updateCustomize();
		magazine = NBTWrapper.getGunLoadedMagazines(gunTag.get());
		shootDelay = NBTWrapper.getGunShootDelay(gunTag.get());
	}

	public void saveAndClear() {
		// セーブ
		if (isGun() && !onClient) {
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
			int shootdelay = (int) MillistoTick(
					(int) (RPMtoMillis(modifyData.RPM) - (Minecraft.getSystemTime() - lastShootTime)));
			shootdelay = Math.max(0, shootdelay);
			NBTWrapper.setGunShootDelay(gunTag.get(), shootdelay);
			System.out.println(shootdelay);
		}
		// 削除
		uid = 0;
		gunTag = null;
		originalData = null;
		customize.clear();
		modifyData = null;
		shootDelay = 0;
		magazine = new LoadedMagazine();
		amount = 0;
		completionTick = 0f;
		lastTime = 0;
		lastShootTime = 0;
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

	/** シューターを設定 */
	public Gun setShooter(Entity shooter) {
		Shooter = shooter;
		return this;
	}

	public boolean idEquals(long id) {
		return isGun() && uid == id;
	}

	public boolean stateEquals(Gun gun) {
		return this.modifyData.ITEM_SHORTNAME.equals(gun.modifyData.ITEM_SHORTNAME)
				&& this.getFireMode() == gun.getFireMode();
	}

	public GunData getGunData() {
		return modifyData;
	}

	public NBTTagCompound getGunTag() {
		return gunTag.get();
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
				System.out.println("Magazine更新");
			}
			amount = now.getLoadedNum();
		} else {
			NBTWrapper.setGunShootDelay(gunTag.get(), shootDelay);
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
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

	/** サーバーサイドでパケットからの射撃処理 */
	public static void shoot(EntityPlayer player, long uid, float offset, boolean isADS, double x, double y, double z,
			float yaw, float pitch) {
		Gun gun = HidePlayerData.getServerData(player).getGun(uid);
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
		if (bulletdata.BULLETDATA != null) {
			for (int i = 0; i < bulletdata.BULLETDATA.SHOOT_NUM; i++) {
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
	public void reload(EntityPlayer player) {
		if (!isGun())
			return;
		System.out.println("reloadReq ");
		// 空きがなければ停止
		if (magazine.getList().size() >= modifyData.LOAD_NUM) {
			return;
		}

		String name = getUseMagazine();
		int c = 0;
		ItemStack maxitem = null;
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, name)) {
				int bulletNum = NBTWrapper.getMagazineBulletNum(item);
				System.out.println("find mag " + item + " " + bulletNum);
				if (bulletNum > c) {
					maxitem = item;
					c = bulletNum;
					if (bulletNum >= PackData.getBulletData(name).MAGAZINE_SIZE)
						break;
				}
			}
		}
		if (maxitem != null) {
			Magazine mag = magazine.new Magazine(name, NBTWrapper.getMagazineBulletNum(maxitem));
			magazine.addMagazinetoLast(mag);
			maxitem.setCount(maxitem.getCount() - 1);
			System.out.println("add  " + mag);

			// 全リロードの場合ループ
			if (modifyData.RELOAD_ALL)
				reload(player);

			System.out.println("SAVE");
			NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
		}
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
	public void reloadReq(Entity e, Container inv, int addReloadTime) {
		// ReloadAllの場合リロード可能なマガジンをすべて取り外す
		// ReloadAll以外ので空スロットがない場合同じ種類で1番少ないマガジンを取り外す
		// リロードカウントを始める
		if (!isGun() || !needReload())
			return;
		System.out.println("unload ");
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM) {
			if (!modifyData.RELOAD_ALL) {
				return;
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
		NBTWrapper.setGunLoadedMagazines(gunTag.get(), magazine);
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

	@Override
	public int getBulletAmount() {
		int num = 0;
		for (Magazine mag : magazine.getList()) {
			num += mag.num;
		}
		return num;
	}

	@Override
	public int getMaxBulletAmount() {
		return modifyData.LOAD_NUM * PackData.getBulletData(getUseMagazine()).MAGAZINE_SIZE;
	}

	@Override
	public String getUseMagazine() {
		return NBTWrapper.getGunUseingBullet(gunTag.get());
	}

	@Override
	public boolean setUseMagazine(String name) {
		boolean f = false;
		for (String str : modifyData.MAGAZINE_USE) {
			if (str.equals(name))
				f = true;
		}
		if (!f)
			return false;
		NBTWrapper.setGunUseingBullet(gunTag.get(), name);
		return true;
	}

	@Override
	public List<String> getUseMagazineList() {
		List<String> list = new ArrayList<>();
		for (String str : modifyData.MAGAZINE_USE)
			list.add(str);
		return list;
	}

	@Override
	public GunFireMode getFireMode() {
		return NBTWrapper.getGunFireMode(gunTag.get());
	}

	@Override
	public boolean setFireMode(GunFireMode firemode) {
		NBTWrapper.getGunFireMode(gunTag.get());
		return true;
	}

	@Override
	public List<GunFireMode> getFireModeList() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public boolean reload(Container container) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public void trigger(int time) {
		// TODO 自動生成されたメソッド・スタブ

	}
}
