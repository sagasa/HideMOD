package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import handler.PacketHandler;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import handler.SoundHandler;
import helper.NBTWrapper;
import hideMod.HideMod;
import hideMod.PackData;
import io.netty.buffer.ByteBuf;
import item.ItemGun;
import item.ItemMagazine;
import item.LoadedMagazine;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAnvilBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import playerdata.PlayerData;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunFireMode;
import types.Sound;

/** サーバー */
public class PacketPlayerState implements IMessage, IMessageHandler<PacketPlayerState, IMessage> {

	byte mode;
	static final byte TRIGGER_CHANGE = 0;
	static final byte GUN_RELOAD = 1;
	static final byte EQUIP_MODE = 2;
	static final byte GUN_MODE = 3;
	static final byte GUN_BULLET = 4;

	public PacketPlayerState() {
	}

	private boolean right;
	private boolean left;

	/** トリガーパケット */
	public PacketPlayerState(boolean left, boolean right) {
		this.mode = TRIGGER_CHANGE;
		this.right = right;
		this.left = left;
	}

	private EquipMode equipMode;

	/** 状態通知 */
	public PacketPlayerState(EquipMode equipmode) {
		equipMode = equipmode;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == TRIGGER_CHANGE) {
			right = buf.readBoolean();
			left = buf.readBoolean();
		} else if (mode == GUN_MODE) {
		} else if (mode == GUN_BULLET) {
		} else if (mode == GUN_RELOAD) {
		} else if (mode == EQUIP_MODE) {
			equipMode = EquipMode.valueOf(PacketHandler.readString(buf));
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == TRIGGER_CHANGE) {
			buf.writeBoolean(right);
			buf.writeBoolean(left);
		} else if (mode == GUN_MODE) {
		} else if (mode == GUN_BULLET) {
		} else if (mode == GUN_RELOAD) {
		} else if (mode == EQUIP_MODE) {
			PacketHandler.writeString(buf, equipMode.toString());
		}
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(PacketPlayerState m, MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		System.out.println("パケット受信");
		PlayerData data = PlayerHandler.getPlayerData(ctx.getServerHandler().player);
		if (m.mode == TRIGGER_CHANGE) {
			data.leftMouse = m.left;
			data.rightMouse = m.right;
		}else if(m.mode == EQUIP_MODE){
			data.equipMode = m.equipMode;
		}
		return null;
	}
}
