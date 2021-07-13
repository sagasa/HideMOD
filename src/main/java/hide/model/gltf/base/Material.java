package hide.model.gltf.base;

public class Material implements IDisposable {

	private MetallicRoughness pbrMetallicRoughness;
	private NormalTextureInfo normalTexture;
	private OcclusionTextureInfo occlusionTexture;
	private TextureInfo emissiveTexture;
	private float[] emissiveFactor;
	private AlphaMode alphaMode;
	private float alphaCutoff;
	private boolean doubleSided;

	public static class MetallicRoughness {
		private float[] baseColorFactor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		private TextureInfo baseColorTexture;
		private float metallicFactor;
		private float roughnessFactor;
		private TextureInfo metallicRoughnessTexture;
	}

	public static class TextureInfo {
		private int index;
		private int texCoord;

		transient private int texID;

		public void regiser() {

		}
	}

	public static class NormalTextureInfo extends TextureInfo {
		private float scale;
	}

	public static class OcclusionTextureInfo extends TextureInfo {
		private float strength;
	}

	public Material regiser() {
		if(pbrMetallicRoughness.baseColorTexture!=null)
			pbrMetallicRoughness.baseColorTexture.regiser();


		return this;
	}
	@Override
	public void dispose() {

	}
	public enum AlphaMode {
		OPAQUE, MASK, BLEND
	}


}
