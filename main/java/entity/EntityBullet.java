package entity;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.core.net.DatagramSocketManager;

import hideMod.loadPack;
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
	EntityLivingBase Shooter;
	UUID Shooter_uuid;
	RayTracer RayTracer;

	public EntityBullet(World worldIn,EntityLivingBase shooter,float yaw, float pitch) {
		this(worldIn);

		Shooter = shooter;
		Shooter_uuid = shooter.getUniqueID();
		//System.out.println(this.worldObj.isRemote);

		//Shooter.addChatMessage(new ChatComponentText("発射"));
		//データ格納

		setLocationAndAngles(Shooter.posX, Shooter.posY+1.65F, Shooter.posZ, yaw, pitch);
		setPosition(posX, posY, posZ);

		this.motionX = 0;
		this.motionY = 0;
		this.motionZ = 0;

		//向いている方向に
		float spead = 1F;
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch)) * spead;
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch)) * spead;
		motionY = -Math.sin(Math.toRadians(rotationPitch)) * spead;

	}

	@Override
	public void onUpdate() {
		this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;

        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);



        if(!this.worldObj.isRemote){
        	//サーバーサイド
        	Vec3 lvo = new Vec3(lastTickPosX, lastTickPosY, lastTickPosX);
            Vec3 lvt = new Vec3(posX, posY, posZ);
            	DamageSource damagesource = new EntityDamageSource("bullet",Shooter).setDamageBypassesArmor();
            	for(Entity e: RayTracer.getHitEntity(this,worldObj, lvo, lvt)){
            		System.out.println(e);
            		if(e!=null){
            			//無敵時間を殺す
            			e.attackEntityFrom(damagesource, 1);
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
        if(life<tick){
			this.setDead();
        }
        tick++;
	}


	@Override
	public void writeSpawnData(ByteBuf buffer) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	protected void entityInit() {

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		long top = tag.getLong("ShooterUUID_top");
		long last = tag.getLong("ShooterUUID_last");
		String name = tag.getString("ShooterName");
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
		UUID uuid = Shooter.getUniqueID();
		tag.setLong("ShooterUUID_top",uuid.getMostSignificantBits());
		tag.setLong("ShooterUUID_last",uuid.getLeastSignificantBits());
		tag.setString("ShooterName",Shooter.getName());
	}

}
