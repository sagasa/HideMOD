package model;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import hide.core.HidePlayerDataManager;
import hide.guns.PlayerData.ClientPlayerData;
import hide.model.impl.IAnimation;
import hide.model.impl.ModelImpl;
import hide.model.impl.NodeImpl;
import hide.model.util.BufferUtil;
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

	protected IAnimation getAnimation(DataEntry<WeightEntry<String>[]> entry) {
		WeightEntry<String>[] array = get(entry);
		if (array.length == 0)
			return null;

		String name = array[0].Value;//TODO 全クライアントで同じ結果を返したい
		return model.getAnimation(name);
	}

	public HideModel setModel(ModelImpl model) {
		System.out.println("setModel " + model);
		this.model = model;
		return this;
	}

	public NodeImpl getNode(String key) {
		return model.getNode(key);
	}

	private static final ThreadLocal<FloatBuffer> TMP_FB16 = ThreadLocal.withInitial(() -> BufferUtil.createFloatBuffer(16));

	@SideOnly(Side.CLIENT)
	public void render(boolean firstPerson, IRenderProperty prop, float partialTicks) {
		GL11.glPushMatrix();
		GL11.glScalef(0.15f, 0.15f, 0.15f);

		if (prop != null) {
			IAnimation anim = getAnimation(ReloadAnimation);
			if (anim != null) {
				anim.apply(prop.getAnimationProp(AnimationType.Reload, partialTicks));
				//System.out.println("A " + prop.getAnimationProp(AnimationType.Reload, partialTicks));
			}
		}
		//model.render();
		ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
		float progress = prop != null ? prop.getAnimationProp(AnimationType.ADS, partialTicks) : 0;
		float reload = prop != null ? prop.getAnimationProp(AnimationType.ADS, partialTicks) : 0;

		Vector3f sight = model.getNodePos(get(HideModel.SightPos));
		Vector3f hand = model.getNodePos(get(HideModel.HandPos));

		sight.scale(progress);
		hand.scale(1 - progress);

		Vector3f.add(hand, sight, hand);
		GL11.glTranslatef(-hand.x, -hand.y, -hand.z);

		model.render();

		//model.render();
		GL11.glPopMatrix();
	}
}
