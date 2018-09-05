package playerdata;

/**銃のアップデートに関係する数値*/
public class GunState {
	public boolean stopshoot = false;
	public float shootDelay = 0;
	public int shootNum = 0;

	/**tick処理*/
	public void update(){
		if(0<shootDelay){
			shootDelay -= 1f;
		}
	}
	/**クリア 持ち替えなどのために状態を初期化する*/
	public void clear(){
		stopshoot = false;
		shootDelay = 0;
		shootNum = 0;
	}
}
