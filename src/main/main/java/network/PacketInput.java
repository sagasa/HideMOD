package network;

import gamedata.HidePlayerData;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 入力をサーバーに送る */
public class PacketInput implements IMessage, IMessageHandler<PacketInput, IMessage> {

	private byte mode;
	public static final byte TRIGGER_CHANGE = 0;
	public static final byte GUN_RELOAD = 1;
	public static final byte GUN_ADS = 2;
	public static final byte GUN_MODE = 3;
	public static final byte GUN_BULLET = 4;
	public static final byte DRIVABLE_LEFT = 5;
	public static final byte DRIVABLE_RIGHT = 6;
	public static final byte DRIVABLE_FORWARD = 7;
	public static final byte DRIVABLE_BACK = 8;

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
			data.Server.changeFireMode = true;
		}else if(m.mode == GUN_RELOAD){
			data.Server.reload = true;
		}else if(m.mode == DRIVABLE_LEFT) {
			data.Server.leftInputDown = !data.Server.leftInputDown;
		}else if(m.mode == DRIVABLE_RIGHT) {
			data.Server.rightInputDown = !data.Server.rightInputDown;
		}else if(m.mode == DRIVABLE_FORWARD) {
			data.Server.forwardInputDown = !data.Server.forwardInputDown;
		}else if(m.mode == DRIVABLE_BACK) {
			data.Server.backInputDown = !data.Server.backInputDown;
		}

		return null;
	}
}
