package types;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
    	//参照されたリソースが存在するかの指定。
    	Pattern itemModel = Pattern.compile("^models\\/item\\/");
    	Pattern json = Pattern.compile("\\.json$");
    	if (resource.getResourcePath().startsWith("models/item/")) {
    		String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
    		//銃なら
    		if(PackLoader.GUN_DATA_MAP.containsKey(registerName)){
    			return makeGunModel(PackLoader.GUN_DATA_MAP.get(registerName).getDataString(GunDataList.ICON), true);
    		}
		}
		return null;
    }

    /**Jsonの内容！！！*/
    private InputStream makeGunModel(String texture,boolean hasModel){
    	String data;
    	if(hasModel){
    		data = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"hidemod:items/"+texture+"\"},\"display\":{\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0,0,0]},\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[0,0,0]}}}";
    	}else{
    		data = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"hidemod:items/"+texture+"\"},\"display\":{\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[1.7,1.7,1.7]}}}";
    	}
    	return new ByteArrayInputStream(data.getBytes());
    }

    @Override
    public boolean resourceExists(ResourceLocation resource) {
        //参照されたリソースが存在するかの指定。
    	Pattern itemModel = Pattern.compile("^models\\/item\\/");
    	Pattern json = Pattern.compile("\\.json$");
    	if (resource.getResourcePath().startsWith("models/item/")) {
    		String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
    		//銃なら
    		if(PackLoader.GUN_DATA_MAP.containsKey(registerName)){
    			return true;
    		}
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
