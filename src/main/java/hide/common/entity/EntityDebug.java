package hide.common.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public abstract class EntityDebug extends Entity implements IEntityAdditionalSpawnData {


    public EntityDebug(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    @SideOnly(Side.CLIENT)
    public abstract void render(EntityDebug entity, double d, double d1, double d2, float f, float f1) ;

    /** 消えるまでの時間 */
    protected float life = 20;
    public float r, g, b;

    public EntityDebug(World w, float r, float g, float b) {
        this(w);
        this.r = r;
        this.g = g;
        this.b = b;
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
        if (life < 0){
            setDead();
        }
    }
}
