package hide.model.gltf;

import java.util.ArrayList;

import hide.model.impl.IMaterial;
import hide.model.util.HideTexture;

class Material implements IMaterial {

	private MetallicRoughness pbrMetallicRoughness = MetallicRoughness.DEFAULT;
	private NormalTextureInfo normalTexture = NormalTextureInfo.DEFAULT;
	private OcclusionTextureInfo occlusionTexture = OcclusionTextureInfo.DEFAULT;
	private TextureInfo emissiveTexture = TextureInfo.DEFAULT;
	private float[] emissiveFactor = new float[4];
	private AlphaMode alphaMode = AlphaMode.BLEND;
	private float alphaCutoff;
	private boolean doubleSided = false;

	@Override
	public int getBaseColorTexture() {
		return pbrMetallicRoughness.baseColorTexture.texID;
	}

	public static final Material DUMMY = new Material();

	public static class MetallicRoughness {
		private static final MetallicRoughness DEFAULT = new MetallicRoughness();

		private float[] baseColorFactor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		private TextureInfo baseColorTexture = TextureInfo.DEFAULT;
		private float metallicFactor = 0;
		private float roughnessFactor = 0;
		private TextureInfo metallicRoughnessTexture = TextureInfo.DEFAULT;
	}

	public static class TextureInfo {
		private static final TextureInfo DEFAULT = new TextureInfo();

		private int index = -1;
		private int texCoord;

		transient private int texID = 0;

		public void regiser(ArrayList<HideTexture> textures) {
			if (index != -1)
				texID = textures.get(index).getTexID();
		}
	}

	public static class NormalTextureInfo extends TextureInfo {
		private static final NormalTextureInfo DEFAULT = new NormalTextureInfo();
		private float scale = 1;
	}

	public static class OcclusionTextureInfo extends TextureInfo {
		private static final OcclusionTextureInfo DEFAULT = new OcclusionTextureInfo();
		private float strength = 1;
	}

	@Override
	public void dispose() {

	}

	public enum AlphaMode {
		OPAQUE, MASK, BLEND
	}

	public Material register(ArrayList<HideTexture> textures) {
		pbrMetallicRoughness.baseColorTexture.regiser(textures);
		pbrMetallicRoughness.metallicRoughnessTexture.regiser(textures);
		normalTexture.regiser(textures);
		occlusionTexture.regiser(textures);
		emissiveTexture.regiser(textures);
		return this;
	}

}
