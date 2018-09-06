package item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.realmsclient.gui.ChatFormatting;

import handler.GunManager;
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
import playerdata.GunState;
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
	/** 射撃 */
	public static void shootUpdate(ItemStack gun, Entity shooter, GunFireMode mode, GunState state, boolean isADS,
			boolean trigger) {
		GunManager.gunUpdate(shooter, getGunData(gun), NBTWrapper.getHideTag(gun), mode, state, isADS, trigger);
	}

	/** 弾が1つでも入っているか */
	public static boolean canShoot(ItemStack gun) {
		if (!isGun(gun)) {
			return false;
		}
		if(LoadedMagazine.getLoadedNum(NBTWrapper.getGunLoadedMagazines(gun))>0){
			return true;
		}
		return false;
	}

	/** リロード まだリロード処理が残ればtrue */
	public static boolean reload(EntityPlayer player, ItemStack gun, boolean isexit) {
		System.out.println("開始");
		// リスト化
		List<LoadedMagazine> list = new ArrayList<>();
		for (LoadedMagazine mag : NBTWrapper.getGunLoadedMagazines(gun)) {
			if (mag != null && mag.num > 0) {
				list.add(mag);
			}
		}
		// 排出
		if (isexit) {
			for (LoadedMagazine magazine : list) {
				if (PackData.getBulletData(magazine.name) != null) {
					player.addItemStackToInventory(ItemMagazine.makeMagazine(magazine.name, magazine.num));
				}
			}
			list.clear();
		}
		GunData data = getGunData(gun);
		System.out.println("start" + list);
		if (data.RELOAD_ALL) {
			while (reload(player, gun, list))
				;
			NBTWrapper.setGunLoadedMagazines(gun, list.toArray(new LoadedMagazine[list.size()]));
			return false;
		} else {
			reload(player, gun, list);
			System.out.println("end" + list);
			NBTWrapper.setGunLoadedMagazines(gun, list.toArray(new LoadedMagazine[list.size()]));
			return reload(player, gun, list);
		}
	}

	/** リロード処理 何かしたらtrue */
	private static boolean reload(EntityPlayer player, ItemStack gun, List<LoadedMagazine> list) {
		GunData data = getGunData(gun);
		String magName = NBTWrapper.getGunUseingBullet(gun);
		int magSize = ItemMagazine.getBulletData(magName).MAGAZINE_SIZE;
		if (list.size() < data.LOAD_NUM) {
			int n = getMag(magName, magSize, player, ItemMagazine.getBulletData(magName).MAGAZINE_BREAK);
			if (n == 0) {
				return false;
			}
			list.add(0, new LoadedMagazine(magName, n));
			return true;
		}
		for (LoadedMagazine mag : list) {
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

	private static int getMag(String name, int value, EntityPlayer player, boolean isBreak) {
		int c = value;
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, name)) {
				int n = NBTWrapper.getMagazineBulletNum(item);
				if (n <= c) {
					c -= n;
					if (item.getCount() > 1) {
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
	/**銃で使用中の弾薬の所持数を返す*/
	public static int getCanUseingBulletNum(ItemStack gun,EntityPlayer player){
		int num = 0;
		String bulletName = NBTWrapper.getGunUseingBullet(gun);
		for(ItemStack item : player.inventory.mainInventory){
			if(ItemMagazine.isMagazine(item, bulletName)){
				num += NBTWrapper.getMagazineBulletNum(item)*item.getCount();
			}
		}
		return num;
	}
}
