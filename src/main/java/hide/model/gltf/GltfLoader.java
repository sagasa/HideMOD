package hide.model.gltf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import hide.model.impl.AccessorImpl;
import hide.model.impl.BufferViewImpl;
import hide.model.impl.IAnimation;
import hide.model.impl.IMaterial;
import hide.model.impl.ISkin;
import hide.model.impl.MeshImpl;
import hide.model.impl.MeshImpl.Attribute;
import hide.model.impl.MeshPrimitivesImpl;
import hide.model.impl.ModelImpl;
import hide.model.impl.NodeImpl;
import hide.model.util.BufferUtil;
import hide.model.util.ByteBufferInputStream;
import hide.model.util.HideTexture;
import hide.opengl.ServerRenderContext;
import net.minecraft.profiler.Profiler.Result;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class GltfLoader {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final int JSON_CHUNK = 0x4E4F534A;
	private static final int BIN_CHUNK = 0x004E4942;

	static ModelImpl test;

	public static void render() {

		GL11.glPushMatrix();
		for (int i = 0; i < 1; i++) {
			//test.render();
			GL11.glTranslatef(10, 0, 0);
		}
		GL11.glPopMatrix();

		for (Result res : ModelImpl.profiler.getProfilingData("hide.render")) {
			//System.out.println(res.profilerName + " " + res.totalUsePercentage + " " + res.usePercentage);
		}

	}

	public static void test() {
		System.out.println("==================MODEL LOAD TEST==================");
		long time = System.currentTimeMillis();
//		try (InputStream ins = new DataInputStream(new FileInputStream(new File(Loader.instance().getConfigDir().getParent(), "m2.glb")))) {
//			test = load(ins);
//		} catch (IOException | GltfException e) {
//			e.printStackTrace();
//		}

		//		try (InputStream ins = new DataInputStream(new FileInputStream(new File(Loader.instance().getConfigDir().getParent(), "ModelBAR.obj")))) {
		//			test = ObjLoader.load(ins);
		//		} catch (Throwable e) {
		//			e.printStackTrace();
		//		}
		time = System.currentTimeMillis() - time;
		System.out.println("==================MODEL LOAD END================== " + time);
	}

	//=== パース用クラス ===
	static class Accessor extends AccessorImpl {

		private int bufferView;

		public Accessor register(ArrayList<BufferViewImpl> bufArray) {
			buffer = bufArray.get(bufferView);
			return this;
		}
	}

	static class BufferView extends BufferViewImpl {

		public BufferView slice(ByteBuffer from) {
			buffer = (ByteBuffer) from.slice().position(byteOffset)
					.limit(byteOffset + byteLength).mark();
			buffer.order(ByteOrder.nativeOrder());
			return this;
		}
	}

	static class MeshPrimitives extends MeshPrimitivesImpl {
		@SerializedName("attributes")
		private Map<String, Integer> attributeIndex;
		@SerializedName("indices")
		private int indicesIndex;
		@SerializedName("material")
		private int materialIndex = -1;
		@SerializedName("targets")
		transient private Map<Attribute, Integer>[] targetsIndex;

		private void register(GltfLoader loader) {

			material = loader.getMaterial(materialIndex);
			indices = loader.getAccessor(indicesIndex);
			attributes = new EnumMap<>(Attribute.class);
			attributeIndex.forEach((att, index) -> {
				Attribute attribute = Attribute.valueOf(att);
				if (attribute != null)
					attributes.put(attribute, loader.getAccessor(index));
			});

			boolean hasTarget = targetsIndex != null && targetsIndex.length != 0;

			//モーフィングがあるなら
			if (hasTarget) {
				targets = new ArrayList<>();
				target = new EnumMap<>(Attribute.class);
				for (Map<Attribute, Integer> src : targetsIndex) {
					Map<Attribute, AccessorImpl> map = new EnumMap<>(Attribute.class);
					for (Entry<Attribute, Integer> entry : src.entrySet()) {
						map.put(entry.getKey(), loader.getAccessor(entry.getValue()));
					}
					targets.add(map);
				}
			}
		}
	}

	static class Mesh extends MeshImpl {

		private MeshPrimitives[] primitives;

		@Override
		protected MeshPrimitivesImpl[] getPrimitives() {
			return primitives;
		}

		public Mesh register(GltfLoader loader) {
			for (MeshPrimitives meshPrimitives : primitives) {
				meshPrimitives.register(loader);
			}
			return this;
		}
	}

	static class Node extends NodeImpl {

		@SerializedName("children")
		private int[] childrenIndex = ArrayUtils.EMPTY_INT_ARRAY;
		@SerializedName("skin")
		private int skinIndex = -1;
		@SerializedName("mesh")
		private int meshIndex = -1;
		private String name;

		public Node register(GltfLoader loader) {
			children = new Node[childrenIndex.length];
			for (int i = 0; i < childrenIndex.length; i++) {
				Node child = loader.getNode(childrenIndex[i]);
				child.parent = this;
				children[i] = child;
			}

			mesh = loader.getMesh(meshIndex);
			skin = loader.getSkin(skinIndex);
			return this;
		}
	}

	static class Skin implements ISkin {
		@SerializedName("inverseBindMatrices")
		private int inverseBindMatricesIndex;
		@SerializedName("joints")
		private int[] jointsIndex;

		transient private AccessorImpl inverseBindMatrices;
		transient private NodeImpl[] joints;

		public Skin register(GltfLoader loader) {
			inverseBindMatrices = loader.getAccessor(inverseBindMatricesIndex);
			joints = new NodeImpl[jointsIndex.length];
			for (int i = 0; i < joints.length; i++) {
				joints[i] = loader.getNode(jointsIndex[i]);

			}
			return this;
		}

		@Override
		public AccessorImpl getInverseBindMatrices() {
			return inverseBindMatrices;
		}

		@Override
		public NodeImpl[] getJoints() {
			return joints;
		}
	}

	static class Model extends ModelImpl {

		public Model(List<? extends NodeImpl> nodes, List<NodeImpl> rootNodes, HashMap<String, IAnimation> animations, List<? extends IMaterial> materials) {
			this.nodes = Collections.unmodifiableList(nodes);
			this.animations = animations;

			for (NodeImpl node : nodes)
				if (node.hasSkin())
					skinRoot.add(node);

			for (NodeImpl node : rootNodes)
				if (node.hasMesh())
					meshRoot.add(node);
				else
					debugRoot.add(node);

			for (IAnimation hideAnimation : animations.values()) {
				hideAnimation.apply(0.5f);
			}
		}

		float anim = 0;

		@Override
		public void render() {
			for (IAnimation hideAnimation : animations.values()) {
				hideAnimation.apply(anim);
			}
			anim += 0.001f;
			anim %= 1;
			super.render();
		}

	}

	//=== 読み込み ===
	public static ModelImpl load(InputStream stream) throws IOException, GltfException {
		return new GltfLoader(stream).res;
	}

	private static final Material DefatultMat = new Material();

	Mesh getMesh(int index) {
		if (index == -1)
			return null;
		return meshCache.get(index);
	}

	Node getNode(int index) {
		if (index == -1)
			return null;
		return nodeCache.get(index);
	}

	Accessor getAccessor(int index) {
		if (index == -1)
			return null;
		return accessors.get(index);
	}

	Skin getSkin(int index) {
		if (index == -1)
			return null;
		return skins.get(index);
	}

	Material getMaterial(int index) {
		return index == -1 ? DefatultMat : materials.get(index);
	}

	private List<Mesh> meshCache = new ArrayList<>();
	private List<Node> nodeCache = new ArrayList<>();
	private List<Accessor> accessors = new ArrayList<>();
	private List<Skin> skins = new ArrayList<>();
	private List<Material> materials = new ArrayList<>();

	private ModelImpl res;
	private static final boolean useGL = FMLCommonHandler.instance().getSide().isClient() || ServerRenderContext.SUPPORT_CONTEXT;

	private GltfLoader(InputStream stream) throws GltfException, IOException {
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

		//System.out.println(gson.toJson(root));

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

		ByteBuffer binData = BufferUtil.createByteBuffer(data.length);
		binData.order(ByteOrder.nativeOrder());
		binData.put(data);
		binData.rewind();

		lap("load bin");

		// Load buffer views
		ArrayList<BufferViewImpl> bufferViews = new ArrayList<>();
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
			nodeCache.add(gson.fromJson(element, Node.class));
		}

		ArrayList<NodeImpl> rootNodes = new ArrayList<>();
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
		HashMap<String, IAnimation> animations = new HashMap<>();
		if (root.has("animations")) {
			for (JsonElement element : root.get("animations").getAsJsonArray()) {
				JsonObject object = element.getAsJsonObject();
				String name = getName(object, "default");
				animations.put(name, gson.fromJson(element, Animation.class).register(this));
			}
		}

		// register nodes
		for (Node node : nodeCache) {
			node.register(this);
		}

		lap("load end");
		res = new Model(nodeCache, rootNodes, animations, materials);
		res.postInit();
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
