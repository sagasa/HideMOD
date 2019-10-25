package types.base;

public class ValueChange extends DataBase{

	public String PATH = "";

	public Operater OPERATER = Operater.Set;

	public Object VALUE = null;

	public enum Operater{
		Set,Add,Multiply,ListAdd,ListRemove
	}
}
