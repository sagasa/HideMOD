package hide.guns;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import helper.HideAdder;
import hide.guns.data.LoadedMagazine;
import hide.guns.data.LoadedMagazine.Magazine;
import hide.types.guns.GunFireMode;
import hide.types.guns.ProjectileData;
import hide.types.items.GunData;
import hide.types.items.ItemData;
import hide.types.items.MagazineData;
import hide.types.util.DataView;
import hide.types.util.DataView.ViewCache;
import items.ItemMagazine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import pack.PackData;

/** 銃の制御系
 * TickUpdateを要求
 * Playerに紐付けで動作 */
public abstract class CommonGun {

	protected static final Logger log = LogManager.getLogger();
	/**ロード可能な弾薬のどれかをロードする*/
	public static final String LOAD_ANY = "ANY";

	public static final byte SOUND_RELOAD = HideAdder.SoundCate.getNumber();

	protected ShootPoints shootPoint = ShootPoints.DefaultShootPoint;
	protected NBTTagCompound gun;
	protected IMagazineHolder magazineHolder;
	protected EnumHand hand;

	// ===============クライアント,サーバー共通部分==================
	protected DataView<ProjectileData> dataView = new DataView<>(ProjectileData.class, 1);
	protected GunData gunData = null;

	public CommonGun(EnumHand hand) {
		this.hand = hand;
	}

	//銃の情報の比較用キャッシング
	private String gunName;
	private List<String> gunAttachments;

	/**NBTを読んで銃のデータ更新の必要があれば更新する*/
	public boolean updateTag(NBTTagCompound gunTag) {
		if (gunTag == null) {
			if (gun != null) {
				gun = null;
				gunName = null;
				updateData();
			}
			return false;
		}
		List<String> attachments = HideGunNBT.GUN_ATTACHMENTS.get(gunTag);
		String name = HideGunNBT.DATA_NAME.get(gunTag);
		//データ更新の必要があるなら
		if (!(attachments.equals(gunAttachments) && name.equals(gunName))) {
			gunName = name;
			gunAttachments = attachments;

			gun = PackData.getGunData(name) == null ? null : gunTag;
			updateCustomize();
			updateData();
			return true;
		}
		gun = gunTag;
		return false;
	}

	/**武器のデータが変わったら呼び出される*/
	abstract protected void updateData();

	/** カスタムとオリジナルから修正版のGunDataを作成
	 * カスタムパーツが見つからなければスキップ*/
	protected void updateCustomize() {
		if (isGun()) {
			gunData = PackData.getGunData(HideGunNBT.DATA_NAME.get(gun));
			dataView.setBase(gunData.get(GunData.Data));
			dataView.setModifier(0, null);
		}
	}

	public void setMagHolder(IMagazineHolder mag) {
		magazineHolder = mag;
	}

	public boolean isGun() {
		return gun != null && magazineHolder != null;
	}

	public boolean stateEquals(CommonGun other) {
		return this.dataView.get(ItemData.ShortName).equals(other.dataView.get(ItemData.ShortName))
				&& this.getFireMode() == other.getFireMode();
	}

	public GunData getGunData() {
		return gunData;
	}

	public ViewCache<ProjectileData> getProjectile() {
		return dataView.getView();
	}

	public NBTTagCompound getGunTag() {
		return gun;
	}

	// 射撃アップデート

	/** NBT保存 */
	public LoadedMagazine magazine;

	public abstract void tickUpdate();

	/** RPMをミリ秒に変換 */
	protected int RPMtoMillis(float rPM) {
		return 0 < rPM ? (int) (60000 / rPM) : 0;
	}

	/** ミリ秒をTickに変換 */
	protected float MillistoTick(int millis) {
		return millis / 50f;
	}

	public void saveToNBT() {
		HideGunNBT.GUN_MAGAZINES.set(gun, magazine);
	}

	/** 次の射撃モードを取得 */
	public GunFireMode getNextFireMode() {
		if (!isGun())
			return null;
		GunFireMode now = HideGunNBT.GUN_FIREMODE.get(gun);
		List<GunFireMode> modes = Arrays.asList(gunData.get(GunData.FireMode));
		int index = modes.indexOf(now) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		return modes.get(index);
	}

	/** 次の使用する弾を取得 */
	public String getNextUseMagazine() {
		if (!isGun())
			return null;
		String now = HideGunNBT.GUN_USEBULLET.get(gun);
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
		return gunData.get(GunData.LoadSize) * Arrays.stream(getUseMagazines())
				.map(str -> PackData.getBulletData(str).get(MagazineData.MagazineSize)).max(Comparator.naturalOrder()).get();
	}

	/**利用可能なすべてのマガジンを返す NBTの文字列じゃないことに注意*/
	public String[] getUseMagazines() {
		String name = HideGunNBT.GUN_USEBULLET.get(gun);
		if (name == null)
			return ArrayUtils.EMPTY_STRING_ARRAY;
		return name.equals(LOAD_ANY) ? getUseMagazineList() : new String[] { name };
	}

	public String getUseMagazine() {
		return HideGunNBT.GUN_USEBULLET.get(gun);
	}

	public String[] getUseMagazineList() {
		return gunData.get(GunData.UseMagazine);
	}

	public GunFireMode getFireMode() {
		return HideGunNBT.GUN_FIREMODE.get(gun);
	}

	public List<GunFireMode> getFireModeList() {
		return Arrays.asList(gunData.get(GunData.FireMode)).stream().collect(Collectors.toList());
	}

	public void setGun(EntityPlayer player) {

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
							int bulletNum = HideGunNBT.getMagazineBulletNum(item);
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
							num += HideGunNBT.getMagazineBulletNum(item) * item.getCount();
							break;
						}
				}
				return num;
			}

			@Override
			public Magazine getMaxMagazine(String... name) {
				InventoryPlayer inv = player.inventory;
				Magazine mag = new Magazine(null, 0);
				int index = -1;
				for (int i = 0; i < inv.mainInventory.size(); i++) {
					ItemStack item = inv.mainInventory.get(i);
					//
					for (String str : name)
						if (ItemMagazine.isMagazine(item, str)) {
							int bulletNum = HideGunNBT.getMagazineBulletNum(item);
							//	System.out.println("find mag " + item + " " + bulletNum);
							if (bulletNum > mag.num) {
								index = i;
								mag.num = bulletNum;
								mag.name = str;
							}
							break;
						}
				}
				return mag;
			}

			@Override
			public void addMagazine(String name, int amount) {
				if (!player.inventory.addItemStackToInventory(ItemMagazine.makeMagazine(name, amount)))
					player.dropItem(ItemMagazine.makeMagazine(name, amount), true);
			}
		};
	}

	public interface IMagazineHolder {
		/**指定された弾の数*/
		public int getBulletCount(String... name);

		/**指定されたマガジンの中で1番残弾が多いものを返す*/
		public Magazine getMaxMagazine(String... name);

		/**指定されたマガジンの中で1番残弾が多いものを消費して返す*/
		public Magazine useMagazine(String... name);

		public void addMagazine(String name, int amount);
	}
}

//=================== Mobから利用するためのスタティックなコントローラー ========================= //TODO
