package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import gamedata.HidePlayerData;
import gamedata.LoadedMagazine;
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
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunFireMode;
import types.Sound;

/** 入力をサーバーに送る */
public class PacketInput implements IMessage, IMessageHandler<PacketInput, IMessage> {

	private byte mode;
	public static final byte TRIGGER_CHANGE = 0;
	public static final byte GUN_RELOAD = 1;
	public static final byte GUN_ADS = 2;
	public static final byte GUN_MODE = 3;
	public static final byte GUN_BULLET = 4;

	public PacketInput() {
	}

	private boolean right;
	private boolean left;

	/** トリガーパケット */
	public PacketInput(boolean left, boolean right) {
		this.mode = TRIGGER_CHANGE;
		this.right = right;
		this.left = left;
	}
	public PacketInput(byte mode){
		this.mode = mode;
	}

	private boolean isADS;
	/**ADSトグルパケット*/
	public PacketInput(boolean isads){
		this.mode = GUN_ADS;
		this.isADS = isads;
	}

	private EquipMode equipMode;

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == TRIGGER_CHANGE) {
			right = buf.readBoolean();
			left = buf.readBoolean();
		} else if (mode == GUN_ADS) {
			isADS = buf.readBoolean();
		} else if (mode == GUN_BULLET) {
		} else if (mode == GUN_RELOAD) {
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == TRIGGER_CHANGE) {
			buf.writeBoolean(right);
			buf.writeBoolean(left);
		} else if (mode == GUN_ADS) {
			buf.writeBoolean(isADS);
		} else if (mode == GUN_BULLET) {
		} else if (mode == GUN_RELOAD) {
		}
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(PacketInput m, MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		HidePlayerData data = PlayerHandler.getPlayerData(ctx.getServerHandler().player);
		if (m.mode == TRIGGER_CHANGE) {
			data.Server.leftMouse = m.left;
			data.Server.rightMouse = m.right;
		}else if(m.mode == GUN_BULLET){
			data.Server.changeAmmo = true;
		}else if(m.mode == GUN_MODE){
			data.Server.changeFiremode = true;
		}else if(m.mode == GUN_RELOAD){
			data.Server.reload = true;
		}

		return null;
	}
}
