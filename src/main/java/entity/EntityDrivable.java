package entity;

import java.util.List;

import guns.CommonGun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityDrivable extends Entity {

	//=======================
	// 実装してほしいです

	public List<CommonGun> guns;
	//=======================

    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(EntityDrivable.class, DataSerializers.FLOAT);

    private float acceleration = 0F;
    private float maxSpeed = 20F;
    private float accelerationAmplifier = 5F;
    private float rotate;
    private float rotateAmplifier = 5F;


    public EntityDrivable(World worldIn)
    {
        super(worldIn);
        this.preventEntitySpawning = true;
        //this.setSize(0.98F, 0.7F);
    }

    public EntityDrivable(World worldIn, double x, double y, double z)
    {
        this(worldIn);
        this.setPosition(x, y, z);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
    }

    public boolean processInitialInteract(EntityPlayer player, EnumHand hand)
    {
        if (super.processInitialInteract(player, hand)) return true;

        if (player.isSneaking()) {
            return false;
        }
        else if (this.isBeingRidden()) {
            return true;
        }
        else
        {
            if (!this.world.isRemote)
            {
                player.startRiding(this);
            }

            return true;
        }
    }

    @Override
    public void onUpdate() {
        double speed = Math.sqrt(Math.pow(this.motionX, 2) + Math.pow(this.motionY, 2) + Math.pow(this.motionZ, 2));

        if (speed < 0.05F) this.motionX = this.motionY = this.motionZ = 0F;

        if (!this.isBeingRidden()) {
            this.acceleration = 0F;
            this.rotate = 0F;
        }

        travel();



    }

    public void travel(){
        Vec3d look_vec = getLookVec().normalize();
        //double norm = Math.sqrt(Math.pow(look_vec.x, 2) + Math.pow(look_vec.y, 2) + Math.pow(look_vec.z, 2));
        double speed = Math.sqrt(Math.pow(this.motionX, 2) + Math.pow(this.motionY, 2) + Math.pow(this.motionZ, 2));
        if (speed < maxSpeed) {
            this.motionX += look_vec.x * this.acceleration * this.accelerationAmplifier;
            this.motionY += look_vec.y * this.acceleration * this.accelerationAmplifier;
            this.motionZ += look_vec.z * this.acceleration * this.accelerationAmplifier;
        } else {
            this.motionX = look_vec.scale(maxSpeed).x;
            this.motionY = look_vec.scale(maxSpeed).y;
            this.motionZ = look_vec.scale(maxSpeed).z;
        }

        if (acceleration == 0F) {
            this.motionX = 0.99F * this.motionX;
            this.motionY = 0.99F * this.motionY;
            this.motionZ = 0.99F * this.motionZ;
        }

        if (rotate != 0F) {
            this.rotationYaw += this.rotate * this.rotateAmplifier;
        }

    }

    /* TODO:hp追加 */


    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!this.world.isRemote && !this.isDead) {
            this.removePassengers();
            this.setDead();
            // TODO: 今の状態だと一撃で壊れる
        }
        return true;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(DAMAGE, Float.valueOf(0.0F));
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    public void setAcceleration(float acceleration){
        this.acceleration = acceleration;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }
}