package helper;

/**サウンドカテゴリなどの分散記述時に定数の重複を阻止*/
public class HideAdder {

	public static final HideAdder SoundCate = new HideAdder();

	byte count = 0;

	public byte getNumber() {
		count++;
		return count;
	}
}
