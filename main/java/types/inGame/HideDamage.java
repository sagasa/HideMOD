package types.inGame;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import types.inGame.HideDamage.HideDamageCase;

public class HideDamage extends DamageSource {

	/** 追加したダメージケース */
	public HideDamageCase DamageCase;
	public String Tool;
	public EntityLivingBase Attacker;

	public HideDamage(HideDamageCase Case, EntityLivingBase attacker) {
		super(Case.langName);
		this.Attacker = attacker;
		this.DamageCase = Case;
	}

	/** ダメージを与える 無敵時間を無効化 */
	public static void Attack(EntityLivingBase victim, HideDamage damageSource, float damage) {
		victim.attackEntityFrom(damageSource, damage);
		victim.hurtResistantTime = 0;

		// リフレクションで改変が必要な変数にぶち抜く
		try {
			// 名前を指定して取得
			Field attackingPlayer = EntityLivingBase.class.getDeclaredField("attackingPlayer");
			Field recentlyHit = EntityLivingBase.class.getDeclaredField("recentlyHit");
			Method recentlySetRevenge = EntityLivingBase.class.getMethod("setRevengeTarget", EntityLivingBase.class);
			// アクセス権限を与える
			attackingPlayer.setAccessible(true);
			recentlyHit.setAccessible(true);
			// 攻撃時と同じ処理を組み込む
			attackingPlayer.set(victim, (EntityPlayer) damageSource.Attacker);
			recentlyHit.set(victim, 100);
			recentlySetRevenge.invoke(victim, damageSource.Attacker);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
				| NoSuchMethodException | InvocationTargetException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public IChatComponent getDeathMessage(EntityLivingBase p_151519_1_) {
		EntityLivingBase entitylivingbase1 = p_151519_1_.func_94060_bK();
		String s = "death.attack." + this.damageType;
		String s1 = s + ".player";
		return entitylivingbase1 != null && StatCollector.canTranslate(s1)
				? new ChatComponentTranslation(s1,
						new Object[] { p_151519_1_.getDisplayName(), entitylivingbase1.getDisplayName() })
				: new ChatComponentTranslation(s, new Object[] { p_151519_1_.getDisplayName() });
	}

	public enum HideDamageCase {
		GUN_Explosion("gun.exp"), GUN_BULLET("gun");
		String langName;

		private HideDamageCase(String name) {
			langName = name;
		}
	}
}
