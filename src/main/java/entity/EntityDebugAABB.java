package entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityDebugAABB extends Entity implements IEntityAdditionalSpawnData {

	public EntityDebugAABB(World worldIn) {
		super(worldIn);
		this.setSize(0.5F, 0.5F);
	}

	/** 消えるまでの時間 */
	private float life = 20;
	public float r, g, b;
	public AxisAlignedBB aabb = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

	public EntityDebugAABB(World w, AxisAlignedBB aabb, float r, float g, float b) {
		this(w);
		this.aabb = aabb;
		this.r = r;
		this.g = g;
		this.b = b;
		setPosition(aabb.minX, aabb.minY, aabb.minZ);
	}

	@Override
	public boolean isInRangeToRender3d(double x, double y, double z) {
		double d0 = this.posX - x;
		double d2 = this.posZ - z;
		double d3 = d0 * d0 + d2 * d2;
		return this.isInRangeToRenderDist(d3);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 800;
	}

	@Override
	public void onUpdate() {
		life--;
		if (life < 0)
			setDead();
	}

	@Override
	protected void entityInit() {

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {

	}

	@Override
	public void writeSpawnData(ByteBuf buf) {
		buf.writeDouble(aabb.minX);
		buf.writeDouble(aabb.minY);
		buf.writeDouble(aabb.minZ);
		buf.writeDouble(aabb.maxX);
		buf.writeDouble(aabb.maxY);
		buf.writeDouble(aabb.maxZ);
	}

	@Override
	public void readSpawnData(ByteBuf buf) {
		aabb = new AxisAlignedBB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
	}
}
