package handler.client;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public class HideItemRender extends TileEntityItemStackRenderer {

	@Override
	public void renderByItem(ItemStack p_192838_1_, float partialTicks) {
	System.out.println("RENDER!!!!!!!!");
	}
}
