package network;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import handler.PlayerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketAcceleration implements IMessage, IMessageHandler<PacketAcceleration, IMessage> {

    public PacketAcceleration() { }


    private float acceleration = 0F;

    public PacketAcceleration(float acceleration) {
        //this.player = player;
        this.acceleration = acceleration;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        acceleration = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(acceleration);
    }


    @Override
    public IMessage onMessage(PacketAcceleration message, MessageContext ctx) {

        EntityPlayer player = ctx.getServerHandler().player;
        if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
            EntityDrivable drivable = (EntityDrivable)player.getRidingEntity();
            drivable.setAcceleration(acceleration);
        }
        return null;
    }
}
