package types;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

public class HideDamage extends DamageSource{

	/**追加したダメージケース*/
	public HideDamageCase DamageCase;
	public String Tool;
	public EntityLivingBase Attacker;

	public HideDamage(HideDamageCase Case,EntityLivingBase attacker) {
		super(Case.toString());
		this.Attacker = attacker;
		this.DamageCase = Case;
	}

	public enum HideDamageCase{
		GUN_Explosion,
		GUN_BULLET
	}
}
