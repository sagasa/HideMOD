package hide.model.gltf;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hide.model.gltf.animation.Animation;
import hide.model.gltf.animation.Skin;
import hide.model.gltf.base.Accessor;
import hide.model.gltf.base.BufferView;
import hide.model.gltf.base.ByteBufferInputStream;
import hide.model.gltf.base.HideTexture;
import hide.model.gltf.base.Material;
import hide.model.gltf.base.Mesh;
import hide.opengl.ServerRenderContext;
import net.minecraft.profiler.Profiler.Result;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;

public class GltfLoader {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final int JSON_CHUNK = 0x4E4F534A;
	private static final int BIN_CHUNK = 0x004E4942;

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
		try (InputStream ins = new DataInputStream(new FileInputStream(new File(Loader.instance().getConfigDir().getParent(), "m2.glb")))) {
			test = loadGlb("test", ins, Side.SERVER);
		} catch (IOException | GltfException e) {
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("==================MODEL LOAD END================== " + time);
	}

	public static Model loadGlb(String modelname, InputStream stream, Side side) throws IOException, GltfException {

		return new GltfLoader(modelname, stream, side).res;

		//*/
	}

	private static final Material DefatultMat = new Material();

	public Mesh getMesh(int index) {
		return meshCache.get(index);
	}

	public HideNode getNode(int index) {
		return nodeCache.get(index);
	}

	public Accessor getAccessor(int index) {
		return accessors.get(index);
	}

	public Skin getSkin(int index) {
		return skins.get(index);
	}

	public Material getMaterial(int index) {
		return index == -1 ? DefatultMat : materials.get(index);
	}

	public void addSkinNode(HideNode node) {
		;
	}

	private List<Mesh> meshCache = new ArrayList<>();
	private List<HideNode> nodeCache = new ArrayList<>();
	private List<Accessor> accessors = new ArrayList<>();
	private List<Skin> skins = new ArrayList<>();
	private List<Material> materials = new ArrayList<>();

	private Model res;

	private GltfLoader(String modelname, InputStream stream, Side side) throws GltfException, IOException {
		start();
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
		binData.order(ByteOrder.nativeOrder());
		binData.put(data);
		binData.rewind();

		lap("load bin");

		// Load buffer views
		ArrayList<BufferView> bufferViews = new ArrayList<>();
		for (JsonElement element : root.get("bufferViews").getAsJsonArray()) {
			BufferView bufferView = gson.fromJson(element, BufferView.class);
			bufferView.slice(binData);
			bufferViews.add(bufferView);
		}

		lap("load buffer view");

		// Load accessors
		for (JsonElement element : root.get("accessors").getAsJsonArray()) {
			accessors.add(gson.fromJson(element, Accessor.class).register(bufferViews));
		}

		lap("load accessor");
		// Load textures

		// Client
		boolean useGL = side == Side.CLIENT || ServerRenderContext.SUPPORT_CONTEXT;

		ArrayList<HideTexture> textures = new ArrayList<>();
		if (useGL) {
			if (root.has("images")) {
				root.get("images").getAsJsonArray().forEach(imageJson -> {
					JsonObject imageObj = imageJson.getAsJsonObject();
					ByteBuffer bufferView = bufferViews.get(imageObj.get("bufferView").getAsInt()).getBuffer();
					textures.add(HideTexture.load(new ByteBufferInputStream(bufferView)));
				});
			}
		}

		if (root.has("materials"))
			for (JsonElement element : root.get("materials").getAsJsonArray()) {
				Material mat = useGL ? gson.fromJson(element, Material.class) : Material.DUMMY;
				materials.add(mat.register(textures));
				System.out.println("Add Mat");
			}
		lap("load texture");

		// Load nodes
		for (JsonElement element : root.get("nodes").getAsJsonArray()) {
			nodeCache.add(gson.fromJson(element, HideNode.class));
		}

		ArrayList<HideNode> rootNodes = new ArrayList<>();
		for (int index : gson.fromJson(scene.get("nodes"), int[].class)) {
			rootNodes.add(getNode(index));
		}

		// Load skins
		if (root.has("skins"))
			for (JsonElement element : root.get("skins").getAsJsonArray()) {
				skins.add(gson.fromJson(element, Skin.class).register(this));
			}

		lap("load skins");
		// Load meshes
		for (JsonElement element : root.get("meshes").getAsJsonArray()) {
			System.out.println(element);
			meshCache.add(gson.fromJson(element, Mesh.class).register(this));
		}

		lap("load mesh");

		// Load animations
		HashMap<String, Animation> animations = new HashMap<>();
		if (root.has("animations")) {
			for (JsonElement element : root.get("animations").getAsJsonArray()) {
				JsonObject object = element.getAsJsonObject();
				String name = getName(object, "default");
				animations.put(name, gson.fromJson(element, Animation.class).register(this));
			}
		}

		// register nodes
		for (HideNode node : nodeCache) {
			node.register(this);
		}

		lap("load end");
		res = new Model(nodeCache, rootNodes, animations, materials);

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
