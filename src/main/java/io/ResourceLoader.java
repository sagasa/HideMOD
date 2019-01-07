package io;

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

import hideMod.HideMod;
import hideMod.PackData;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

/** デフォルトリソースパックに割り込んでリソースを押し込む */
public class ResourceLoader implements IResourcePack {

	/** TEXTURE をテクスチャに置換して */
	private static final String NoModelJson = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"hidemod:items/TEXTURE\"},\"display\":{"
			+ "\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
			+ "\"thirdperson\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0.85,0.85,0.85]},"
			+ "\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[1.7,1.7,1.7]},"
			+ "\"firstperson\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[1.7,1.7,1.7]}"
			+ "}}";
	/** TEXTURE をテクスチャに置換して */
	private static final String HasModelJson = "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"hidemod:items/TEXTURE\"},\"display\":{"
			+ "\"thirdperson_righthand\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0,0,0]},"
			+ "\"thirdperson_lefthand\":{\"rotation\":[0,90,-35],\"translation\":[0,1.25,-3.5],\"scale\":[0,0,0]},"
			+ "\"firstperson_righthand\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[0,0,0]},"
			+ "\"firstperson_lefthand\":{\"rotation\":[0,-135,25],\"translation\":[0,4,2],\"scale\":[0,0,0]}"
			+ "}}";

	@Override
	public InputStream getInputStream(ResourceLocation resource) throws IOException {
		// 参照されたリソースを渡す
		// sounds.json
		if (resource.getResourcePath().equals("sounds.json")) {
			return makeSoundJson();
		}
		Pattern itemModel = Pattern.compile("^models\\/item\\/");
		Pattern json = Pattern.compile("\\.json$");
		if (itemModel.matcher(resource.getResourcePath()).find()) {
			String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			// 銃なら
			if (PackData.GUN_DATA_MAP.containsKey(registerName)) {
				return makeItemModel(PackData.GUN_DATA_MAP.get(registerName).ITEM_ICONNAME, true);
			}
			// 弾なら
			if (PackData.BULLET_DATA_MAP.containsKey(registerName)) {
				return makeItemModel(PackData.BULLET_DATA_MAP.get(registerName).ITEM_ICONNAME, true);
			}
		}
		Pattern itemTexture = Pattern.compile("^textures\\/items\\/");
		Pattern png = Pattern.compile("\\.png$");
		if (itemTexture.matcher(resource.getResourcePath()).find()) {
			String iconName = png.matcher(itemTexture.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.ICON_MAP.containsKey(iconName)) {
				return new ByteArrayInputStream(PackData.ICON_MAP.get(iconName));
			}
		}
		Pattern Sound = Pattern.compile("^sounds\\/");
		Pattern ogg = Pattern.compile("\\.ogg$");
		if (Sound.matcher(resource.getResourcePath()).find()) {
			String iconName = ogg.matcher(Sound.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
			if (PackData.SOUND_MAP.containsKey(iconName)) {
				return new ByteArrayInputStream(PackData.SOUND_MAP.get(iconName));
			}
		}
		return null;
	}

	/** sounds.jsonの内容 */
	private InputStream makeSoundJson() {
		StringBuilder sb = new StringBuilder("{");
		for (String name : PackData.SOUND_MAP.keySet()) {
			sb.append("\"" + name + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + name
					+ "\" ]},");
		}
		sb.append("\"" + "sample" + "\": {\"category\" : \"player\",\"sounds\" : [ \"" + HideMod.MOD_ID + ":" + "sample"
				+ "\" ]}}");
		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	/** Jsonの内容！！！ */
	private InputStream makeItemModel(String texture, boolean hasModel) {
		String data;
		if (hasModel) {
			data = HasModelJson.replace("TEXTURE", texture);
		} else {
			data = NoModelJson.replace("TEXTURE", texture);
		}
		return new ByteArrayInputStream(data.getBytes());
	}

	@Override
	public boolean resourceExists(ResourceLocation resource) {
		System.out.println("ReceiveRequest : " + resource.getResourcePath());
		// 参照されたリソースが存在するかの指定。
		// sounds.json
		if (resource.getResourcePath().equals("sounds.json")) {
			return true;
		}
		Pattern itemModel = Pattern.compile("^models\\/item\\/");
		Pattern json = Pattern.compile("\\.json$");
		if (itemModel.matcher(resource.getResourcePath()).find()) {
			String registerName = json.matcher(itemModel.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			// 銃なら
			if (PackData.GUN_DATA_MAP.containsKey(registerName)) {
				return true;
			}
			// 弾なら
			if (PackData.BULLET_DATA_MAP.containsKey(registerName)) {
				return true;
			}
		}
		Pattern itemTexture = Pattern.compile("^textures\\/items\\/");
		Pattern png = Pattern.compile("\\.png$");
		if (itemTexture.matcher(resource.getResourcePath()).find()) {
			String iconName = png.matcher(itemTexture.matcher(resource.getResourcePath()).replaceAll(""))
					.replaceAll("");
			if (PackData.ICON_MAP.containsKey(iconName) && !iconName.equals("sample")) {
				return true;
			}
		}
		Pattern Sound = Pattern.compile("^sounds\\/");
		Pattern ogg = Pattern.compile("\\.ogg$");
		if (Sound.matcher(resource.getResourcePath()).find()) {
			String iconName = ogg.matcher(Sound.matcher(resource.getResourcePath()).replaceAll("")).replaceAll("");
			if (PackData.SOUND_MAP.containsKey(iconName)) {
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
