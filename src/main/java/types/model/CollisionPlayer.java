package types.model;

public class CollisionPlayer {
	public HideCollision Head;
	public HideCollision Body;
	
	public CollisionPlayer(){
		this(1);//TODO 後で倍率書かなきゃ
	}
	
	public CollisionPlayer(double scale) {
		Head = new HideCollision();
		Head.addbox(-4.0F, -8.0F, -4.0F, 8, 8, 8,scale);
		Body = new HideCollision();
		//body
		Body.addbox(-4.0F, 0.0F, -2.0F, 8, 12, 4,scale);
		//arm
		Body.addbox(-3.0F, -2.0F, -2.0F, 4, 12, 4,scale);
		Body.addbox(-1.0F, -2.0F, -2.0F, 4, 12, 4,scale);
		//leg
		Body.addbox(-2.0F, 0.0F, -2.0F, 4, 12, 4,scale);
		Body.addbox(-2.0F, 0.0F, -2.0F, 4, 12, 4,scale);
	}
}
