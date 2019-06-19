package handler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import network.PacketHit;
import network.PacketInput;
import network.PacketPlaySound;
import network.PacketShoot;

public class PacketHandler {

	// このMOD用のSimpleNetworkWrapperを生成。チャンネルの文字列は固有であれば何でも良い。MODIDの利用を推奨。
	// チャンネル名は20文字以内の文字数制限があるので注意。
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("HideMod");

	public static void init() {
		DataSerializers.registerSerializer(Vec3d);
		DataSerializers.registerSerializer(Vec2f);
		/*
		 * IMesssageHandlerクラスとMessageクラスの登録。
		 * 第三引数：MessageクラスのMOD内での登録ID。256個登録できる
		 * 第四引数：送り先指定。クライアントかサーバーか、Side.CLIENT Side.SERVER
		 */
		INSTANCE.registerMessage(PacketInput.class, PacketInput.class, 0, Side.SERVER);
		INSTANCE.registerMessage(PacketShoot.class, PacketShoot.class, 1, Side.SERVER);
		INSTANCE.registerMessage(PacketHit.class, PacketHit.class, 2, Side.CLIENT);
		INSTANCE.registerMessage(PacketPlaySound.class, PacketPlaySound.class, 3, Side.CLIENT);
		INSTANCE.registerMessage(PacketPlaySound.class, PacketPlaySound.class, 4, Side.SERVER);

//		INSTANCE.registerMessage(PacketSync.class, PacketSync.class, 5, Side.SERVER);
	}

	/***/
	public static void syncToClient() {
//TODO
	}
	public static void syncFromServer(byte cate) {
//TODO
	}

	/** バッファに文字列を書き込む */
	public static void writeString(ByteBuf buf, String str) {
		buf.writeInt(str.length());
		buf.writeBytes(str.getBytes());
	}

	/** バッファから文字列を読み込む */
	public static String readString(ByteBuf buf) {
		int length = buf.readInt();
		return buf.readBytes(length).toString(Charset.forName("UTF-8"));
	}

	/** バッファにbyte配列を書き込む */
	public static void writeBytes(ByteBuf buf, byte[] data) {
		buf.writeInt(data.length);
		buf.writeBytes(data);
	}

	/** バッファからbyte配列を読み込む */
	public static byte[] readBytes(ByteBuf buf) {
		int length = buf.readInt();
		return buf.readBytes(length).array();
	}

	/** SHA-256でエンコードしたハッシュを返す */
	public static String toEncryptedHashValue(byte[] value) {
		MessageDigest md = null;
		StringBuilder sb = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(value);
		sb = new StringBuilder();
		for (byte b : md.digest()) {
			String hex = String.format("%02x", b);
			sb.append(hex);
		}
		return sb.toString();
	}

	/** EntityDataManager用のデータシリアライザ */
	public static final DataSerializer<Vec3d> Vec3d = new DataSerializer<Vec3d>() {
		public void write(PacketBuffer buf, Vec3d value) {
			buf.writeDouble(value.x);
			buf.writeDouble(value.y);
			buf.writeDouble(value.z);
		}

		public Vec3d read(PacketBuffer buf) throws IOException {
			return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
		}

		public DataParameter<Vec3d> createKey(int id) {
			return new DataParameter<>(id, this);
		}

		public Vec3d copyValue(Vec3d value) {
			return value;
		}
	};
	/** EntityDataManager用のデータシリアライザ */
	public static final DataSerializer<Vec2f> Vec2f = new DataSerializer<Vec2f>() {
		public void write(PacketBuffer buf, Vec2f value) {
			buf.writeFloat(value.x);
			buf.writeFloat(value.y);
		}

		public Vec2f read(PacketBuffer buf) throws IOException {
			return new Vec2f(buf.readFloat(), buf.readFloat());
		}

		public DataParameter<Vec2f> createKey(int id) {
			return new DataParameter<>(id, this);
		}

		public Vec2f copyValue(Vec2f value) {
			return value;
		}
	};
}