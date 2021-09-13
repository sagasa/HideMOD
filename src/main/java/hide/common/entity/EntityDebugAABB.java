package hide.common.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import org.lwjgl.opengl.GL11;

public class EntityDebugAABB extends EntityDebug {


	/** 消えるまでの時間 */
	public AxisAlignedBB aabb;

	public EntityDebugAABB(World w){
		super(w);
	}

	public EntityDebugAABB(World w, AxisAlignedBB aabb, float r, float g, float b) {
		super(w, r, g, b);
		this.aabb = aabb;
		setPosition(aabb.minX, aabb.minY, aabb.minZ);
	}

	@Override
	public void render(EntityDebug entity, double d, double d1, double d2, float f, float f1) {
		EntityDebugAABB aabb = (EntityDebugAABB) entity;
		// GL11.glColor4ub(1,0, 0, 0.2F);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		GL11.glLineWidth(3);
		//RenderGlobal.drawBoundingBox(0, 0, 0, aabb.aabb.maxX - aabb.aabb.minX, aabb.aabb.maxY - aabb.aabb.minY, aabb.aabb.maxZ - aabb.aabb.minZ, aabb.r, aabb.g, aabb.b, 0.5f);

		buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
		RenderGlobal.addChainedFilledBoxVertices(buf, 0, 0, 0, aabb.aabb.maxX - aabb.aabb.minX, aabb.aabb.maxY - aabb.aabb.minY, aabb.aabb.maxZ - aabb.aabb.minZ, aabb.r, aabb.g, aabb.b, 0.2f);
		tessellator.draw();
	}

	@Override
	public void writeSpawnData(ByteBuf buf) {
		buf.writeDouble(aabb.minX);
		buf.writeDouble(aabb.minY);
		buf.writeDouble(aabb.minZ);
		buf.writeDouble(aabb.maxX);
		buf.writeDouble(aabb.maxY);
		buf.writeDouble(aabb.maxZ);
		buf.writeFloat(r);
		buf.writeFloat(g);
		buf.writeFloat(b);
	}

	@Override
	public void readSpawnData(ByteBuf buf) {
		aabb = new AxisAlignedBB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
		r = buf.readFloat();
		g = buf.readFloat();
		b = buf.readFloat();
	}
}
