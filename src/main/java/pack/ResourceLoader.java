package pack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;

import hidemod.HideMod;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

/**
 * デフォルトリソースパックに割り込んでリソースを押し込む
 */
public class ResourceLoader implements IResourcePack {

    enum OverrideResource {
        Icon("textures/", ".png", PackData.CurrentData.iconMap),
        Skin("skin/", ".png", PackData.CurrentData.textureMap),
        Scope("scopes/", ".png", PackData.CurrentData.scopeMap),
        Sound("sounds/", ".ogg", PackData.CurrentData.soundMap),
        ;

        final Pattern prefixPattern;
        final Pattern suffixPattern;
        final Map<String, byte[]> data;

        OverrideResource(String prefix, String suffix, Map<String, byte[]> map) {
            prefixPattern = Pattern.compile("^" + Pattern.quote(prefix));
            suffixPattern = Pattern.compile(Pattern.quote(suffix) + "$");
            data = map;
        }

        String getName(ResourceLocation rl){
            return suffixPattern.matcher(prefixPattern.matcher(rl.getResourcePath()).replaceAll("")).replaceAll("");
        }

        InputStream getResource(ResourceLocation rl) {
            String name = getName(rl);
            if (data.containsKey(name))
                return new ByteArrayInputStream(data.get(name));
            return null;
        }
        boolean match(ResourceLocation rl){
            return prefixPattern.matcher(rl.getResourcePath()).find();
        }
        boolean have(ResourceLocation rl){
            return match(rl)&&data.containsKey(getName(rl));
        }
    }

    @Override
    public InputStream getInputStream(ResourceLocation resource) throws IOException {
        // 参照されたリソースを渡す
        // sounds.json
        if (resource.getResourcePath().equals("sounds.json")) {
            return makeSoundJson();
        }
        for (OverrideResource entry : OverrideResource.values())
            if(entry.match(resource))
                return entry.getResource(resource);

        System.out.println("not found " + resource.getResourcePath());
        return null;
    }

    /**
     * sounds.jsonの内容
     */
    private static InputStream makeSoundJson() {
        StringBuilder sb = new StringBuilder("{");
        for (String name : PackData.CurrentData.soundMap.keySet()) {
            sb.append("\"" + name + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + name
                    + "\" ]},");
        }
        sb.append("\"" + "sample" + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + "sample"
                + "\" ]}}");
        System.out.println(sb.toString());
        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    @Override
    public boolean resourceExists(ResourceLocation resource) {

        // 参照されたリソースが存在するかの指定。
        // sounds.json
        if (resource.getResourcePath().equals("sounds.json")) {
            return true;
        }

        for (OverrideResource entry : OverrideResource.values())
            if(entry.have(resource))
                return true;

        final Pattern itemModel = Pattern.compile("^models\\/item\\/");
        final Pattern json = Pattern.compile("\\.json$");
        if (itemModel.matcher(resource.getResourcePath()).find()) {
            String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll(""))
                    .replaceAll("");
            // 銃なら
            if (PackData.CurrentData.gunDataMap.containsKey(registerName)) {
                return true;
            }
            // 弾なら
            if (PackData.CurrentData.magazineDataMap.containsKey(registerName)) {
                return true;
            }
        }
        System.out.println("ReceiveRequest : " + resource.toString()+" not found");
        return false;
    }

    @Override
    public Set getResourceDomains() {
        return ImmutableSet.of(HideMod.MOD_ID);
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "DummyResourcePack";
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer,
                                                          String metadataSectionName) throws IOException {
        return null;
    }
}
