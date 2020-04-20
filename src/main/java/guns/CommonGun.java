package guns;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import entity.EntityBullet;
import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import handler.SoundHandler;
import helper.HideAdder;
import helper.HideNBT;
import items.ItemMagazine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import pack.PackData;
import types.base.GunFireMode;
import types.items.GunData;
import types.items.MagazineData;

/** 銃の制御系
 * TickUpdateを要求
 * Playerに紐付けで動作 */
public abstract class CommonGun {

	protected static final Logger log = LogManager.getLogger();
	/**ロード可能な弾薬のどれかをロードする*/
	public static final String LOAD_ANY = "ANY";

	public static final byte SOUND_RELOAD = HideAdder.SoundCate.getNumber();

	protected EntityPlayer owner;
	protected ShootPoints shootPoint = ShootPoints.DefaultShootPoint;
	protected IGuns gun;
	protected IMagazineHolder magazineHolder;
	protected EnumHand hand;

	// ===============クライアント,サーバー共通部分==================
	protected GunData modifyData = null;

	/** リロードの状況保存0でリロード完了処理 */
	protected int reloadProgress = -1;

	public CommonGun(EnumHand hand) {
		this.hand = hand;
	}

	/** カスタムとオリジナルから修正版のGunDataを作成
	 * カスタムパーツが見つからなければスキップ*/
	protected void updateCustomize() {
		System.out.println("updateCastomize");
		if (isGun()) {
			modifyData = (GunData) PackData.getGunData(gun.getGunTag().getString(HideNBT.DATA_NAME)).clone();
			HideNBT.getGunAttachments(gun.getGunTag()).stream().map(str -> PackData.getAttachmentData(str))
					.filter(data -> data != null).forEach(part -> {
						modifyData.applyChange(part.CHANGE_LIST);
					});
		}
	}

	public void setMagHolder(IMagazineHolder mag) {
		magazineHolder = mag;
	}

	public boolean isGun() {
		return gun != null && magazineHolder != null;
	}

	/**GunDataの更新が必要か*/
	public boolean NBTEquals(NBTTagCompound hideTag) {
		return HideNBT.getGunAttachments(gun.getGunTag()).equals(HideNBT.getGunAttachments(hideTag)) &&
				HideNBT.getTag(gun.getGunTag(), HideNBT.DATA_NAME).equals(HideNBT.getTag(hideTag, HideNBT.DATA_NAME));
	}

	public boolean stateEquals(CommonGun other) {
		return this.modifyData.ITEM_SHORTNAME.equals(other.modifyData.ITEM_SHORTNAME)
				&& this.getFireMode() == other.getFireMode();
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

	public abstract void tickUpdate();

	/** 保存時のShootDelay補完用 */
	protected long lastShootTime = 0;

	/** サーバーサイド */
	public void shoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch) {
		shoot(modifyData, magazine.getNextBullet(), owner, isADS, offset, x, y, z, yaw, pitch);
		stopReload();
		lastShootTime = System.currentTimeMillis();
	}

	/** エンティティを生成 ShootNumに応じた数弾を出す */
	private static void shoot(GunData gundata, MagazineData bulletdata, Entity shooter, boolean isADS, float offset,
			double x, double y, double z, float yaw, float pitch) {
		if (bulletdata != null && bulletdata.BULLETDATA != null) {
			SoundHandler.broadcastSound(shooter, 0, 0, 0, gundata.SOUND_SHOOT, true);
			for (int i = 0; i < bulletdata.BULLETDATA.SHOOT_NUM; i++) {
				EntityBullet bullet = new EntityBullet(gundata, bulletdata, shooter, isADS, offset, x, y, z, yaw,
						pitch);
				shooter.world.spawnEntity(bullet);
			}
		}
	}

	/** RPMをミリ秒に変換 */
	protected int RPMtoMillis(float rPM) {
		return (int) (60000 / rPM);
	}

	/** ミリ秒をTickに変換 */
	protected float MillistoTick(int millis) {
		return millis / 50f;
	}

	protected void stopReload() {
		if (reloadProgress != -1) {
			reloadProgress = -1;
			SoundHandler.bloadcastCancel(owner.world, owner.getEntityId(), SOUND_RELOAD);
		}
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
			MagazineData magData = PackData.getBulletData(mag.name);
			if (magData == null || mag.num < magData.MAGAZINE_SIZE) {
				return true;
			}
		}
		return false;
	}

	/**オプションを考慮したマガジン排出処理*/
	private void exitMagazine(Magazine mag) {
		MagazineData magData = PackData.getBulletData(mag.name);
		if (magData != null && (0 < mag.num || !magData.MAGAZINE_BREAK))
			magazineHolder.addMagazine(mag.name, mag.num);
	}

	private int prevAddReloadTime = 0;

	/**
	 * プレリロード マガジンを外す
	 */
	public boolean preReload(int addReloadTime) {
		// ReloadAllの場合リロード可能なマガジンをすべて取り外す
		// ReloadAll以外+アンロードが許可されている+空スロットがない場合同じ種類で1番少ないマガジンを取り外す
		// リロードカウントを始める
		if (!isGun() || !needReload())
			return false;
		//リロード中ならdsq
		if (reloadProgress != -1) {
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
		SoundHandler.broadcastSound(owner, 0, 0, 0,
				modifyData.SOUND_RELOAD, false, SOUND_RELOAD);
		reloadProgress = modifyData.RELOAD_TICK + addReloadTime;
		//複数回リロード用に追加時間を保存
		prevAddReloadTime = addReloadTime;
		// ReloadAll以外で空きスロットがある場合何もしない
		if (magazine.getList().size() < modifyData.LOAD_NUM && !modifyData.RELOAD_ALL) {
			return true;
		}
		//最小のマガジンを検出
		float min = 1f;
		Magazine minMag = null;
		Iterator<Magazine> itr = magazine.getList().iterator();
		while (itr.hasNext()) {
			Magazine mag = itr.next();
			MagazineData magData = PackData.getBulletData(mag.name);
			//存在しないマガジンなら排出
			if (magData == null) {
				itr.remove();
			}
			//reloadAllなら問答無用で排出
			else if (modifyData.RELOAD_ALL && mag.num < magData.MAGAZINE_SIZE) {
				exitMagazine(mag);
				itr.remove();
			} else {
				float dia = mag.num / magData.MAGAZINE_SIZE;
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

	protected void reload() {
		if (magazine.getList().size() >= modifyData.LOAD_NUM) {
			log.info(magazine.getList());
			log.info("reload stop! magazine is full");
			return;
		}
		Magazine mag = magazineHolder.useMagazine(getUseMagazines());
		if (mag.num > 0) {
			magazine.addMagazinetoLast(mag);
			// 全リロードの場合ループ
			if (modifyData.RELOAD_ALL)
				reload();
			else
				preReload(prevAddReloadTime);
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
		List<String> modes = Arrays.asList(getUseMagazineList());
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
		return isGun() ? magazineHolder.getBulletCount(getUseMagazines()) : 0;
	}

	/**利用可能なすべてのマガジンの中で最大の保持できる弾薬数*/
	public int getMaxBulletAmount() {
		return modifyData.LOAD_NUM * Arrays.stream(getUseMagazines())
				.map(str -> PackData.getBulletData(str).MAGAZINE_SIZE).max(Comparator.naturalOrder()).get();
	}

	/**利用可能なすべてのマガジンを返す NBTの文字列じゃないことに注意*/
	public String[] getUseMagazines() {
		String name = HideNBT.getGunUseingBullet(gun.getGunTag());
		if (name == null)
			return ArrayUtils.EMPTY_STRING_ARRAY;
		return name.equals(LOAD_ANY) ? getUseMagazineList() : new String[] { name };
	}

	public String getUseMagazine() {
		return HideNBT.getGunUseingBullet(gun.getGunTag());
	}

	public String[] getUseMagazineList() {
		return modifyData.MAGAZINE_USE;
	}

	public GunFireMode getFireMode() {
		return HideNBT.getGunFireMode(gun.getGunTag());
	}

	public List<GunFireMode> getFireModeList() {
		return Arrays.asList(modifyData.FIREMODE).stream().map(str -> GunFireMode.getFireMode(str))
				.collect(Collectors.toList());
	}

	public void setGun(GunData gunData, Supplier<NBTTagCompound> gunTag, EntityPlayer player) {

		magazineHolder = new IMagazineHolder() {
			@Override
			public Magazine useMagazine(String... name) {
				InventoryPlayer inv = player.inventory;
				Magazine mag = new Magazine(null, 0);
				int index = -1;
				for (int i = 0; i < inv.mainInventory.size(); i++) {
					ItemStack item = inv.mainInventory.get(i);
					//
					for (String str : name)
						if (ItemMagazine.isMagazine(item, str)) {
							int bulletNum = HideNBT.getMagazineBulletNum(item);
							//	System.out.println("find mag " + item + " " + bulletNum);
							if (bulletNum > mag.num) {
								index = i;
								mag.num = bulletNum;
								mag.name = str;
							}
							break;
						}
				}
				if (mag.num > 0) {
					ItemStack item = inv.mainInventory.get(index);
					if (item.getCount() > 1)
						item.setCount(item.getCount() - 1);
					else
						inv.mainInventory.set(index, ItemStack.EMPTY);
				}
				return mag;
			}

			@Override
			public int getBulletCount(String... name) {
				int num = 0;
				for (ItemStack item : player.inventory.mainInventory) {
					for (String str : name)
						if (ItemMagazine.isMagazine(item, str)) {
							num += HideNBT.getMagazineBulletNum(item) * item.getCount();
							break;
						}
				}
				return num;
			}

			@Override
			public int getMaxBulletCount(String... name) {
				int num = 0;
				for (ItemStack item : player.inventory.mainInventory)
					for (String str : name)
						if (ItemMagazine.isMagazine(item, str)) {
							num = Math.max(num, HideNBT.getMagazineBulletNum(item));
							break;
						}
				return num;
			}

			@Override
			public void addMagazine(String name, int amount) {
				if (!player.inventory.addItemStackToInventory(ItemMagazine.makeMagazine(name, amount)))
					player.dropItem(ItemMagazine.makeMagazine(name, amount), true);
			}
		};
	}

	/**銃の元にできるインターフェース*/
	public interface IGuns {
		public NBTTagCompound getGunTag();
	}

	public interface IMagazineHolder {
		/**指定された弾の数*/
		public int getBulletCount(String... name);

		public int getMaxBulletCount(String... name);

		/**指定されたマガジンの中で1番残弾が多いものを消費してその残弾数を返す*/
		public Magazine useMagazine(String... name);

		public void addMagazine(String name, int amount);
	}
}

//=================== Mobから利用するためのスタティックなコントローラー ========================= //TODO
