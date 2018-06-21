package newwork;

import handler.PacketHandler;
import handler.SoundHandler;
import hideMod.HideMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.Sound;
import types.inGame.HideSound;

public class PacketPlaySound implements IMessage, IMessageHandler<PacketPlaySound, IMessage> {

	private static final byte CLIENT_PLAYSOUND = 0;
	private static final byte SERVER_PLAYREQ = 1;
	private byte Mode;
	private String Name;
	private double X;
	private double Y;
	private double Z;
	private float Vol;
	private float Pitch;

	private int Delay;

	private float Range;
	private boolean UseDelay;
	private boolean UseDecay;

	public PacketPlaySound() {
	}

	/**サーバー→クライアント 指定位置で再生*/
	public PacketPlaySound(String soundName, double x, double y, double z, float vol, float pitch, int delay) {
		Mode = CLIENT_PLAYSOUND;
		Name = soundName;
		X = x;
		Y = y;
		Z = z;
		Vol = vol;
		Pitch = pitch;
		Delay = delay;
	}
	/**クライアント→クライアント 指定位置で再生*/
	public PacketPlaySound(Sound sound, double x, double y, double z) {
		this(sound.name, x, y, z, sound.vol, sound.pitch, sound.range, sound.isDelay, sound.isDecay);
	}
	/**クライアント→クライアント 指定位置で再生*/
	public PacketPlaySound(String soundName, double x, double y, double z, float vol, float pitch, float range,boolean delay,boolean decay) {
		Mode = SERVER_PLAYREQ;
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
		Mode = buf.readByte();
		Name = PacketHandler.readString(buf);
		X = buf.readDouble();
		Y = buf.readDouble();
		Z = buf.readDouble();
		Vol = buf.readFloat();
		Pitch = buf.readFloat();
		if(Mode==SERVER_PLAYREQ){
			Range = buf.readFloat();
			UseDelay = buf.readBoolean();
			UseDecay = buf.readBoolean();
		}else{
			Delay = buf.readInt();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(Mode);
		PacketHandler.writeString(buf, Name);
		buf.writeDouble(X);
		buf.writeDouble(Y);
		buf.writeDouble(Z);
		buf.writeFloat(Vol);
		buf.writeFloat(Pitch);
		if(Mode==SERVER_PLAYREQ){
			buf.writeFloat(Range);
			buf.writeBoolean(UseDelay);
			buf.writeBoolean(UseDecay);
		}else{
			buf.writeInt(Delay);
		}
	}
	@Override
	public IMessage onMessage(final PacketPlaySound m, final MessageContext ctx) {
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable() {
				public void run() {
					processMessage(m);
				}
				private void processMessage(PacketPlaySound m) {
					EntityPlayer player = ctx.getServerHandler().playerEntity;
					SoundHandler.broadcastSound(player.worldObj, m.Name, m.X, m.Y, m.Z, m.Range, m.Vol, m.Pitch, m.UseDelay, m.UseDecay);
				}
			});
		}else{
			//再生
			playSound(m);
		}
		return null;
	}
	@SideOnly(Side.CLIENT)
	private void playSound(PacketPlaySound m){
		HideSound sound = new HideSound(m.Name, m.Vol, m.Pitch, (float)m.X, (float)m.Y, (float)m.Z);
		Minecraft.getMinecraft().getSoundHandler().playDelayedSound(sound, m.Delay);
	}
}
