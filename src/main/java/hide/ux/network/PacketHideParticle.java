package hide.ux.network;

import java.util.Random;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketHideParticle implements IMessage, IMessageHandler<PacketHideParticle, IMessage> {

	public PacketHideParticle() {
	}

	private static final byte VelocityType = 0b0000001;
	private static final byte VelocityVec3 = 0b0000001;

	private static final byte SpwanType = 0b0000010;
	private static final byte SpawnBox = 0b0000000;
	private static final byte SpawnCylinder = 0b0000010;

	byte mode;

	int id;
	int[] param;

	public PacketHideParticle(int id, int... param) {
		this.id = id;
		this.param = param;
	}

	float vx, vy, vz, speed;

	public PacketHideParticle setVelecity(float speed) {
		this.speed = speed;
		return this;
	}

	public PacketHideParticle setVelecity3(float vx, float vy, float vz) {
		mode &= ~VelocityType;
		mode |= VelocityVec3;
		this.vx = vx;
		this.vy = vy;
		this.vz = vz;
		return this;
	}

	float x, y, z, x1, y1, z1, r;
	int count;

	public PacketHideParticle spawnBox(float x, float y, float z, float dx, float dy, float dz, int count) {
		mode &= ~SpwanType;
		mode |= SpawnBox;
		this.x = x;
		this.y = y;
		this.z = z;
		this.x1 = dx;
		this.y1 = dy;
		this.z1 = dz;
		this.count = count;
		return this;
	}

	public PacketHideParticle spawnCylinder(float x0, float y0, float z0, float x1, float y1, float z1, float r, int count) {
		mode &= ~SpwanType;
		mode |= SpawnCylinder;
		this.x = x0;
		this.y = y0;
		this.z = z0;
		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		this.r = r;
		this.count = count;
		return this;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		mode = buf.readByte();
		id = buf.readInt();
		int size = buf.readInt();
		param = new int[size];
		for (int i = 0; i < size; i++)
			param[i] = buf.readInt();

		x = buf.readFloat();
		y = buf.readFloat();
		z = buf.readFloat();
		x1 = buf.readFloat();
		y1 = buf.readFloat();
		z1 = buf.readFloat();
		if ((mode & SpwanType) == SpawnCylinder) {
			r = buf.readFloat();
		}
		count = buf.readInt();

		speed = buf.readFloat();
		if ((mode & VelocityType) == VelocityVec3) {
			vx = buf.readFloat();
			vy = buf.readFloat();
			vz = buf.readFloat();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		buf.writeInt(id);
		buf.writeInt(param.length);
		for (int i = 0; i < param.length; i++)
			buf.writeInt(param[i]);
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
		buf.writeFloat(x1);
		buf.writeFloat(y1);
		buf.writeFloat(z1);
		if ((mode & SpwanType) == SpawnCylinder)
			buf.writeFloat(r);
		buf.writeInt(count);

		buf.writeFloat(speed);
		if ((mode & VelocityType) == VelocityVec3) {
			buf.writeFloat(vx);
			buf.writeFloat(vy);
			buf.writeFloat(vz);
		}
	}

	private static final Random randome = new Random();

	@Override
	public IMessage onMessage(PacketHideParticle msg, MessageContext ctx) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			for (int i = 0; i < msg.count; i++) {
				double x = msg.x;
				double y = msg.y;
				double z = msg.z;
				if ((msg.mode & SpwanType) == SpawnBox) {
					x += randome.nextGaussian() * msg.x1;
					y += randome.nextGaussian() * msg.y1;
					z += randome.nextGaussian() * msg.z1;
				} else {
					//直線状に分散
					double pos = randome.nextDouble();
					x += msg.x - (msg.x1 - msg.x) * pos;
					y += msg.y - (msg.y1 - msg.y) * pos;
					z += msg.z - (msg.z1 - msg.z) * pos;
					//半径で分散
					x += randome.nextGaussian() * msg.r;
					y += randome.nextGaussian() * msg.r;
					z += randome.nextGaussian() * msg.r;
				}

				double vx = this.randome.nextGaussian() * msg.speed;
				double vy = this.randome.nextGaussian() * msg.speed;
				double vz = this.randome.nextGaussian() * msg.speed;
				if ((msg.mode & VelocityType) == VelocityVec3) {
					vx += msg.vx;
					vy += msg.vy;
					vz += msg.vz;
				}
				mc.effectRenderer.spawnEffectParticle(msg.id, x, y, z, vx, vy, vz, msg.param);
			}

		});
		return null;
	}
}
