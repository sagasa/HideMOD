package hide.common.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class EntityDebugLine extends EntityDebug{

    private List<Vec3d> line;
    @Override
    public void render(EntityDebug entity, double d, double d1, double d2, float f, float f1) {
        EntityDebugLine entityLine = (EntityDebugLine) entity;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.getBuffer();
        GL11.glLineWidth(3);

        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (Vec3d vec:entityLine.line){
            buf.pos(vec.x, vec.y, vec.z).color(r, g, b, 0.5f).endVertex();
        }
        tessellator.draw();
    }

    public EntityDebugLine(World w){
        super(w);
    }

    public EntityDebugLine(World w,List<Vec3d> line, float r, float g, float b) {
        super(w, r, g, b);
        this.line = line;
        if(line.size()==0)
            setPosition(line.get(0).x, line.get(0).y, line.get(0).z);
    }

    @Override
    public void writeSpawnData(ByteBuf buf) {
        buf.writeInt(line.size());
        for (Vec3d pos:line) {
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
        }
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        final int size = buf.readInt();
        line = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            line.add(new Vec3d(buf.readDouble(),buf.readDouble(),buf.readDouble()));
        }
        r = buf.readFloat();
        g = buf.readFloat();
        b = buf.readFloat();
    }
}
