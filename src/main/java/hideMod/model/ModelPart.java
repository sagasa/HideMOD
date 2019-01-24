package hideMod.model;

import net.minecraft.util.math.Vec3d;

public class ModelPart {
	public HidePolygon[] Polygon;

	public ModelPart(HidePolygon[] array) {
		Polygon = array;
	}

	static public class HidePolygon {
		public HidePolygon() {

		}

		public HidePolygon(VertexUV[] vert) {
			Vertex = vert;
		}

		public VertexUV[] Vertex;
	}

	static public class VertexUV extends Vec3d{
		public VertexUV(double x, double y, double z, float u, float v) {
			super(z, y, z);
			U = u;
			V = v;
		}
		public final float U;
		public final float V;
	}
}
