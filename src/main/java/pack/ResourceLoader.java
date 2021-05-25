package pack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;

import hide.types.items.ItemData;
import hidemod.HideMod;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

/** デフォルトリソースパックに割り込んでリソースを押し込む */
public class ResourceLoader implements IResourcePack {

	/** TEXTURE をテクスチャに置換して */
	private static final String NoModelJson = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"TEXTURE\"},\"display\":{"
			+ "\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
			+ "\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
			+ "\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[1.7,1.7,1.7]},"
			+ "\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[1.7,1.7,1.7]}"
			+ "}}";
	/** TEXTURE をテクスチャに置換して */
	private static final String HasModelJson = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"TEXTURE\"},\"display\":{"
			+ "\"thirdperson_righthand\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0,0,0]},"
			+ "\"thirdperson_lefthand\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0,0,0]},"
			+ "\"firstperson_righthand\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[0,0,0]},"
			+ "\"firstperson_lefthand\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[0,0,0]}"
			+ "}}";

	@Override
	public InputStream getInputStream(ResourceLocation resource) throws IOException {
		// 参照されたリソースを渡す
		System.out.println("useResource " + resource.getResourcePath());
		// sounds.json
		if (resource.getResourcePath().equals("sounds.json")) {
			return makeSoundJson();
		}
		final Pattern itemModel = Pattern.compile("^models\\/item\\/");
		final Pattern json = Pattern.compile("\\.json$");
		//*
		if (itemModel.matcher(resource.getResourcePath()).find()) {
			String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");

			return new ByteArrayInputStream(NoModelJson.replace("TEXTURE", HideMod.MOD_ID + ":default_m14_scope").getBytes());
			/*
			// 銃なら
			if (PackData.getGunData(registerName)!=null) {
				return makeItemModel(PackData.getGunData(registerName));
			}
			// 弾なら
			if (PackData.getBulletData(registerName)!=null) {
				return makeItemModel(PackData.getBulletData(registerName));
			}
			*/
		}
		//*/
		final Pattern itemTexture = Pattern.compile("^textures\\/");
		final Pattern png = Pattern.compile("\\.png$");
		if (itemTexture.matcher(resource.getResourcePath()).find()) {
			String iconName = png.matcher(itemTexture.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.ICON_MAP.containsKey(iconName)) {
				return new ByteArrayInputStream(PackData.currentData.ICON_MAP.get(iconName));
			}
		}
		final Pattern modelSkin = Pattern.compile("^skin\\/");
		if (modelSkin.matcher(resource.getResourcePath()).find()) {
			String skinName = png.matcher(modelSkin.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.TEXTURE_MAP.containsKey(skinName)) {
				return new ByteArrayInputStream(PackData.currentData.TEXTURE_MAP.get(skinName));
			}
		}
		final Pattern scopeTex = Pattern.compile("^scopes\\/");
		if (scopeTex.matcher(resource.getResourcePath()).find()) {
			String name = png.matcher(scopeTex.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.SCOPE_MAP.containsKey(name)) {
				return new ByteArrayInputStream(PackData.currentData.SCOPE_MAP.get(name));
			}
		}
		final Pattern Sound = Pattern.compile("^sounds\\/");
		final Pattern ogg = Pattern.compile("\\.ogg$");
		if (Sound.matcher(resource.getResourcePath()).find()) {
			String iconName = ogg.matcher(Sound.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
			if (PackData.currentData.SOUND_MAP.containsKey(iconName)) {
				return new ByteArrayInputStream(PackData.currentData.SOUND_MAP.get(iconName));
			}
		}
		return null;
	}

	/** sounds.jsonの内容 */
	private static InputStream makeSoundJson() {
		StringBuilder sb = new StringBuilder("{");
		for (String name : PackData.currentData.SOUND_MAP.keySet()) {
			sb.append("\"" + name + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + name
					+ "\" ]},");
		}
		sb.append("\"" + "sample" + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + "sample"
				+ "\" ]}}");
		System.out.println(sb.toString());
		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	/** Jsonの内容！！！ */
	public static InputStream makeItemModel(ItemData item) {
		String data;
		if (PackData.getModel(item.get(ItemData.ModelName)) != null) {
			data = HasModelJson.replace("TEXTURE", item.get(ItemData.IconName));
		} else {
			data = NoModelJson.replace("TEXTURE", item.get(ItemData.IconName));
		}
		return new ByteArrayInputStream(data.getBytes());
	}

	@Override
	public boolean resourceExists(ResourceLocation resource) {
		System.out.println("ReceiveRequest : " + resource.toString());
		// 参照されたリソースが存在するかの指定。
		// sounds.json
		if (resource.getResourcePath().equals("sounds.json")) {
			return true;
		}
		final Pattern itemModel = Pattern.compile("^models\\/item\\/");
		final Pattern json = Pattern.compile("\\.json$");
		if (itemModel.matcher(resource.getResourcePath()).find()) {
			String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			// 銃なら
			if (PackData.currentData.GUN_DATA_MAP.containsKey(registerName)) {
				return true;
			}
			// 弾なら
			if (PackData.currentData.MAGAZINE_DATA_MAP.containsKey(registerName)) {
				return true;
			}
		}
		final Pattern itemTexture = Pattern.compile("^textures\\/");
		final Pattern png = Pattern.compile("\\.png$");
		if (itemTexture.matcher(resource.getResourcePath()).find()) {
			String iconName = png.matcher(itemTexture.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.ICON_MAP.containsKey(iconName) && !iconName.equals("sample")) {
				return true;
			}
		}
		final Pattern scopeTex = Pattern.compile("^scopes\\/");
		if (scopeTex.matcher(resource.getResourcePath()).find()) {
			String name = png.matcher(scopeTex.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.SCOPE_MAP.containsKey(name)) {
				return true;
			}
		}
		final Pattern modelSkin = Pattern.compile("^skin\\/");
		if (modelSkin.matcher(resource.getResourcePath()).find()) {
			String skinName = png.matcher(modelSkin.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.currentData.TEXTURE_MAP.containsKey(skinName)) {
				return true;
			}
		}
		final Pattern Sound = Pattern.compile("^sounds\\/");
		final Pattern ogg = Pattern.compile("\\.ogg$");
		if (Sound.matcher(resource.getResourcePath()).find()) {
			String name = ogg.matcher(Sound.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
			if (PackData.currentData.SOUND_MAP.containsKey(name)) {
				return true;
			}
		}
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
