package types;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import hideMod.loadPack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import types.GunData.GunDataList;

/**デフォルトリソースパックに割り込んでリソースを押し込む*/
public class ResourceLoader implements IResourcePack{

    @Override
    public InputStream getInputStream(ResourceLocation resource) throws IOException {
        //参照されたリソースに対し、InputStreamを返す。
    	//langファイルのダミーを作って渡す
    	if (resource.getResourcePath().equals("lang/en_US.lang")){
    		ArrayList<String> langData = new ArrayList<String>();
    		for (GunData data:loadPack.gunMap.values()){
    			langData.add("item."+data.getData(GunDataList.SHORT_NAME)+".name="+data.getData(GunDataList.DISPLAY_NAME));
    		}
    		return new ByteArrayInputStream(String.join("\n", langData).getBytes(Charset.forName("UTF-8")));
    	}
		return null;
    }

    @Override
    public boolean resourceExists(ResourceLocation resource) {
        //参照されたリソースが存在するかの指定。
    	//langを渡す
    	System.out.println("ResourceLIST : "+resource.getResourcePath());
    	if (resource.getResourcePath().equals("lang/en_US.lang")){
    		return true;
    	}
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
