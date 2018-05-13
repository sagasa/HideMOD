package types.render;


/**レンダー用x y z u v格納*/
public class VertexUV {
	public double X;
	public double Y;
	public double Z;

	public double U;
	public double V;
	public VertexUV(double x,double y,double z,double u,double v) {
		X = x;
		Y = y;
		Z = z;
		U = u;
		V = v;
	}

}
