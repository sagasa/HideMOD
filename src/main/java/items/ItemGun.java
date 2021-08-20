package items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mojang.realmsclient.gui.ChatFormatting;

import hide.guns.CommonGun;
import hide.guns.HideGunNBT;
import hide.guns.data.LoadedMagazine.Magazine;
import hide.types.guns.GunFireMode;
import hide.types.items.GunData;
import hide.types.items.ItemData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import pack.PackData;

public class ItemGun extends HideItem<GunData> {
	/** モデル描画 */
	// public RenderHideGun Model;

	// ========================================================================

	public ItemGun(String name, Map<String, GunData> data) {
		super(name, data);
	}

	public static ItemGun INSTANCE;

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(String name) {
		return makeGun(PackData.getGunData(name));
	}

	/** アイテムスタックを作成 */
	public static ItemStack makeGun(GunData data) {
		if (data != null) {
			ItemStack stack = new ItemStack(INSTANCE);
			stack = makeGunNBT(stack, data);
			return stack;
		}
		return null;
	}

	/** どのような状態からでも有効なNBTを書き込む */
	public static ItemStack makeGunNBT(ItemStack item, GunData data) {
		if (!(item.getItem() instanceof ItemGun)) {
			return item;
		}
		// タグがなければ書き込む;
		NBTTagCompound hideTag = HideGunNBT.getHideTag(item);
		HideGunNBT.DATA_NAME.set(hideTag, data.get(ItemData.ShortName));
		HideGunNBT.GUN_SHOOTDELAY.set(hideTag, 0);
		HideGunNBT.GUN_FIREMODE.set(hideTag,
				GunFireMode.getFireMode(Arrays.asList(data.get(GunData.FireMode)).iterator().next().toString()));
		HideGunNBT.GUN_USEBULLET.set(hideTag, CommonGun.LOAD_ANY);
		return item;
	}

	private static final String HideItemModifiers = "HideItemModifiers";

	public static void updateItenStack(ItemStack item) {
		System.out.println("check item valid " + PackData.getSessionTime() + " " + HideGunNBT.DATA_SESSIONTIME.get(HideGunNBT.getHideTag(item)));
		System.out.println("state " + item.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		if (PackData.getSessionTime() != HideGunNBT.DATA_SESSIONTIME.get(HideGunNBT.getHideTag(item))) {
			System.out.println("update item modifire");
			HideGunNBT.DATA_SESSIONTIME.set(HideGunNBT.getHideTag(item), PackData.getSessionTime());
			GunData data = getGunData(item);
			System.out.println(data.toJson());
			item.addAttributeModifier("generic.movementSpeed", new AttributeModifier("hideItemAttrib", data.get(GunData.ItemMoveSpeed), 1), EntityEquipmentSlot.MAINHAND);
			item.addAttributeModifier("generic.attackDamage", new AttributeModifier("hideItemAttrib", data.get(GunData.ItemAttackDamage), 1), EntityEquipmentSlot.MAINHAND);
			item.addAttributeModifier("generic.knockbackResistance", new AttributeModifier("hideItemAttrib", data.get(GunData.ItemKnockbackResistance), 1), EntityEquipmentSlot.MAINHAND);
			item.addAttributeModifier("generic.maxHealth", new AttributeModifier("hideItemAttrib", data.get(GunData.ItemMaxHealth), 1), EntityEquipmentSlot.MAINHAND);
			if (data.get(GunData.UseSecondary)) {
				//item.getAttributeModifiers(EntityEquipmentSlot.OFFHAND).replaceValues(HideItemModifiers, modifiers);
			} else {
				//item.getAttributeModifiers(EntityEquipmentSlot.OFFHAND).replaceValues(HideItemModifiers, Collections.emptyList());
			}
		}
	}

	/** データ破損チェック */
	public static boolean isNormalData(GunData data) {
		// 弾が登録されているか
		if (data.get(GunData.UseMagazine).length == 0) {
			return false;
		}
		return true;
	}

	// TODO 銃剣のオプション次第
	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		return true;
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return super.initCapabilities(stack, nbt);
	}

	// =========================================================
	// 更新 便利機能
	/** アップデート 表示更新など */
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		// 破損チェック
		NBTTagCompound hideTag = HideGunNBT.getHideTag(stack);
		tooltip.add(ChatFormatting.GRAY + "FireMode : " + HideGunNBT.GUN_FIREMODE.get(hideTag));
		String useBullet = HideGunNBT.GUN_USEBULLET.get(hideTag);
		tooltip.add(ChatFormatting.GRAY + "UseBullet : "
				+ (CommonGun.LOAD_ANY.equals(useBullet) ? CommonGun.LOAD_ANY
						: ItemMagazine.getMagazineName(useBullet)));
		for (Magazine magazine : HideGunNBT.GUN_MAGAZINES.get(hideTag).getList()) {
			if (magazine != null) {
				tooltip.add(ItemMagazine.getMagazineName(magazine.name) + "x" + magazine.num);
			} else {
				tooltip.add("empty");
			}
		}
	}

	/** 銃かどうか */
	public static boolean isGun(ItemStack item) {
		return getGunData(item) != null;
	}

	/** GunData取得 */
	public static GunData getGunData(String name) {
		return PackData.getGunData(name);
	}

	/** GunData取得 */
	public static GunData getGunData(ItemStack item) {
		if (item != null && !(item.getItem() instanceof ItemGun)) {
			return null;
		}
		return PackData.getGunData(HideGunNBT.DATA_NAME.get(HideGunNBT.getHideTag(item)));
	}

	@Override
	public ItemStack makeItem(GunData data) {
		// TODO 自動生成されたメソッド・スタブ
		return makeGun(data);
	}
}
