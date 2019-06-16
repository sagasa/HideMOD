package guns;

import net.minecraft.nbt.NBTTagCompound;
import types.items.GunData;

public interface IGuns {
	public GunData getGunData();

	public NBTTagCompound getGunTag();
}
