package entity;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.core.net.DatagramSocketManager;

import helper.RayTracer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.GunData;
import types.GunData.GunDataList;
import types.HideDamage;
import types.HideDamage.HideDamageCase;

/**銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加*/
public class EntityBullet extends Entity implements IEntityAdditionalSpawnData{

	/*
	 */

	public EntityBullet(World worldIn) {
		super(worldIn);
		this.setSize(0.5F, 0.5F);
		RayTracer = new RayTracer();
	}

	int life = 60;
	int tick = 0;
	public EntityLivingBase Shooter;

	GunData gunData;
	UUID Shooter_uuid;
	RayTracer RayTracer;

	/**init時点での速度*/
	Vec3 Vec0;
	boolean fastTick = true;

	float DamageForPlayer = 1.5F;

	public EntityBullet(World worldIn,EntityLivingBase shooter,GunData data,float yaw, float pitch) {
		this(worldIn);
		gunData = data;
		Shooter = shooter;
		Shooter_uuid = shooter.getUniqueID();
		//System.out.println(this.worldObj.isRemote);

		//Shooter.addChatMessage(new ChatComponentText("発射"));
		//データ格納

		setLocationAndAngles(Shooter.posX, Shooter.posY+1.62F, Shooter.posZ, yaw, pitch);
		setPosition(posX, posY, posZ);

		//データから読み取る
		float spead = gunData.getDataFloat(GunDataList.BULLET_SPEED);
		//向いている方向に
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionY = -Math.sin(Math.toRadians(rotationPitch));

		float f2 = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ + motionY * motionY);
		motionX /= f2;
		motionZ /= f2;
		motionY /= f2;

		motionX *= spead;
		motionY *= spead;
		motionZ *= spead;
		Vec0 = new Vec3(motionX, motionY, motionZ);
	}

	@Override
	public void onUpdate() {
		//初期化
		if(fastTick&&Vec0!=null){
			setVelocity(Vec0.xCoord,Vec0.yCoord,Vec0.zCoord);
		}
		this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;

        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);

        if(!this.worldObj.isRemote){
        	//サーバーサイド
        	Vec3 lvo = new Vec3(lastTickPosX, lastTickPosY, lastTickPosZ);
            Vec3 lvt = new Vec3(posX, posY, posZ);
            	float damage = DamageForPlayer;
            	DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET,Shooter).setDamageBypassesArmor();
            	for(Entity e: RayTracer.getHitEntity(this,worldObj, lvo, lvt)){
            	//	System.out.println(e);
            		System.out.println(e.getName());
            		if(e!=null&&e instanceof EntityLiving || e instanceof EntityPlayer){
            			//無敵時間を殺す
            			if (e instanceof EntityPlayer){
            				damage = RayTracer.getPartDamage(e, lvo, lvt, damage);

            			}
            			e.attackEntityFrom(damagesource, damage);
            			e.hurtResistantTime = 0;
            		//	Shooter.addChatMessage(new ChatComponentText("Hit to "+e.getName()));
            		}
            //	System.out.println(e.getName());
            	}
            	//Shooter.addChatMessage(new ChatComponentText(""));
            	 //List<Entity>
    		//	System.out.println("deat");
        }else{

        	//クライアントサイド
        	//this.worldObj.spawnParticle(EnumParticleTypes.REDSTONE, this.posX, this.posY, this.posZ, 1, 1, 1, new int[0]);
        }
     //   System.out.println(life +" "+tick);
      //  System.out.println(posX+" "+posY+" "+posZ+"  "+worldObj.getWorldTime());
        if(life<tick){
			this.setDead();
        }
        tick++;
	}
	/**クライアントに必要な情報を送る*/
	@Override
	public void writeSpawnData(ByteBuf buffer) {
	//	System.out.println("write to "+rotationYaw+ " "+rotationPitch+ " "+motionX);
		buffer.writeFloat(rotationYaw);
		buffer.writeFloat(rotationPitch);
		buffer.writeDouble(motionX);
		buffer.writeDouble(motionY);
		buffer.writeDouble(motionZ);
	}

	/**サーバーからの情報を変数に書き込む*/
	@Override
	public void readSpawnData(ByteBuf Data) {
		rotationYaw = Data.readFloat();
		rotationPitch = Data.readFloat();
		Vec0 = new Vec3(Data.readDouble(), Data.readDouble(), Data.readDouble());
	}

	@Override
	protected void entityInit() {

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		long top = tag.getLong("ShooterUUID_top");
		long last = tag.getLong("ShooterUUID_last");
		String name = tag.getString("ShooterName");
		Shooter_uuid = new UUID(top,last);
		EntityPlayer shooter = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().getPlayerByUsername(name);
		if(shooter == null){
			//this.setDead();
			System.out.println("ロード時にプレイヤーが見つかりませんでいた");
		}else{
			Shooter = shooter;
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		//System.out.println("save"+Shooter.getName()+Shooter_uuid);
		tag.setLong("ShooterUUID_top",Shooter_uuid.getMostSignificantBits());
		tag.setLong("ShooterUUID_last",Shooter_uuid.getLeastSignificantBits());
		tag.setString("ShooterName",Shooter.getName());
	}

}
