package network;

import gamedata.HideEntitySound;
import handler.PacketHandler;
import handler.SoundHandler;
import handler.client.HideSoundManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.effect.Sound;

public class PacketPlaySound implements IMessage, IMessageHandler<PacketPlaySound, IMessage> {

	boolean isStopSound = false;
	String Name;
	double X;
	double Y;
	double Z;
	float Vol;
	float Pitch;
	float Range;
	boolean UseDelay;
	boolean UseDecay;
	boolean Excepting;

	Byte trackID = null;

	Integer entityID = null;

	public PacketPlaySound() {
	}

	/***/
	public PacketPlaySound(int e, byte track) {
		isStopSound = true;
		entityID = e;
		trackID = track;
	}

	/**サーバー→クライアントのエンティティにバインドした音*/
	public PacketPlaySound(int e, String soundName, double x, double y, double z, float vol, float pitch,
			float range, boolean useDelay, boolean useDecay) {
		entityID = e;
		Name = soundName;
		X = x;
		Y = y;
		Z = z;
		Vol = vol;
		Pitch = pitch;
		Range = range;
		UseDelay = useDelay;
		UseDecay = useDecay;
	}

	/**サーバー→クライアントのエンティティにバインドし、キャンセル可能な音*/
	public PacketPlaySound(int e, byte track, String soundName, double x, double y, double z, float vol, float pitch,
			float range, boolean useDelay, boolean useDecay) {
		this(e, soundName, x, y, z, vol, pitch, range, useDelay, useDecay);
		trackID = track;
	}

	/** クライアント→クライアント 指定位置で再生 */
	public PacketPlaySound(Sound sound, double x, double y, double z) {
		this(sound.NAME, x, y, z, sound.VOL, sound.PITCH, sound.RANGE, sound.USE_DELAY, sound.USE_DECAY, true);
	}

	/** クライアント→クライアント 指定位置で再生 */
	public PacketPlaySound(String soundName, double x, double y, double z, float vol, float pitch, float range,
			boolean delay, boolean decay, boolean excepting) {
		Name = soundName;
		X = x;
		Y = y;
		Z = z;
		Vol = vol;
		Pitch = pitch;
		Range = range;
		UseDelay = delay;
		UseDecay = decay;
		Excepting = excepting;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		isStopSound = buf.readBoolean();
		if (isStopSound) {
			entityID = buf.readInt();
			trackID = buf.readByte();
		} else {
			Name = PacketHandler.readString(buf);
			X = buf.readDouble();
			Y = buf.readDouble();
			Z = buf.readDouble();
			Vol = buf.readFloat();
			Pitch = buf.readFloat();
			Range = buf.readFloat();
			UseDelay = buf.readBoolean();
			UseDecay = buf.readBoolean();
			Excepting = buf.readBoolean();
			if (buf.readBoolean())
				entityID = buf.readInt();
			if (buf.readBoolean())
				trackID = buf.readByte();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(isStopSound);
		if (isStopSound) {
			buf.writeInt(entityID);
			buf.writeByte(trackID);
		} else {
			PacketHandler.writeString(buf, Name);
			buf.writeDouble(X);
			buf.writeDouble(Y);
			buf.writeDouble(Z);
			buf.writeFloat(Vol);
			buf.writeFloat(Pitch);
			buf.writeFloat(Range);
			buf.writeBoolean(UseDelay);
			buf.writeBoolean(UseDecay);
			buf.writeBoolean(Excepting);
			buf.writeBoolean(entityID != null);
			if (entityID != null)
				buf.writeInt(entityID);
			buf.writeBoolean(trackID != null);
			if (trackID != null)
				buf.writeByte(trackID);
		}
	}

	@Override
	public IMessage onMessage(final PacketPlaySound m, final MessageContext ctx) {
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
				EntityPlayer player = ctx.getServerHandler().player;
				SoundHandler.broadcastSound(ctx.getServerHandler().player.world, m.entityID, m.Name, m.X, m.Y, m.Z,
						m.Range, m.Vol, m.Pitch,
						m.UseDelay, m.UseDecay, m.Excepting, m.trackID);
			});
		} else
			Minecraft.getMinecraft().addScheduledTask(() -> playSound(m));
		return null;
	}

	@SideOnly(Side.CLIENT)
	static void playSound(PacketPlaySound m) {
		if (m.isStopSound) {
			HideSoundManager.stopSound(m.entityID, m.trackID);
		} else {
			HideEntitySound sound = new HideEntitySound(Minecraft.getMinecraft().world.getEntityByID(m.entityID),
					m.Name,
					m.X, m.Y, m.Z, m.Vol, m.Pitch, m.Range, m.UseDelay, m.UseDecay, SoundCategory.PLAYERS);
			if (m.trackID == null)
				HideSoundManager.playSound(sound);
			else
				HideSoundManager.playSound(sound, m.trackID);
		}
	}

}
