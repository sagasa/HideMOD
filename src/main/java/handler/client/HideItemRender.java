package handler.client;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;

public class HideItemRender extends TileEntityItemStackRenderer {

	@Override
	public void renderByItem(ItemStack item, float partialTicks) {
//		System.out.println("RENDER!!!!!!!!");
		Minecraft mc = Minecraft.getMinecraft();
		Tessellator tessellator = Tessellator.getInstance();
		Rectangle size = new Rectangle(0, 0, 1, 1);
		GL11.glPushMatrix();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(7, DefaultVertexFormats.POSITION_TEX);
		double Zlevel = 0;
		buf.pos(size.x, size.y, Zlevel ).tex(0, 0).endVertex();
		buf.pos(size.x + size.width, size.y, Zlevel).tex(1, 0).endVertex();
		buf.pos(size.x + size.width, size.y + size.height, Zlevel).tex(1, 1).endVertex();
		buf.pos(size.x, size.y + size.height, Zlevel).tex(0, 1).endVertex();
		tessellator.draw();
		GL11.glPopMatrix();
	}


	public static void register(ModelBakeEvent e) {
		e.getModelRegistry().putObject(new ModelResourceLocation("hidemod:gun", "inventory"), dummyModel);
	}

	static IBakedModel dummyModel = new IBakedModel() {

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return true;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			return Collections.emptyList();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return null;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}
	};
}
