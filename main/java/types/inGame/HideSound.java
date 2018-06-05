package types.inGame;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

/** 全パラメータ定義可能なコンストラクタ */
public class HideSound extends PositionedSound {

	public HideSound(String soundName, float volume, float pitch, float xPosition, float yPosition, float zPosition) {
		this(soundName, volume, pitch, false, 0, ISound.AttenuationType.NONE, xPosition, yPosition, zPosition);
	}

	public HideSound(String soundName, float volume, float pitch, boolean repeat, int repeatDelay,
			ISound.AttenuationType attenuationType, float xPosition, float yPosition, float zPosition) {
		super(new ResourceLocation(soundName));
		this.volume = volume;
		this.pitch = pitch;
		this.xPosF = xPosition;
		this.yPosF = yPosition;
		this.zPosF = zPosition;
		this.repeat = repeat;
		this.repeatDelay = repeatDelay;
		this.attenuationType = attenuationType;
	}

}
