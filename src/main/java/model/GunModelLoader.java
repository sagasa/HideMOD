package model;

import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Function;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IModelState;

public class GunModelLoader  implements IModel{
    /**
    先に読み込まれていてほしいModelのLocationを返すっぽい。
    今回は、特に親とするModelはないので空Listを返す。
    */
   public Collection<ResourceLocation> getDependencies(){
       return Collections.emptyList();
   }

   /**
    Modelに必要なTextureを返す。
    といっても、バニラのTextureは特に指定しなくてもよく、独自にTextureを用意するときのみ返す。
    今回は、バニラの石テクスチャを用いるので空Listを返す。
    */
   public Collection<ResourceLocation> getTextures(){
       return Collections.emptyList();
   }

   /**
    Modelを焼成する。
    IModelで最も重要な部分。
    ここで返したいBakedModelを返せれば後はなんとかなる。
    */
   public IFlexibleBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation,TextureAtlasSprite> bakedTextureGetter){
       return new ItemModelBakery(bakedTextureGetter);
   }

   /**
    上のbakeメソッドに渡されるModelStateのデフォルト。
    現在のバニラの実装でNPEが出る箇所はないが、念の為にダミーを返す。
    */
   public IModelState getDefaultState(){
       return ModelRotation.X0_Y0;
   }
}