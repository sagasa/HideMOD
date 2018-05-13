package types;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import hideMod.PackLoader;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import types.guns.GunData;
import types.guns.GunData.GunDataList;

/**デフォルトリソースパックに割り込んでリソースを押し込む*/
public class ResourceLoader implements IResourcePack{

    @Override
    public InputStream getInputStream(ResourceLocation resource) throws IOException {
        //参照されたリソースに対し、InputStreamを返す。
    	//langファイルのダミーを作って渡す
    	if (resource.getResourcePath().equals("lang/en_US.lang")){
    		ArrayList<String> langData = new ArrayList<String>();
    		for (GunData data:PackLoader.GUN_DATA_MAP.values()){
    			langData.add("item."+data.getDataString(GunDataList.SHORT_NAME)+".name="+data.getDataString(GunDataList.DISPLAY_NAME));
    		}
    		return new ByteArrayInputStream(String.join("\n", langData).getBytes(Charset.forName("UTF-8")));
    	}
    /*	//銃のテクスチャ
    	if (resource.getResourcePath().startsWith("gun_",12)) {
    		return ClassLoader.getSystemResourceAsStream("assets/hidemod/models/item/gunItem.json");
		}*/
		return null;
    }

    @Override
    public boolean resourceExists(ResourceLocation resource) {
        //参照されたリソースが存在するかの指定。
    	//langを渡す
    	System.out.println("ResourceLIST : " + resource.getResourceDomain() + " / "+resource.getResourcePath());
    	if (resource.getResourcePath().equals("lang/en_US.lang")){
    		return true;
    	}
   /* 	if (resource.getResourcePath().startsWith("gun_",12)) {
    		return true;
		}*/
        return false;
    }

    @Override
    public Set getResourceDomains() {
        return ImmutableSet.of("hidemod");
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer par1MetadataSerializer, String par2Str){
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "DummyResourcePack";
    }
}
