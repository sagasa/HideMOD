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

	public static double completion(double old, double now, Float completion) {
		return completion == null ? now : old + (now - old) * completion;
	}

	public static float completion(float old, float now, Float completion) {
		return completion == null ? now : old + (now - old) * completion;
	}

	public static int completion(int old, int now, Float completion) {
		return completion == null ? now : (int) (old + (now - old) * completion);
	}
}
