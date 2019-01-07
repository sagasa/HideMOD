package hideMod.model;

import net.minecraft.util.math.Vec3d;

public class VertexUV extends Vec3d{
	public VertexUV(double x, double y, double z, float u, float v) {
		super(z, y, z);
		U = u;
		V = v;
	}
	public float U;
	public float V;
}