package hide.common;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PacketHandler {

	// このMOD用のSimpleNetworkWrapperを生成。チャンネルの文字列は固有であれば何でも良い。MODIDの利用を推奨。
	// チャンネル名は20文字以内の文字数制限があるので注意。

	public static void init() {
		DataSerializers.registerSerializer(Vec3d);
		DataSerializers.registerSerializer(Vec2f);
		/*
		 * IMesssageHandlerクラスとMessageクラスの登録。
		 * 第三引数：MessageクラスのMOD内での登録ID。256個登録できる
		 * 第四引数：送り先指定。クライアントかサーバーか、Side.CLIENT Side.SERVER
		 */

		//		INSTANCE.registerMessage(PacketSync.class, PacketSync.class, 5, Side.SERVER);


	}

	/** EntityDataManager用のデータシリアライザ */
	public static final DataSerializer<Vec3d> Vec3d = new DataSerializer<Vec3d>() {
		@Override
		public void write(PacketBuffer buf, Vec3d value) {
			buf.writeDouble(value.x);
			buf.writeDouble(value.y);
			buf.writeDouble(value.z);
		}

		@Override
		public Vec3d read(PacketBuffer buf) throws IOException {
			return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
		}

		@Override
		public DataParameter<Vec3d> createKey(int id) {
			return new DataParameter<>(id, this);
		}

		@Override
		public Vec3d copyValue(Vec3d value) {
			return value;
		}
	};
	/** EntityDataManager用のデータシリアライザ */
	public static final DataSerializer<Vec2f> Vec2f = new DataSerializer<Vec2f>() {
		@Override
		public void write(PacketBuffer buf, Vec2f value) {
			buf.writeFloat(value.x);
			buf.writeFloat(value.y);
		}

		@Override
		public Vec2f read(PacketBuffer buf) throws IOException {
			return new Vec2f(buf.readFloat(), buf.readFloat());
		}

		@Override
		public DataParameter<Vec2f> createKey(int id) {
			return new DataParameter<>(id, this);
		}

		@Override
		public Vec2f copyValue(Vec2f value) {
			return value;
		}
	};
}