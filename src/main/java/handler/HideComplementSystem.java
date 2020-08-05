package handler;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**プレイヤー用補完システム*/
public class HideComplementSystem {

	private static final int MAX_COMP_TICK = 10;
	private int p = 0;
	private Vec3d[] compArray = new Vec3d[MAX_COMP_TICK];

	private Vec3d getVec(int length) {
		//最大補完にクランプ マイナスに
		length = -MathHelper.clamp(length, 0, MAX_COMP_TICK);
		length += p;
		if (length < 0) {
			length += MAX_COMP_TICK;
		}
		return compArray[length];
	}

	/**指定Tick前のVecを返す*/
	public Vec3d getCompVec(float tick) {
		float f = tick % 1;
		//ピッタリの数値があるなら
		if (f == 0f)
			return getVec(Math.round(tick));

		Vec3d prev = getVec(MathHelper.ceil(tick));
		Vec3d next = getVec(MathHelper.floor(tick));
		return prev.add(next.subtract(prev).scale(f));
	}

	public void update(Vec3d vec) {
		if (MAX_COMP_TICK <= p)
			p = 0;
		compArray[p] = vec;
		p++;
	}
}
