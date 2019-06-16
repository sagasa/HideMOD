package network;

import entity.EntityDrivable;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRotate implements IMessage, IMessageHandler<PacketRotate, IMessage> {

    public PacketRotate() { }

    private float rotate = 0F;

    public PacketRotate(float rotate) {
        this.rotate = rotate;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        rotate = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(rotate);
    }



    @Override
    public IMessage onMessage(PacketRotate message, MessageContext ctx) {
        EntityPlayer player = ctx.getServerHandler().player;
        if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
            EntityDrivable drivable = (EntityDrivable)player.getRidingEntity();
            drivable.setRotate(rotate);
        }
        return null;
    }
}