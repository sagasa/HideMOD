package newwork;

import java.nio.ByteBuffer;

import entity.EntityBullet;
import helper.ParseByteArray;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import types.BulletData;
import types.GunData;

/**送受信両用*/
public class PacketGuns implements IMessage,  IMessageHandler<PacketGuns, IMessage> {


	//モード
	/**弾の出現をクライアントからサーバーにリクエスト*/
	public static final int GUN_SHOOT = 0;
	/**エフェクト＋ヒットマーク用にサーバーからクライアントへ通知*/
	public static final int GUN_HIT = 1;

	//メッセージの内容
	int Mode;
    float Yaw;
    float Pitch;
    GunData gunData;
    BulletData bulletData;

    public PacketGuns(){}

    /**視線とプレイヤーインスタンス弾の名前のセット*/
    public PacketGuns(int mode,GunData data , float yaw,float pitch) {
        this.Yaw= yaw;
        this.Pitch = pitch;
        this.Mode = mode;
        this.gunData = data;
    }

    @Override//IMessageのメソッド。ByteBufからデータを読み取る。
    public void fromBytes(ByteBuf buf) {
    	this.Yaw = buf.readFloat();
    	this.Pitch = buf.readFloat();
    	//System.out.println("fromByte"+Yaw+" "+Pitch);
    }

    @Override//IMessageのメソッド。ByteBufにデータを書き込む。
    public void toBytes(ByteBuf buf) {
    	//System.out.println("toByte"+Yaw+" "+Pitch);
    	buf.writeFloat(Yaw);
    	buf.writeFloat(Pitch);
    }
    //受信イベント
    @Override//IMessageHandlerのメソッド
    public IMessage onMessage(final PacketGuns m, final MessageContext ctx) {
        //クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
        //EntityPlayer player = SamplePacketMod.proxy.getEntityPlayerInstance();
        //サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
    	//EntityPlayer Player = ctx.getServerHandler().playerEntity;
    //	System.out.println(ctx.side);
    	ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable()
    	{
    	  public void run() {
    	    processMessage(m);
    	  }
		private void processMessage(PacketGuns m) {
			 EntityPlayer Player = ctx.getServerHandler().playerEntity;
		     //弾を発射
		     //System.out.println("onMessage"+ctx.side);
		     EntityBullet bullet = new EntityBullet(Player.worldObj, Player,gunData, m.Yaw,m.Pitch);
		     Player.worldObj.spawnEntityInWorld(bullet);
		}
    	});

    	//プレイヤーを取得

        return null;//本来は返答用IMessageインスタンスを返すのだが、旧来のパケットの使い方をするなら必要ない。
    }
}
