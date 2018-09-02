package handler;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import newwork.PacketHit;
import newwork.PacketPlaySound;
import newwork.PacketPlayerState;

public class PacketHandler {

    //このMOD用のSimpleNetworkWrapperを生成。チャンネルの文字列は固有であれば何でも良い。MODIDの利用を推奨。
    //チャンネル名は20文字以内の文字数制限があるので注意。
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("HideMod");


    public static void init() {

        /*IMesssageHandlerクラスとMessageクラスの登録。
        *第三引数：MessageクラスのMOD内での登録ID。256個登録できる
        *第四引数：送り先指定。クライアントかサーバーか、Side.CLIENT Side.SERVER*/
        INSTANCE.registerMessage(PacketPlayerState.class, PacketPlayerState.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PacketHit.class, PacketHit.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(PacketPlaySound.class, PacketPlaySound.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PacketPlaySound.class, PacketPlaySound.class, 4, Side.SERVER);
    }
    /**バッファに文字列を書き込む*/
    public static void writeString(ByteBuf buf,String str){
    	buf.writeInt(str.length());
    	buf.writeBytes(str.getBytes());
    }
    /**バッファから文字列を読み込む*/
    public static String readString(ByteBuf buf){
    	int length = buf.readInt();
    	return buf.readBytes(length).toString(Charset.forName("UTF-8"));
    }
    /**バッファにbyte配列を書き込む*/
    public static void writeBytes(ByteBuf buf,byte[] data){
    	buf.writeInt(data.length);
    	buf.writeBytes(data);
    }
    /**バッファからbyte配列を読み込む*/
    public static byte[] readBytes(ByteBuf buf){
    	int length = buf.readInt();
    	return buf.readBytes(length).array();
    }

    /**SHA-256でエンコードしたハッシュを返す*/
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
}