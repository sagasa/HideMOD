package model;

import org.lwjgl.opengl.GL11;

import hide.model.impl.ModelImpl;
import hide.types.base.Info;
import hide.types.base.NamedData;
import hide.types.value.WeightEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HideModel extends NamedData {

	public static final DataEntry<String> Name = of("sample");

	public static final DataEntry<String> Model = of("sample", new Info().IsName(true));

	public static final DataEntry<String> HandPos = of("");
	public static final DataEntry<String> SightPos = of("");
	public static final DataEntry<String> BullelPos = of("");

	public ModelImpl model;

	public static final DataEntry<WeightEntry<String>[]> ReloadAnimation = of(new WeightEntry[0]);
	public static final DataEntry<WeightEntry<String>[]> ShootAnimation = of(new WeightEntry[0]);

	@Override
	public DataEntry<String> displayName() {
		return Name;
	}

	@Override
	public DataEntry<String> systemName() {
		return Name;
	}

	public HideModel setModel(ModelImpl model) {
		System.out.println("setModel " + model);
		this.model = model;
		return this;
	}

	public static class Pos3f {
		public float X = 0, Y = 0, Z = 0;

		@Override
		public String toString() {
			return "[" + X + "," + Y + "," + Z + "]";
		}
	}

	@SideOnly(Side.CLIENT)
	public void render(boolean firstPerson, IRenderProperty prop) {
		GL11.glPushMatrix();
		GL11.glScalef(0.15f, 0.15f, 0.15f);
		model.render();
		GL11.glPopMatrix();
	}
}
