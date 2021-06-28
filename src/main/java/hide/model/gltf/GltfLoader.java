package hide.model.gltf;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfReaderV2;
import de.javagl.jgltf.model.v2.GltfModelV2;
import hide.model.gltf.base.ByteBufferInputStream;
import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.profiler.Profiler.Result;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GltfLoader {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final int JSON_CHUNK = 0x4E4F534A;
	private static final int BIN_CHUNK = 0x004E4942;
	private static final GltfReaderV2 reader = new GltfReaderV2();

	static Model test;

	public static void render() {

		GL11.glPushMatrix();
		for (int i = 0; i < 1; i++) {
			test.render();
			GL11.glTranslatef(10, 0, 0);
		}
		GL11.glPopMatrix();


		for (Result res : Model.profiler.getProfilingData("hide")) {
			//System.out.println(res.profilerName+" "+res.totalUsePercentage+" "+res.usePercentage);
		}

	}

	public static void test() {
		System.out.println("==================MODEL LOAD TEST==================");
		long time = System.currentTimeMillis();
		try (InputStream ins = new DataInputStream(new FileInputStream(new File(Loader.instance().getConfigDir().getParent(), "test.glb")))) {
			test = loadGlb("test", ins, Side.SERVER);
		} catch (IOException | GltfException e) {
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("==================MODEL LOAD END================== " + time);
	}

	public static Model loadGlb(String modelname, InputStream stream, Side side) throws IOException, GltfException {
		start();

		//;

		int magic = readUnsignedInt(stream);
		int version = readUnsignedInt(stream);
		int length = readUnsignedInt(stream);

		if (magic != 0x46546C67)
			throw new IllegalArgumentException("file is not GLB");
		if (version != 2)
			throw new IllegalArgumentException("GLB File is not v2");

		// First chunk is always JSON
		int chunkLength = readUnsignedInt(stream);
		int chunkType = readUnsignedInt(stream);
		byte[] data = new byte[chunkLength];
		if (stream.read(data, 0, chunkLength) != chunkLength) {
			throw new IOException("Failed to read file");
		}
		if (chunkType != JSON_CHUNK) {
			throw new IOException("first chunk is not json");
		}

		GlTF gltf = reader.read(new ByteArrayInputStream(data));
		JsonParser parser = new JsonParser();
		JsonObject root = parser.parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

		//System.out.println(gson.toJson(root));

		lap("load json");

		// Load BIN data
		chunkLength = readUnsignedInt(stream);
		chunkType = readUnsignedInt(stream);
		data = new byte[chunkLength];
		if (stream.read(data, 0, chunkLength) != chunkLength) {
			throw new IOException("Failed to read GLB file");
		}
		if (chunkType != BIN_CHUNK) {
			throw new IOException("Expected BIN data but didn't get it");
		}

		ByteBuffer binData = BufferUtils.createByteBuffer(data.length);
		binData.put(data);
		binData.rewind();

		GltfModel model = new GltfModelV2(new GltfAssetV2(gltf, binData));

		return new Model(model);
		/*
		meshCache.clear();
		nodeCache.clear();
		accessors.clear();

		lap("init stream");
		int magic = readUnsignedInt(stream);
		int version = readUnsignedInt(stream);
		int length = readUnsignedInt(stream);

		if (magic != 0x46546C67)
			throw new IllegalArgumentException("file is not GLB");
		if (version != 2)
			throw new IllegalArgumentException("GLB File is not v2");

		// First chunk is always JSON
		int chunkLength = readUnsignedInt(stream);
		int chunkType = readUnsignedInt(stream);
		byte[] data = new byte[chunkLength];
		if (stream.read(data, 0, chunkLength) != chunkLength) {
			throw new IOException("Failed to read file");
		}
		if (chunkType != JSON_CHUNK) {
			throw new IOException("first chunk is not json");
		}

		lap("load json");

		JsonParser parser = new JsonParser();
		JsonObject root = parser.parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

		System.out.println(gson.toJson(root));

		// Get scene
		if (!root.has("scenes")) {
			throw new GltfException("scenes not found");
		}
		JsonObject scene = root.getAsJsonArray("scenes").get(root.has("scene") ? root.get("scene").getAsInt() : 0)
				.getAsJsonObject();

		// Load BIN data
		chunkLength = readUnsignedInt(stream);
		chunkType = readUnsignedInt(stream);
		data = new byte[chunkLength];
		if (stream.read(data, 0, chunkLength) != chunkLength) {
			throw new IOException("Failed to read GLB file");
		}
		if (chunkType != BIN_CHUNK) {
			throw new IOException("Expected BIN data but didn't get it");
		}

		ByteBuffer binData = BufferUtils.createByteBuffer(data.length);
		binData.put(data);
		binData.rewind();

		lap("load bin");

		// Load buffer views
		ArrayList<BufferView> bufferViews = new ArrayList<>();
		for (JsonElement element : root.get("bufferViews").getAsJsonArray()) {
			BufferView bufferView = gson.fromJson(element, BufferView.class);
			bufferView.setData((ByteBuffer) binData.slice().position(bufferView.byteOffset)
					.limit(bufferView.byteOffset + bufferView.byteLength));
			bufferViews.add(bufferView);
		}

		lap("load buffer view");

		// Load accessors
		for (JsonElement element : root.get("accessors").getAsJsonArray()) {
			accessors.add(gson.fromJson(element, Accessor.class).register(bufferViews));
		}

		lap("load accessor");

		// Load meshes
		for (JsonElement meshJson : root.get("meshes").getAsJsonArray()) {
			ArrayList<Geometry> geometries = new ArrayList<>();
			for (JsonElement primitive : meshJson.getAsJsonObject().get("primitives").getAsJsonArray()) {
				geometries.add(loadPrimitive(gson.fromJson(primitive, MeshPrimitive.class)));
			}
			meshCache.add(new Mesh(geometries));
		}

		lap("load mesh");

		// Load textures
		ArrayList<ResourceLocation> textures = new ArrayList<>();
		// Client
		if (side == Side.CLIENT) {
			if (root.has("images")) {
				root.get("images").getAsJsonArray().forEach(imageJson -> {
					JsonObject imageObj = imageJson.getAsJsonObject();
					ByteBuffer bufferView = bufferViews.get(imageObj.get("bufferView").getAsInt()).getData();
					String mimeType = imageObj.get("mimeType").getAsString();
					String name = getName(imageObj, "Image_" + textures.size());
					textures.add(registerTexture(bufferView, modelname));
				});
			} else {
				// todo default texture
				textures.add(RESOURCE_LOCATION_EMPTY);
			}
		}

		lap("load texture");

		// Load nodes
		int[] sceneNodes = gson.fromJson(scene.get("nodes"), int[].class);
		JsonArray nodeJsonArray = root.get("nodes").getAsJsonArray();
		ArrayList<Node> rootNodes = new ArrayList<>();
		for (int index : sceneNodes) {
			rootNodes.add(loadNode(nodeJsonArray, index));
		}

		// Load animations
		HashMap<String, Animation> animations = new HashMap<>();
		if (root.has("animations")) {
			for (JsonElement element : root.get("animations").getAsJsonArray()) {
				JsonObject object = element.getAsJsonObject();
				String name = getName(object, "default");
				ArrayList<Sampler> samplers = gson.fromJson(object.get("samplers"), new TypeToken<ArrayList<Sampler>>() {
				}.getType());
				ArrayList<Channel> channels = new ArrayList<>();

				for (JsonElement channelElement : object.get("channels").getAsJsonArray()) {
					JsonObject channelObject = channelElement.getAsJsonObject();
					JsonObject targetObject = channelObject.get("target").getAsJsonObject();
					Sampler sampler = samplers.get(channelObject.get("sampler").getAsInt());
					System.out.println(accessors.get(sampler.input).getComponentType());

					Accessor ac = accessors.get(sampler.input);
					ByteBuffer bb = accessors.get(sampler.input).getBufferView().getData();
					FloatBuffer fb = accessors.get(sampler.input).getBufferView().getData().asFloatBuffer();

					System.out.println(accessors.get(sampler.input).getBufferView() + " " + bb);
					for (int i = 0; i < ac.getCount(); i++) {
						System.out.println(i + " " + " " + bb.getFloat());

					}
					bb.rewind();
					channels.add(new Channel(
							sampler,
							accessors.get(sampler.input),
							accessors.get(sampler.output),
							nodeCache.get(targetObject.get("node").getAsInt()),
							gson.fromJson(targetObject.get("path"), AnimationPath.class)));
				}
				animations.put(name, new Animation(channels));
			}
		}
		lap("load end");
		return new Model(new ArrayList<>(nodeCache.values()), rootNodes, animations, textures);
		//*/
	}

	private static final ResourceLocation RESOURCE_LOCATION_EMPTY = new ResourceLocation("");

	/**Client側処理*/
	@SideOnly(Side.CLIENT)
	private static ResourceLocation registerTexture(ByteBuffer data, String name) {
		try (ByteBufferInputStream is = new ByteBufferInputStream(data)) {

			DynamicTexture texture = new DynamicTexture(TextureUtil.readBufferedImage(is));
			lap("make dynamic texture");
			// TODO we're assuming that we have one texture, and giving it the same name/id
			// as the main model file. This is the same one as defined in the part
			// definition file
			ResourceLocation texResLoc = new ResourceLocation(HideMod.MOD_ID, name);

			Minecraft.getMinecraft().getTextureManager().loadTexture(texResLoc, texture);

			return texResLoc;
		} catch (IOException e) {
			//Tails.LOGGER.error("Failed to load texture " + name, e);
		}
		return RESOURCE_LOCATION_EMPTY;
	}

	/*
		private static Node loadNode(JsonArray nodeJsonArray, int index) {
			if (nodeCache.containsKey(index)) {
				return nodeCache.get(index);
			}
			JsonObject nodeJson = nodeJsonArray.get(index).getAsJsonObject();
			Node node;
			ArrayList<Node> children = null;

			// Load children if there any
			if (nodeJson.has("children")) {
				int[] childrenIndexes = gson.fromJson(nodeJson.get("children"), int[].class);
				children = new ArrayList<>(childrenIndexes.length);
				for (int childIndex : childrenIndexes) {
					children.add(loadNode(nodeJsonArray, childIndex));
				}
			}

			// Load matrix, or TSR values
			if (nodeJson.has("matrix")) {
				node = new Node(children, gson.fromJson(nodeJson.get("matrix"), float[].class));
			} else {
				float[] translation = new float[] { 0, 0, 0 };
				float[] rotation = new float[] { 0, 0, 0, 1 };
				float[] scale = new float[] { 1, 1, 1 };
				if (nodeJson.has("translation")) {
					translation = gson.fromJson(nodeJson.get("translation"), float[].class);
				}
				if (nodeJson.has("rotation")) {
					rotation = gson.fromJson(nodeJson.get("rotation"), float[].class);
				}
				if (nodeJson.has("scale")) {
					scale = gson.fromJson(nodeJson.get("scale"), float[].class);
				}
				node = new Node(children, translation, rotation, scale);
			}

			// NOTE: depends on meshes being preloaded into the cache
			if (nodeJson.has("mesh")) {
				node.setMesh(meshCache.get(nodeJson.get("mesh").getAsInt()));
			}

			if (nodeJson.has("weights")) {
				node.weights = gson.fromJson(nodeJson.get("weights"), float[].class);
			}

			node.name = getName(nodeJson, "default");
			nodeCache.put(index, node);

			return node;
		}

		private static String getName(JsonObject object, String defaultName) {
			if (object.has("name")) {
				return object.get("name").getAsString();
			}
			return defaultName;
		}



		private static float readFloat(ByteBuffer buf) throws IOException {
			int arg0 = buf.get();
			int arg1 = buf.get();
			int arg2 = buf.get();
			int arg3 = buf.get();

			return Float.intBitsToFloat((arg0 << 0) + (arg1 << 8) + (arg2 << 16) + (arg3 << 24));
		}

		private static Geometry loadPrimitive(MeshPrimitive primitive) {
			Geometry geometry = new Geometry(primitive.mode.gl);

			for (Entry<Attribute, Integer> attribute : primitive.attributes.entrySet()) {
				Accessor accessor = GltfLoader.accessors.get(attribute.getValue());
				//int itemBytes = accessor.type.size * accessor.componentType.size;
				geometry.setBuffer(attribute.getKey(), accessor);
			}

			if (primitive.indices != null) {
				Accessor accessor = GltfLoader.accessors.get(primitive.indices);
				geometry.setIndices(accessor);
			}

			return geometry;
		}

	//*/
	private static int readUnsignedInt(InputStream stream) throws IOException {
		int arg0 = stream.read();
		int arg1 = stream.read();
		int arg2 = stream.read();
		int arg3 = stream.read();
		if ((arg0 | arg1 | arg2 | arg3) < 0) {
			throw new EOFException();
		} else {
			return (arg0 << 0) + (arg1 << 8) + (arg2 << 16) + (arg3 << 24);
		}
	}

	public static class GltfException extends Throwable {
		private static final long serialVersionUID = -7696024338732078517L;

		public GltfException(String str) {
			super(str);
		}
	}

	static long time;

	private static void start() {
		time = System.currentTimeMillis();
	}

	private static void lap(String name) {
		System.out.println("Time:" + (System.currentTimeMillis() - time) + " " + name);
		time = System.currentTimeMillis();
	}
}
