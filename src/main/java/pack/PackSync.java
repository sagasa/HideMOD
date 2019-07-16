package pack;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import handler.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import network.PacketSync;
import types.base.DataBase;

public class PackSync {

	private static final Logger log = LogManager.getLogger();

	/***/
	public static void syncPack() {
		PacketHandler.INSTANCE.sendToAll(new PacketSync());
	}

	public static void sendPackInfo() {
	//	Minecraft.getMinecraft().player.sendChatMessage("Start server sync");
		List<List<Integer>> hash = getPackHash();
		PacketHandler.INSTANCE.sendToServer(new PacketSync(hash));
	}

	public static void makeChangeList(List<List<Integer>> clientList, EntityPlayerMP player) {
		System.out.println("list " + clientList);
		List<List<Integer>> removeList = new ArrayList<>();
		List<List<Integer>> addList = new ArrayList<>();
		List<List<Integer>> serverList = getPackHash();
		boolean removeFlag = false;
		boolean addFlag = false;
		for (int i = 0; i < clientList.size(); i++) {
			List<Integer> client = clientList.get(i);
			List<Integer> server = serverList.get(i);
			//削除差分
			List<Integer> removeSub = new ArrayList<>(client);
			removeSub.removeAll(server);
			if (removeSub.size() > 0)
				removeFlag = true;
			removeList.add(removeSub);
			//追加差分
			List<Integer> addSub = new ArrayList<>(server);
			addSub.removeAll(client);
			if (addSub.size() > 0)
				addFlag = true;
			addList.add(addSub);
		}
		if (removeFlag)
			PacketHandler.INSTANCE.sendTo(new PacketSync(removeList), player);
		if (addFlag) {
			PacketSync sync = new PacketSync();
			sync.setByteData(toData(addList));
			PacketHandler.INSTANCE.sendTo(sync, player);
		}
	}

	public static void addList(List<List<byte[]>> dataList) {
		Minecraft.getMinecraft().player.sendChatMessage("Add " + dataList);

	}

	public static void removeList(List<List<Integer>> hashList) {
		Minecraft.getMinecraft().player.sendChatMessage("Remove " + hashList);
	}

	//======= Hash化メゾットs========
	/**ハッシュからbyte配列に*/
	private static List<List<byte[]>> toData(List<List<Integer>> from) {
		List<List<byte[]>> outList = new ArrayList<>();
		List<Collection<?>> pack = getPackElements();

		for (int i = 0; i < from.size(); i++) {
			List<byte[]> out = new ArrayList<>();
			hashloop: for (Integer hash : from.get(i))
				for (Object obj : pack.get(i))
					if (toHash(toByteArray(obj)) == hash) {
						out.add(toByteArray(obj));
						break hashloop;
					}
			outList.add(out);
		}
		return outList;
	}

	private static List<List<Integer>> getPackHash() {
		return getPackElements().stream().map(elem -> toHashList(elem)).collect(Collectors.toList());
	}

	private static List<Collection<?>> getPackElements() {
		List<Collection<?>> list = new ArrayList<>();
		list.add(PackData.currentData.PACK_INFO);
		list.add(PackData.currentData.GUN_DATA_MAP.values());
		list.add(PackData.currentData.MAGAZINE_DATA_MAP.values());
		list.add(PackData.currentData.ATTACHMENT_DATA_MAP.values());
		list.add(PackData.currentData.ICON_MAP.values());
		list.add(PackData.currentData.MODEL_MAP.values());
		list.add(PackData.currentData.TEXTURE_MAP.values());
		return list;
	}

	private static List<Integer> toHashList(Collection<? extends Object> obj) {
		return obj.stream().map(data -> toHash(toByteArray(data))).collect(Collectors.toList());
	}

	/**ByteArray*/
	private static byte[] toByteArray(Object data) {
		if (data instanceof byte[]) {
			return (byte[]) data;
		} else if (data instanceof DataBase) {
			return ((DataBase) data).MakeJsonData().getBytes(Charset.forName("UTF-8"));
		} else {
			log.warn("toHash data type is not support");
			return ArrayUtils.EMPTY_BYTE_ARRAY;
		}
	}

	/**4バイトハッシュ*/
	private static int toHash(byte[] data) {
		try {
			final MessageDigest md = MessageDigest.getInstance("md2");
			md.update(data);
			return ByteBuffer.wrap(md.digest()).getInt();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return 0;
		}
	}
}