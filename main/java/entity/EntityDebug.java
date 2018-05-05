package entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityDebug extends Entity{

	int life;
	public EntityDebug(World worldIn, Vec3 pos) {
		super(worldIn);
		setLocationAndAngles(pos.xCoord, pos.yCoord , pos.zCoord, 0, 0);
		setPosition(posX, posY, posZ);
		life = 600;
	}

	@Override
	public void onUpdate() {
		life --;
		if(life<0){
			setDead();
		}
	}

	@Override
	protected void entityInit() {


	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompund) {


	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {


	}

}
