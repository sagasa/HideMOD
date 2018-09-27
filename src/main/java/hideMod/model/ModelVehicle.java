package hideMod.model;

import javax.script.ScriptException;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelVehicle extends ModelBase{

	@Override
	protected void scriptInit(String name) throws ScriptException {
		
	}

	@Override
	public void render(double x, double y, double z, float yaw, float pitch, float scale) {
		
	}

}
