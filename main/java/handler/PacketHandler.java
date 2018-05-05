package handler;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import newwork.PacketGuns;
import newwork.PacketHit;

public class PacketHandler {

    //このMOD用のSimpleNetworkWrapperを生成。チャンネルの文字列は固有であれば何でも良い。MODIDの利用を推奨。
    //チャンネル名は20文字以内の文字数制限があるので注意。
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("HideMod");


    public static void init() {

        /*IMesssageHandlerクラスとMessageクラスの登録。
        *第三引数：MessageクラスのMOD内での登録ID。256個登録できる
        *第四引数：送り先指定。クライアントかサーバーか、Side.CLIENT Side.SERVER*/
        INSTANCE.registerMessage(PacketGuns.class, PacketGuns.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PacketGuns.class, PacketGuns.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(PacketHit.class, PacketHit.class, 2, Side.CLIENT);
    }
}