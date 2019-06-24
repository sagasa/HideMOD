package network;

import handler.PacketHandler;
import handler.SoundHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.effect.Sound;

public class PacketPlaySound implements IMessage, IMessageHandler<PacketPlaySound, IMessage> {

	int entityID;
	String Name;
	double X;
	double Y;
	double Z;
	float Vol;
	float Pitch;
	float Range;
	boolean UseDelay;
	boolean UseDecay;

	public PacketPlaySound() {
	}

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

	/** クライアント→クライアント 指定位置で再生 */
	public PacketPlaySound(Sound sound, double x, double y, double z) {
		this(sound.NAME, x, y, z, sound.VOL, sound.PITCH, sound.RANGE, sound.USE_DELAY, sound.USE_DECAY);
	}

	/** クライアント→クライアント 指定位置で再生 */
	public PacketPlaySound(String soundName, double x, double y, double z, float vol, float pitch, float range,
			boolean delay, boolean decay) {
		Name = soundName;
		X = x;
		Y = y;
		Z = z;
		Vol = vol;
		Pitch = pitch;
		Range = range;
		UseDelay = delay;
		UseDecay = decay;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		Name = PacketHandler.readString(buf);
		X = buf.readDouble();
		Y = buf.readDouble();
		Z = buf.readDouble();
		Vol = buf.readFloat();
		Pitch = buf.readFloat();
		Range = buf.readFloat();
		UseDelay = buf.readBoolean();
		UseDecay = buf.readBoolean();
		entityID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketHandler.writeString(buf, Name);
		buf.writeDouble(X);
		buf.writeDouble(Y);
		buf.writeDouble(Z);
		buf.writeFloat(Vol);
		buf.writeFloat(Pitch);
		buf.writeFloat(Range);
		buf.writeBoolean(UseDelay);
		buf.writeBoolean(UseDecay);
		buf.writeInt(entityID);
	}

	@Override
	public IMessage onMessage(final PacketPlaySound m, final MessageContext ctx) {
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
				EntityPlayer player = ctx.getServerHandler().player;
				SoundHandler.broadcastSound(ctx.getServerHandler().player.world, m.entityID, m.Name, m.X, m.Y, m.Z,
						m.Range, m.Vol, m.Pitch,
						m.UseDelay, m.UseDecay);

			});
		} else
			Minecraft.getMinecraft().addScheduledTask(() -> playSound(m));

		return null;
	}

	@SideOnly(Side.CLIENT)
	static void playSound(PacketPlaySound m) {
		SoundHandler.playSound(Minecraft.getMinecraft().world.getEntityByID(m.entityID), m.Name, m.X, m.Y, m.Z, m.Range,
				m.Vol, m.Pitch, m.UseDelay, m.UseDecay);
	}
}
