package types;

/**装填済みのマガジン管理用*/
public class LoadedMagazine{
	public String name;
	public int num;
	public LoadedMagazine(String bulletName,int bulletNum) {
		name = bulletName;
		num = bulletNum;
	}
	@Override
	public String toString() {
		return super.toString()+name+" "+num;
	}
}