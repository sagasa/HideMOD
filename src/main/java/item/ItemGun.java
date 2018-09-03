package item;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.RecoilHandler;
import helper.HideMath;
import helper.NBTWrapper;
import hideMod.HideMod;
import hideMod.PackData;
import io.PackLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.BulletData;
import types.GunData;
import types.GunFireMode;

public class ItemGun extends Item {

	public static Map<String, ItemGun> INSTANCE_MAP = new HashMap<String, ItemGun>();

	public GunData GunData;

	/** モデル描画 */
	// public RenderHideGun Model;

	// ========================================================================
	// 登録
	public ItemGun(GunData data) {
		super();
		this.setCreativeTab(CreativeTabs.COMBAT);
		String name = data.ITEM_INFO.NAME_SHORT;
		this.setUnlocalizedName(name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
		this.GunData = data;
		INSTANCE_MAP.put(name, this);
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(String name) {
		if (PackData.GUN_DATA_MAP.containsKey(name)) {
			ItemStack stack = new ItemStack(INSTANCE_MAP.get(name));
			stack = checkGunNBT(stack);
			return stack;
		}
		return null;
	}

	/** アイテムスタック作成時に呼ばれる これの中でNBTを設定する */
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		checkGunNBT(stack);
		return super.initCapabilities(stack, nbt);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getGunData(stack).ITEM_INFO.NAME_DISPLAY;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack checkGunNBT(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む
		GunData data = getGunData(item);

		NBTWrapper.setHideID(item, UUID.randomUUID().getLeastSignificantBits());
		NBTWrapper.setGunShootDelay(item, 0);
		NBTWrapper.setGunFireMode(item,
				GunFireMode.getFireMode(Arrays.asList(data.FIREMODE).iterator().next().toString()));
		NBTWrapper.setGunUseingBullet(item, Arrays.asList((String[]) data.BULLET_USE).iterator().next().toString());
		return item;
	}

	/** データ破損チェック */
	public static boolean isNormalData(GunData data) {
		// 弾が登録されているか
		if (((String[]) data.BULLET_USE).length == 0) {
			return false;
		}
		return true;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack checkGunMagazines(ItemStack item) {
		// マガジンの弾の登録があるかを確認 無ければ破棄
		LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
		for (int i = 0; i < magazines.length; i++) {
			if (!ItemMagazine.isMagazineExist(magazines[i].name)) {
				magazines[i] = null;
			}
		}
		NBTWrapper.setGunLoadedMagazines(item, magazines);
		return item;
	}

	// TODO 銃剣のオプション次第
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {

		return true;
	}

	// =========================================================
	// 更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		// 破損チェック
		if (!stack.hasTagCompound()) {
			return;
		}
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + NBTWrapper.getGunFireMode(stack));
		tooltip.add(ChatFormatting.GRAY + "UseBullet : "
				+ ItemMagazine.getBulletData(NBTWrapper.getGunUseingBullet(stack)).ITEM_INFO.NAME_DISPLAY);
		for (LoadedMagazine magazine : NBTWrapper.getGunLoadedMagazines(stack)) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getBulletData(magazine.name).ITEM_INFO.NAME_DISPLAY + "x" + magazine.num);
			} else {
				tooltip.add("empty");
			}
		}

	}

	/** 銃かどうか */
	public static boolean isGun(ItemStack item) {
		if (item != null && item.getItem() instanceof ItemGun) {
			return true;
		}
		return false;
	}

	/** スタックから銃の登録名を取得 */
	public static String getGunName(ItemStack item) {
		return getGunData(item).ITEM_INFO.NAME_SHORT;
	}

	/** GunData取得 */
	public static GunData getGunData(String name) {
		return PackData.getGunData(name);
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (!(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return ((ItemGun) item.getItem()).GunData;
	}

	// 銃火器の処理
	/**射撃*/
	public static void shoot(ItemStack gun){
		GunData gunData = getGunData(gun);
		BulletData bulletData = getNextBullet(gun);
		if(bulletData != null){

		}
	}

	/** 次の射撃モードを取得 */
	public static GunFireMode getNextFireMode(ItemStack gun) {
		GunData data = getGunData(gun);
		GunFireMode now = NBTWrapper.getGunFireMode(gun);
		List<String> modes = Arrays.asList(data.FIREMODE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		return GunFireMode.getFireMode(modes.get(index));
	}

	/** 次の使用する弾を取得 */
	public static String getNextUseMagazine(ItemStack gun) {
		GunData data = getGunData(gun);
		String now = NBTWrapper.getGunUseingBullet(gun);
		List<String> modes = Arrays.asList(data.BULLET_USE);
		int index = modes.indexOf(now.toString()) + 1;
		if (index > modes.size() - 1) {
			index = 0;
		}
		if (!ItemMagazine.isMagazineExist(modes.get(index))) {
			return now;
		}
		return modes.get(index);
	}

	/** 弾を1つ消費する 消費した弾のBulletDataを返す */
	private static BulletData getNextBullet(ItemStack gun) {
		LoadedMagazine[] loadedMagazines = NBTWrapper.getGunLoadedMagazines(gun);
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
	/**
	 * ロードできる弾の総量取得 * public static int getCanLoadMagazineNum(EntityPlayer
	 * player) { int num = 0; for (ItemStack item :
	 * player.inventory.mainInventory) { if (ItemMagazine.isMagazine(item,
	 * UsingBulletName)) { num += item.getCount() *
	 * ItemMagazine.getBulletNum(item); } } return num; }
	 *
	 * /** 最初のスロットの空きを取得 * private static int getNextReloadNum() { for
	 * (LoadedMagazine magazine : loadedMagazines) { // 入ってなければ要求 if (magazine
	 * == null) { // System.out.println(UsingBulletName); return
	 * ItemMagazine.getBulletData(UsingBulletName).MAGAZINE_SIZE; } int num =
	 * ItemMagazine.getBulletData(magazine.name).MAGAZINE_SIZE - magazine.num;
	 * if (num > 0 && magazine.name.equals(UsingBulletName)) { return num; } }
	 * return 0; }
	 *
	 * /** マガジンを追加 * private static void addMagazine(String name, int amount) {
	 * for (int i = 0; i < loadedMagazines.length; i++) { LoadedMagazine
	 * magazine = loadedMagazines[i]; // 入ってなければ追加 if (magazine == null) {
	 * loadedMagazines[i] = new LoadedMagazine(name, amount); return; } int num
	 * = ItemMagazine.getBulletData(magazine.name).MAGAZINE_SIZE - magazine.num;
	 * if (num > 0 && magazine.name.equals(UsingBulletName)) {
	 * loadedMagazines[i] = new LoadedMagazine(name, amount + magazine.num);
	 * return; } } }
	 *
	 * /** 弾を1つ消費する 消費した弾の登録名を返す * private static String getNextBullet() { for
	 * (int i = 0; i < loadedMagazines.length; i++) { LoadedMagazine magazine =
	 * loadedMagazines[i]; // 1つ消費する if (magazine != null && magazine.num > 0) {
	 * String name = magazine.name; magazine.num--; boolean flag = false; if
	 * (magazine.num <= 0) { magazine = null; flag = true; } loadedMagazines[i]
	 * = magazine; // マガジン繰り上げ if (flag && loadedMagazines.length > 1) { for
	 * (int j = 1; j < loadedMagazines.length; j++) { loadedMagazines[j - 1] =
	 * loadedMagazines[j]; } loadedMagazines[loadedMagazines.length - 1] = null;
	 * } return name; } } return null; }
	 *
	 * /** 射撃処理 * private static void gunShoot(EntityPlayer player, GunData gun)
	 * { // 弾を確認 String bulletName = getNextBullet(); if (bulletName == null) {
	 * // カチって音を出す… shooted = true; } else { // 存在する弾かどうか if
	 * (ItemMagazine.isMagazineExist(bulletName)) { // 拡散を取得 float spread; if
	 * (isADS) { spread = gun.ACCURACY_ADS; } else { spread = gun.ACCURACY; } //
	 * 向きに拡散を float yaw = (float) Math.toDegrees(Math.atan(Random.nextDouble() /
	 * 50 * HideMath.normal(0, spread))); float pitch = (float)
	 * Math.toDegrees(Math.atan(Random.nextDouble() / 50 * HideMath.normal(0,
	 * spread)));
	 *
	 * // PacketHandler.INSTANCE.sendToServer(new PacketGuns(gun, //
	 * PackData.BULLET_DATA_MAP.get(bulletName), // player.rotationYaw + yaw,
	 * player.rotationPitch + pitch)); // バーストかどうかでrateが変わる if (fireNum > 0) {
	 * ShootDelay = gun.BURST_RATE_TICK; } else { ShootDelay = gun.RATE_TICK; }
	 * // リコイル RecoilHandler.addRecoil(gun); // 100を超えないように代入 // recoilPower =
	 * recoilPower + // RecoilHandler.getRecoilPowerAdd(player, gun) > 100 ? 100
	 * // : recoilPower + RecoilHandler.getRecoilPowerAdd(player, gun); } else {
	 * // 存在しなかったなら破棄処理 // PacketHandler.INSTANCE // .sendToServer(new
	 * PacketGuns(UsingBulletName, (byte) // player.inventory.currentItem, 0));
	 * } // どっとを表示 // EntityDebug dot = new EntityDebug(player.worldObj, new //
	 * Vec3(player.posX,player.posY, player.posZ)); //
	 * player.worldObj.spawnEntityInWorld(dot); } }
	 *
	 * /** リロード完了 マガジンを追加する * public static void reloadEnd(int bulletNum, byte
	 * reloadQueueID) { // キューが進んでいたなら停止 if (reloadQueue != reloadQueueID) {
	 * return; } // リロードできたなら if (bulletNum != 0) { addMagazine(UsingBulletName,
	 * bulletNum); } }//
	 */
}
