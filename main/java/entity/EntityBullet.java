package entity;

import java.util.List;

import hideMod.loadPack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加*/
public class EntityBullet extends Entity implements IEntityAdditionalSpawnData{

	/*
	 */

	public EntityBullet(World worldIn) {super(worldIn);this.setSize(0.5F, 0.5F);}

	int life = 20;
	EntityLivingBase Shooter;

	public EntityBullet(World worldIn,EntityLivingBase shooter,float yaw, float pitch) {
		super(worldIn);

		Shooter = shooter;
		System.out.println(this.worldObj.isRemote);

		Shooter.addChatMessage(new ChatComponentText("発射"));
		//データ格納

		setLocationAndAngles(Shooter.posX, Shooter.posY+1F, Shooter.posZ, yaw, pitch);
		setPosition(posX, posY, posZ);

		this.motionX = 0;
		this.motionY = 0;
		this.motionZ = 0;

		//向いている方向に
		float spead = 4F;
		this.motionX = (double)(-MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * spead);
        this.motionZ = (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * spead);
        this.motionY = (double)(-MathHelper.sin((this.rotationPitch) / 180.0F * (float)Math.PI) * spead);

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
        	Vec3 lvo = new Vec3(posX, posY, posZ);
            Vec3 lvt = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);


        	if(life<0){
    			this.setDead();
    			System.out.println("deat");
    		}else{
    			life--;
    		}
        }else{

        	//クライアントサイド
        	//this.worldObj.spawnParticle(EnumParticleTypes.REDSTONE, this.posX, this.posY, this.posZ, 1, 1, 1, new int[0]);
        }
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
		// TODO 自動生成されたメソッド・スタブ

	}



	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompund) {
		// TODO 自動生成されたメソッド・スタブ

	}


	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		// TODO 自動生成されたメソッド・スタブ

	}

}
