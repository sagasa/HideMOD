package hide.ux;

import hide.core.HideSubSystem.IHideSubSystem;
import hide.ux.network.PacketHideParticle;
import hide.ux.network.PacketInput;
import hide.ux.network.PacketPlaySound;
import hidemod.HideMod;
import net.minecraftforge.fml.relauncher.Side;
import network.PacketPlayerMotion;

/**サウンドパーティクルその他*/
public class HideUXSystem implements IHideSubSystem {

	@Override
	public void init(Side arg0) {
		HideMod.registerNetMsg(PacketInput.class, PacketInput.class, Side.SERVER);
		HideMod.registerNetMsg(PacketPlaySound.class, PacketPlaySound.class, Side.CLIENT);
		HideMod.registerNetMsg(PacketPlaySound.class, PacketPlaySound.class, Side.SERVER);
		HideMod.registerNetMsg(PacketPlayerMotion.class, PacketPlayerMotion.class, Side.SERVER);

		HideMod.registerNetMsg(PacketHideParticle.class, PacketHideParticle.class, Side.CLIENT);
	}

}
