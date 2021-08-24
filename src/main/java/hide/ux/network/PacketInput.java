package hide.ux.network;

import hide.core.HidePlayerDataManager;
import hide.guns.PlayerData.EquipMode;
import hide.guns.PlayerData.ServerPlayerData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 入力をサーバーに送る */
public class PacketInput implements IMessage, IMessageHandler<PacketInput, IMessage> {

	private byte mode;
	public static final byte GUN_RELOAD = 1;
	public static final byte GUN_ADS = 2;
	public static final byte GUN_MODE = 3;
	public static final byte GUN_BULLET = 4;

	public PacketInput() {
	}

	public PacketInput(byte mode) {
		this.mode = mode;
	}

	private float ADState;

	/**ADS変更パケット*/
	public PacketInput(float ads) {
		this.mode = GUN_ADS;
		this.ADState = ads;
	}

	private EquipMode equipMode;

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == GUN_ADS) {
			ADState = buf.readFloat();
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == GUN_ADS) {
			buf.writeFloat(ADState);
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

		ServerPlayerData data = HidePlayerDataManager.getServerData(ServerPlayerData.class, ctx.getServerHandler().player);
		if (m.mode == GUN_BULLET)
			data.changeAmmo = true;
		else if (m.mode == GUN_MODE)
			data.changeFireMode = true;
		else if (m.mode == GUN_RELOAD)
			data.reload = true;
		else if (m.mode == GUN_RELOAD)
			data.reload = true;
		else if (m.mode == GUN_ADS)
			data.setADS(m.ADState);
		return null;
	}
}
