package hide.guns;

public class ShootPoints {
	public static class ShootPoint {
		public float x = 0;
		public float y = 0;
		public float z = 0;
		public float yaw = 0;
		public float pitch = 0;
	}

	public static final ShootPoints DefaultShootPoint;

	static {
		DefaultShootPoint = new ShootPoints();
		DefaultShootPoint.points = new ShootPoint[1][1];
		DefaultShootPoint.points[0][0] = new ShootPoint();
	}

	public ShootPoint[][] points;
}
