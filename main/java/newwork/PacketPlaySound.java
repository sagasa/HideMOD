package newwork;

import handler.PacketHandler;
import handler.SoundHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPlaySound implements IMessage, IMessageHandler<PacketPlaySound, IMessage> {

	private String Name;
	private double X;
	private double Y;
	private double Z;
	private float Vol;
	private float Pitch;
	private int Delay;
	private int EntityID;
	
	public PacketPlaySound(String soundName, double x, double y, double z, float vol, float pitch, int delay,int entityID) {
		Name = soundName;
		X = x;
		Y = y;
		Z = z;
		Vol = vol;
		Pitch = pitch;
		Delay = delay;
		EntityID = entityID;
	}
	@Override
	public void fromBytes(ByteBuf buf) {
		Name = PacketHandler.readString(buf);
		X = buf.readDouble();
		Y = buf.readDouble();
		Z = buf.readDouble();
		Vol = buf.readFloat();
		Pitch = buf.readFloat();
		Delay = buf.readInt();
		EntityID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketHandler.writeString(buf, Name);
		buf.writeDouble(X);
		buf.writeDouble(Y);
		buf.writeDouble(Z);
		buf.writeFloat(Vol);
		buf.writeFloat(Pitch);
		buf.writeInt(Delay);
		buf.writeInt(EntityID);
	}
	@Override
	public IMessage onMessage(PacketPlaySound m, MessageContext ctx) {
		SoundHandler.addSoundTask(m.Name, m.X, m.Y, m.Z, m.Vol, m.Pitch, m.Delay,m.EntityID);
		return null;
	}
}
