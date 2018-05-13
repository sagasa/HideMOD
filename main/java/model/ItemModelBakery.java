package model;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.ISmartBlockModel;
import net.minecraftforge.client.model.ISmartItemModel;

import javax.vecmath.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ItemModelBakery implements IFlexibleBakedModel, ISmartItemModel {

	 //Textureの保持。
    private TextureAtlasSprite stone;
    //BakedQuadを作るためのクラスを保持。
    private FaceBakery faceBakery = new FaceBakery();

    public ItemModelBakery(Function<ResourceLocation,TextureAtlasSprite> bakedTextureGetter){
        //ResourceLocationをTextureに変換する。
        stone = bakedTextureGetter.apply(new ResourceLocation("blocks/stone"));
    }

   /**
    ItemStackの状況でItemのModelの形状を変えるときに用いる。
    NBTは知っての通りいろいろ詰め込めるのでこのメソッドの使い方は大変重要。
    今回は、NBTやMetadataに変更がないのでそのまま返す。

    @see ItemStack#getTagCompound
    @see ISmartItemModel
    */
   public IFlexibleBakedModel handleItemState(ItemStack stack){
       return this;
   }

   /**
    面が不透明なBlockに接していないときのみ描画する面を指定する。
    必要な時しか描画しないのでgetGeneralQuadsで指定するより大幅に軽量化できる。
    が、万能ではないので注意。
    もちろん、一つの面につき一つのQuadのような制約はない。
    今回は、目一杯1Block分の範囲を使っているのでここで全て指定している。

    @see #getGeneralQuads
    */
   public List<BakedQuad> getFaceQuads(EnumFacing face){
       //面の始点
       Vector3f from = new Vector3f(0, 0, 0);

       //面の終点
       Vector3f to = new Vector3f(8, 16, 8);

       //TextureのUVの指定
       BlockFaceUV uv = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);

       //面の描画の設定、ほぼ使用されないと思われる。
       //第一引数:cullface(使用されない)
       //第二引数:tintindex兼layer兼renderPass
       //第三引数:テクスチャの場所(使用されない)
       //第四引数:TextureのUVの指定
       BlockPartFace partFace = new BlockPartFace(face, face.getIndex(), new ResourceLocation("blocks/stone").toString(), uv);

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
       BakedQuad bakedQuad = faceBakery.makeBakedQuad(from, to, partFace, stone, face, ModelRotation.X0_Y0, null, true, true);

       return Collections.emptyList();
   }

   /**
    面が不透明なBlockに接しているか否かを問わず、描画する面を指定する。
    見ての通り引数にEnumFacingはないので、このメソッドの中でfor(EnumFacing facing : EnumFacing.values())などしてあげる必要あり。
    もちろん、一つの面につき一つのQuadのような制約はない。
    今回は、全てgetFaceQuadsで指定するので空Listを返す。

    @see #getFaceQuads
    */
   public List<BakedQuad> getGeneralQuads(){
	   List<BakedQuad> quads = new ArrayList<BakedQuad>();

	 //面の始点
       Vector3f from = new Vector3f(0, 0, 0);

       //面の終点
       Vector3f to = new Vector3f(16, 16, 8);

       //TextureのUVの指定
       BlockFaceUV uv = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);

       //面の描画の設定、ほぼ使用されないと思われる。
       //第一引数:cullface(使用されない)
       //第二引数:tintindex兼layer兼renderPass
       //第三引数:テクスチャの場所(使用されない)
       //第四引数:TextureのUVの指定
       BlockPartFace partFace = new BlockPartFace(EnumFacing.DOWN, EnumFacing.DOWN.getIndex(), new ResourceLocation("blocks/stone").toString(), uv);

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
       quads.add(faceBakery.makeBakedQuad(from, to, partFace, stone, EnumFacing.DOWN, ModelRotation.X0_Y0, null, true, true));
       return Collections.emptyList();
   }


   /**
    BakedQuadのVertexの書式を指定するっぽい？
    使用されている痕跡がないので取り敢えずデフォルトらしきものを返す。
    */
   public VertexFormat getFormat(){
       return Attributes.DEFAULT_BAKED_FORMAT;
   }

   /**
    光を通すかどうか。
    trueなら、光を通さず影が生じる。
    */
   public boolean isAmbientOcclusion(){
       return true;
   }

   /**
    GUI内で3D描画するかどうか。
    Blockなのでtrue。
    */
   public boolean isGui3d(){
       return false;
   }

   /**
    通常、falseである。
    */
   public boolean isBuiltInRenderer(){
       return false;
   }

   /**
    パーティクルに使われる。
    Randomを通せば全部の面をパーティクルにのせるとかもできるかもしれない。
    が、Modelを代表したTextureとして求められる場合が多そうなので、普通に返したほうが無難。
    */
   public TextureAtlasSprite getTexture(){
       return stone;
   }

   /**
    非推奨メソッド。デフォルトを返しておけば間違いはないはず。
    */
   public ItemCameraTransforms getItemCameraTransforms(){
       return ItemCameraTransforms.DEFAULT;
   }
}

