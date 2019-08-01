package handler.client;

import items.ItemGun;
import model.HideModel;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import pack.PackData;

public class HideGunRender implements LayerRenderer<EntityLivingBase>{

	 protected final RenderLivingBase<?> livingEntityRenderer;

	    public HideGunRender(RenderLivingBase<?> livingEntityRendererIn)
	    {
	        this.livingEntityRenderer = livingEntityRendererIn;
	    }

	    @Override
		public void doRenderLayer(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
	    {
	        boolean flag = entitylivingbaseIn.getPrimaryHand() == EnumHandSide.RIGHT;
	        ItemStack itemstack = flag ? entitylivingbaseIn.getHeldItemOffhand() : entitylivingbaseIn.getHeldItemMainhand();
	        ItemStack itemstack1 = flag ? entitylivingbaseIn.getHeldItemMainhand() : entitylivingbaseIn.getHeldItemOffhand();

	        if (!itemstack.isEmpty() || !itemstack1.isEmpty())
	        {
	            GlStateManager.pushMatrix();

	            if (this.livingEntityRenderer.getMainModel().isChild)
	            {
	                float f = 0.5F;
	                GlStateManager.translate(0.0F, 0.75F, 0.0F);
	                GlStateManager.scale(0.5F, 0.5F, 0.5F);
	            }

	            this.renderHeldItem(entitylivingbaseIn, itemstack1, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
	            this.renderHeldItem(entitylivingbaseIn, itemstack, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
	            GlStateManager.popMatrix();
	        }
	    }

	    private void renderHeldItem(EntityLivingBase p_188358_1_, ItemStack p_188358_2_, ItemCameraTransforms.TransformType p_188358_3_, EnumHandSide handSide)
	    {
	        if (!p_188358_2_.isEmpty())
	        {
	            GlStateManager.pushMatrix();

	            if (p_188358_1_.isSneaking())
	            {
	                GlStateManager.translate(0.0F, 0.2F, 0.0F);
	            }
	            // Forge: moved this call down, fixes incorrect offset while sneaking.
	            this.translateToHand(handSide);
	            GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
	            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
	            boolean flag = handSide == EnumHandSide.LEFT;
	            GlStateManager.translate((float)(flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);


	            if (ItemGun.isGun(p_188358_2_)) {
	    			HideModel model = PackData.getModel("default_modelstg44");
	    			if (model != null) {
	    				model.render();
	    			}
	    		}

	            GlStateManager.popMatrix();
	        }
	    }

	    protected void translateToHand(EnumHandSide p_191361_1_)
	    {
	        ((ModelBiped)this.livingEntityRenderer.getMainModel()).postRenderArm(0.0625F, p_191361_1_);
	    }

	    @Override
		public boolean shouldCombineTextures()
	    {
	        return false;
	    }
}
