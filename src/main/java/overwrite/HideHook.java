package overwrite;

import handler.client.HideGunRender;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HideHook {

	@SideOnly(Side.CLIENT)
	public static void hookOnMakeLivingRender(RenderLivingBase<EntityLivingBase> render) {
		render.addLayer(new HideGunRender(render));
	}

	@SideOnly(Side.CLIENT)
	public static void hookOnSetAngle(ModelBiped model, Entity entity) {
		if (ItemGun.isGun(((EntityLivingBase) entity).getHeldItemMainhand())) {
			model.bipedRightArm.rotateAngleY = -0.1F + model.bipedHead.rotateAngleY;
			model.bipedLeftArm.rotateAngleY = 0.1F + model.bipedHead.rotateAngleY + 0.4F;
			model.bipedRightArm.rotateAngleX = -((float) Math.PI / 2F) + model.bipedHead.rotateAngleX;
			model.bipedLeftArm.rotateAngleX = -((float) Math.PI / 2F) + model.bipedHead.rotateAngleX;

		}
		//	System.out.println("呼び出し！！！！！！！！！！！！！！");
	}

	@SideOnly(Side.CLIENT)
	public static boolean hookOnLeftClick(Minecraft mc) {
		return ItemGun.isGun(mc.player.getHeldItemMainhand());
		//	System.out.println("呼び出し！！！！！！！！！！！！！！");
	}
}
