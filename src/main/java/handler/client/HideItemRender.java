package handler.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import hidemod.HideMod;
import model.HideModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pack.PackData;
import types.items.ItemData;

@SideOnly(Side.CLIENT)
public class HideItemRender extends TileEntityItemStackRenderer {

	@Override
	public void renderByItem(ItemStack item, float partialTicks) {
		//		System.out.println("RENDER!!!!!!!!");
		/*	Minecraft mc = Minecraft.getMinecraft();
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
			GL11.glPopMatrix();//*/
	}

	public static void register(ModelBakeEvent e) {
		//e.getModelRegistry().putObject(new ModelResourceLocation("hidemod:gun", "inventory"), dummyModel);
		System.out.println("call Bake Event");

		/*	((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
				@Override
				public void onResourceManagerReload(IResourceManager resourceManager) {
					System.out.println("call Reload Event "+resourceManager.getResourceDomains());

				}
			});//*/
	}

	public static void registerLoader() {
		System.out.println("Register ModelLoader");
		ModelLoaderRegistry.registerLoader(loader);
	}

	static ItemData testItem = new ItemData() {
	};

	private static IModel HideItemModel = new IModel() {
		@Override
		public Collection<ResourceLocation> getTextures() {
			return ImmutableSet.of(new ResourceLocation(HideMod.MOD_ID, "default_m14_scope"));
		}

		@Override
		public IBakedModel bake(IModelState state, VertexFormat format,
				Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
			stone = Minecraft.getMinecraft().getTextureMapBlocks()
					.getAtlasSprite(new ResourceLocation(HideMod.MOD_ID, "default_m14_scope").toString());
			testItem.ITEM_ICONNAME = "default_m14_scope";
			return getHideItemModel(testItem);
		}
	};

	private static ICustomModelLoader loader = new ICustomModelLoader() {

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {

		}

		@Override
		public IModel loadModel(ResourceLocation modelLocation) throws Exception {
			return HideItemModel;
		}

		@Override
		public boolean accepts(ResourceLocation modelLocation) {
			return modelLocation.getResourceDomain().equals(HideMod.MOD_ID);
		}
	};

	static ItemOverrideList override = new ItemOverrideList(new ArrayList<ItemOverride>()) {
		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, net.minecraft.world.World world,
				net.minecraft.entity.EntityLivingBase entity) {
			ItemData data = PackData.getItemData(stack);
			if (data != null) {
				return getHideItemModel(data);
			}
			return super.handleItemState(originalModel, stack, world, entity);
		}
	};

	static List<BakedQuad> model;
	private static FaceBakery faceBakery = new FaceBakery();
	static TextureAtlasSprite stone;

	Map<String, IBakedModel> bakedModel;
	private static final String SimpleIconModel = "{'parent':'item/generated','textures':{'layer0':'ICON'},'display':{'thirdperson_righthand':{'rotation':[0,-90,45],'translation':[0,1,-2],'scale':[1,1,1]},'firstperson_righthand':{'rotation':[0,-90,45],'translation':[1.13,3.2,1.13],'scale':[0.68,0.68,0.68]}}}".replaceAll("'", "\"");
	private static final String SimpleModel = "{'parent':'item/generated'}".replaceAll("'", "\"");
	private static final ModelBlock DummyModel = ModelBlock.deserialize(SimpleModel);
	private static final ItemModelGenerator itemmodelGen = new ItemModelGenerator();
	private static IBakedModel getHideItemModel(ItemData data) {
		String icon = data.ITEM_ICONNAME;
		HideModel model = PackData.getModel(data.ITEM_MODELNAME);
		if (model == null) {

		}
		return bakeModel(HideMod.MOD_ID + ":default_m14_scope");
	}

	protected static IBakedModel bakeModel(String textureLoc) {
		TextureAtlasSprite textureatlassprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(textureLoc);

		ModelBlock itemModel = itemmodelGen.makeItemModel(Minecraft.getMinecraft().getTextureMapBlocks(), ModelBlock.deserialize(SimpleIconModel.replace("ICON", textureLoc)));
		itemModel = ModelBlock.deserialize(SimpleIconModel.replace("ICON", textureLoc));


		SimpleBakedModel.Builder simplebakedmodel$builder = (new SimpleBakedModel.Builder(itemModel, ItemOverrideList.NONE)).setTexture(textureatlassprite);
		System.out.println("bake");
		for (BlockPart blockpart : itemModel.getElements()) {
			for (EnumFacing enumfacing : blockpart.mapFaces.keySet()) {
				BlockPartFace blockpartface = blockpart.mapFaces.get(enumfacing);
				System.out.println(enumfacing+" "+blockpartface.texture+" "+blockpart.positionFrom+" "+blockpart.positionTo);
				simplebakedmodel$builder.addGeneralQuad(makeBakedQuad(blockpart, blockpartface,
						textureatlassprite, enumfacing, ModelRotation.X0_Y0, false));
			}
		}
		return simplebakedmodel$builder.makeBakedModel();
	}

	protected static BakedQuad makeBakedQuad(BlockPart p_177589_1_, BlockPartFace p_177589_2_,
			TextureAtlasSprite p_177589_3_,
			EnumFacing p_177589_4_, net.minecraftforge.common.model.ITransformation p_177589_5_, boolean p_177589_6_) {
		return faceBakery.makeBakedQuad(p_177589_1_.positionFrom, p_177589_1_.positionTo, p_177589_2_, p_177589_3_,
				p_177589_4_, p_177589_5_, p_177589_1_.partRotation, p_177589_6_, p_177589_1_.shade);
	}

	static IBakedModel dummyModel = new IBakedModel() {

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			if (cameraTransformType == TransformType.GUI) {
				return testModel.handlePerspective(cameraTransformType);
			}
			return IBakedModel.super.handlePerspective(cameraTransformType);
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing face, long rand) {
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

	static IBakedModel testModel = new IBakedModel() {

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return false;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing face, long rand) {
			if (face != EnumFacing.DOWN)
				return Collections.emptyList();
			//面の始点
			Vector3f from = new Vector3f(0, 0, 0);

			//面の終点
			Vector3f to = new Vector3f(16, 16, 16);

			//TextureのUVの指定
			BlockFaceUV uv = new BlockFaceUV(new float[] { 0.0F, 0.0F, 16.0F, 16.0F }, 0);

			//面の描画の設定、ほぼ使用されないと思われる。
			//第一引数:cullface(使用されない)
			//第二引数:tintindex兼layer兼renderPass
			//第三引数:テクスチャの場所(使用されない)
			//第四引数:TextureのUVの指定
			BlockPartFace partFace = new BlockPartFace(face, face.getIndex(),
					new ResourceLocation("blocks/stone").toString(), uv);

			//Quadの設定
			//第一引数:面の始点
			//第二引数:面の終点
			//第三引数:面の描画の設定
			//第四引数:テクスチャ
			//第五引数:面の方向
			//第六引数:モデルの回転
			//第七引数:面の回転(nullで自動)
			//第八引数:モデルの回転に合わせてテクスチャを回転させるか
			//第九引数:陰らせるかどうか
			BakedQuad bakedQuad = faceBakery.makeBakedQuad(from, to, partFace, stone, face, ModelRotation.X90_Y0, null,
					true,
					true);

			return Lists.newArrayList(bakedQuad);
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