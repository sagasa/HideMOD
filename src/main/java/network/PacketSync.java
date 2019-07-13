package network;

import java.util.ArrayList;
import java.util.List;

import handler.PacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import pack.PackSync;

public class PacketSync implements IMessage, IMessageHandler<PacketSync, IMessage> {

	//**サーバーからクライアントへ 現在のパックの状況を送るようリクエスト*/
	private static final byte START = 0;
	//**クライアントからサーバーへ 現在のパック状態の送信*/
	private static final byte SEND_HASH = 1;
	private static final byte SEND_DATA = 2;

	private byte mode;

	public PacketSync() {
		mode = START;
	}

	List<List<byte[]>> dataList;

	public void setByteData(List<List<byte[]>> list) {
		mode = SEND_DATA;
		dataList = list;
	}

	List<List<Integer>> hashList;

	public PacketSync(List<List<Integer>> list) {
		mode = SEND_HASH;
		hashList = list;
	}

	@Override
	public IMessage onMessage(PacketSync m, MessageContext ctx) {
		if (m.mode == START)
			PackSync.sendPackInfo();
		else if (m.mode == SEND_HASH)
			if (ctx.side == Side.SERVER)
				PackSync.makeChangeList(m.hashList, ctx.getServerHandler().player);
			else
				PackSync.removeList(m.hashList);
		else if (m.mode == SEND_DATA)
			PackSync.addList(m.dataList);
		return null;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == SEND_HASH) {
			buf.writeInt(hashList.size());
			hashList.forEach(l1 -> {
				buf.writeInt(l1.size());
				l1.forEach(i -> buf.writeInt(i));
			});
		} else if (mode == SEND_DATA) {
			buf.writeInt(dataList.size());
			dataList.forEach(l1 -> {
				buf.writeInt(l1.size());
				l1.forEach(b -> PacketHandler.writeBytes(buf, b));
			});
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		mode = buf.readByte();
		if (mode == SEND_HASH) {
			hashList = new ArrayList<>();
			int size0 = buf.readInt();
			for (int i = 0; i < size0; i++) {
				List<Integer> list1 = new ArrayList<>();
				int size1 = buf.readInt();
				for (int j = 0; j < size1; j++) {
					list1.add(buf.readInt());
				}
				hashList.add(list1);
			}
		} else if (mode == SEND_DATA) {
			dataList = new ArrayList<>();
			int size0 = buf.readInt();
			for (int i = 0; i < size0; i++) {
				List<byte[]> list1 = new ArrayList<>();
				int size1 = buf.readInt();
				for (int j = 0; j < size1; j++) {
					list1.add(PacketHandler.readBytes(buf));
				}
				dataList.add(list1);
			}
		}
	}
}