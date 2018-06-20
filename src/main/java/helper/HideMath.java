package helper;

public class HideMath {
	/** 標準偏差 底 拡散*/
	public static double normal(double ex, double sd) {
		double xw = 0.0;
		double x;
		int n;
		for (n = 1; n <= 12; n++) { /* 12個の一様乱数の合計 */
			xw = xw + Math.random();
		}
		x = sd * (xw - 6.0) + ex;
		// System.out.println("calue : "+ x);
		return (x);
	}
}
