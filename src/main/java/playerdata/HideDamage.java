package playerdata;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class HideDamage extends DamageSource {

	/** 追加したダメージケース */
	public HideDamageCase DamageCase;
	public String Tool;
	public Entity Attacker;

	private static Field attackingPlayer;
	private static Field recentlyHit;
	private static Method recentlySetRevenge;

	public static void init() {
		// 名前を指定して取得
		try {
			//こっちじゃなければもう1つを試す
			try {
				attackingPlayer = EntityLivingBase.class.getDeclaredField("attackingPlayer");
				recentlyHit = EntityLivingBase.class.getDeclaredField("recentlyHit");
				recentlySetRevenge = EntityLivingBase.class.getMethod("setRevengeTarget", EntityLivingBase.class);
			} catch (NoSuchFieldException | NoSuchMethodException e) {
				try {
					attackingPlayer = attackingPlayer == null ? EntityLivingBase.class.getDeclaredField("field_70717_bb") : attackingPlayer;
					recentlyHit = recentlyHit == null ? EntityLivingBase.class.getDeclaredField("field_70718_bc") : recentlyHit;
					recentlySetRevenge = recentlySetRevenge == null
							? EntityLivingBase.class.getMethod("func_70604_c", EntityLivingBase.class) : recentlySetRevenge;
				} catch (NoSuchFieldException | NoSuchMethodException e1) {
					System.out.println("どっちでもなかった");
					e1.printStackTrace();
				}
			}

		} catch (SecurityException e) {
			e.printStackTrace();
		}
		// アクセス権限を与える
		attackingPlayer.setAccessible(true);
		recentlyHit.setAccessible(true);
	}

	public HideDamage(HideDamageCase Case, Entity attacker) {
		super(Case.langName);
		this.Attacker = attacker;
		this.DamageCase = Case;
	}

	/** ダメージを与える 無敵時間を無効化 ダメージが入ったらtrue */
	public static boolean Attack(EntityLivingBase victim, HideDamage damageSource, float damage) {
		boolean value = victim.attackEntityFrom(damageSource, damage);
		victim.hurtResistantTime = 0;
		// リフレクションで改変が必要な変数にぶち抜く
		try {
			// 攻撃時と同じ処理を組み込む TODO タレットなどの処理の追記がいるかも
			if (damageSource.Attacker instanceof EntityPlayer) {
				attackingPlayer.set(victim, (EntityPlayer) damageSource.Attacker);
				recentlyHit.set(victim, 100);
			}
			recentlySetRevenge.invoke(victim, damageSource.Attacker);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e1) {
			e1.printStackTrace();
		}
		return value;
	}

	@Override
	public ITextComponent getDeathMessage(EntityLivingBase p_151519_1_) {
		return super.getDeathMessage(p_151519_1_);
	}

	public enum HideDamageCase {
		GUN_Explosion("gun.exp"), GUN_BULLET("gun");
		String langName;

		private HideDamageCase(String name) {
			langName = name;
		}
	}
}
