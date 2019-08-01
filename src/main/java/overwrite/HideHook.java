package overwrite;

import handler.client.HideGunRender;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;

public class HideHook {

	public static void test(RenderLivingBase<EntityLivingBase> render) {
		render.addLayer(new HideGunRender(render));
	}
}
