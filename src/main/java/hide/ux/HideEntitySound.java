package hide.ux;

import hide.types.effects.Sound;
import hide.types.util.DataView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

//TODO サウンドカテゴリ追加したい…にゃあ
/**多機能サウンドクラス
 * 位置追従可能
 * 音量減衰の独自化
 * 障害物が間にある際の音の変化
 * ドップラー*/
public class HideEntitySound extends PositionedSound implements ITickableSound {
	/** 1ブロック当たり何tickかかるか */
	private static final float SOUND_SPEAD = 0.2f;

	/**トラッキング対象 消えたら再生を終了*/
	final protected Entity entity;

	final protected Vec3d moveVec;

	final private float RANGE;
	final private float VOL;
	final private float PITCH;
	final private boolean USE_DECAY;
	final private boolean USE_DELAY;

	protected boolean donePlaying = false;

	/**距離からディレイを算出*/
	public int getDelay() {
		if (!USE_DELAY)
			return 0;
		return (int) (SOUND_SPEAD * getDistance());
	}

	public float getRange() {
		return RANGE;
	}

	public Entity getEntity() {
		return entity;
	}

	public double getDistance() {
		if (Minecraft.getMinecraft().getRenderViewEntity() == null)
			return 0;
		return Minecraft.getMinecraft().getRenderViewEntity().getPositionVector()
				.distanceTo(new Vec3d(xPosF, yPosF, zPosF));
	}

	public HideEntitySound(Entity e, String soundName, double x, double y, double z, float vol, float pitch,
			float range,
			boolean delay, boolean decay, SoundCategory categoryIn) {
		super(new ResourceLocation(soundName), categoryIn);
		this.pitch = pitch;
		this.volume = vol;
		this.attenuationType = AttenuationType.NONE;
		this.entity = e;
		this.moveVec = (x == 0 && y == 0 && z == 0) ? Vec3d.ZERO : new Vec3d(-z, y, x);
		this.RANGE = range;
		this.USE_DECAY = decay;
		this.USE_DELAY = delay;
		this.PITCH = pitch;
		this.VOL = vol;
		update();
	}

	/** エンティティに追従する音
	 * @param e 追従先*/
	protected HideEntitySound(DataView<Sound> sound, Entity e, SoundCategory categoryIn) {
		this(sound, e, 0, 0, 0, categoryIn);
	}

	/** エンティティに追従する音
	 * @param e 追従先
	 * @param move エンティティと音源の位置関係*/
	protected HideEntitySound(DataView<Sound> sound, Entity e, double x, double y, double z, SoundCategory categoryIn) {
		this(e, sound.get(Sound.Name), x, y, z, sound.get(Sound.Volume), sound.get(Sound.Pitch), sound.get(Sound.Range), sound.get(Sound.UseDelay), sound.get(Sound.UseDecay), categoryIn);
	}

	@Override
	public void update() {
		if (entity == null)
			return;

		if (entity.isDead) {
			donePlaying = true;
			return;
		}
		//位置更新
		Vec3d location = entity.getPositionVector();
		if (moveVec != Vec3d.ZERO) {
			location = location.add(
					moveVec.rotatePitch(-entity.rotationPitch / 57.29578f).rotateYaw(-entity.rotationYaw / 57.29578f));
		}
		xPosF = (float) location.x;
		yPosF = (float) location.y;
		zPosF = (float) location.z;
		if (USE_DECAY)
			volume = VOL * (float) Math.min(1.3f - (getDistance() / RANGE), 1f);
	}

	@Override
	public boolean isDonePlaying() {
		return donePlaying;
	}
}
