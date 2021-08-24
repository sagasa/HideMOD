package hide.model.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

import hide.model.impl.AccessorImpl;
import hide.model.impl.AccessorImpl.ComponentType;
import hide.model.impl.AccessorImpl.ElementType;
import hide.model.impl.BufferViewImpl;
import hide.model.impl.IMaterial;
import hide.model.impl.MeshImpl;
import hide.model.impl.MeshImpl.Attribute;
import hide.model.impl.MeshPrimitivesImpl;
import hide.model.impl.ModelImpl;
import hide.model.impl.NodeImpl;
import hide.model.util.BufferUtil;
import hidemod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ObjLoader {
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Streamからモデルを返す
	 * @throws IOException
	 */
	public static ModelImpl load(InputStream model) throws Throwable {

		List<Vector3f> position = new ArrayList<>();
		List<Vector2f> texCoords = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();

		List<HideVertex> verts = new ArrayList<>();

		List<Integer> index = new ArrayList<>();

		Vector3f vec0 = new Vector3f();
		Vector3f vec1 = new Vector3f();

		BufferedReader reader = new BufferedReader(new InputStreamReader(model, Charset.forName("UTF-8")));
		String group = null;
		String line;
		while ((line = reader.readLine()) != null) {
			String[] split = WHITE_SPACE.split(line, 2);
			if (split.length != 2) {
				continue;
			}
			String key = split[0];
			String data = split[1];
			String[] splitData = WHITE_SPACE.split(data);
			if (key.equalsIgnoreCase("g")) {
				group = data.trim();
			} else if (key.equalsIgnoreCase("v")) {
				position.add(parseVec3f(splitData));
			} else if (key.equalsIgnoreCase("vt")) {
				texCoords.add(parseVec2f(splitData));
			} else if (key.equalsIgnoreCase("vn")) {
				normals.add(parseVec3f(splitData));
			} else if (key.equalsIgnoreCase("f")) {
				if (group == null) {
					LOGGER.warn("グループに属していません");
					continue;
				}
				if (splitData.length < 3) {
					LOGGER.warn("頂点数が足りません");
					continue;
				}

				final int start = verts.size();
				final int count = splitData.length;

				for (String str : splitData) {
					String[] pts = str.split("/");
					int pos = Integer.parseInt(pts[0]) - 1;
					int tex = pts.length < 2 || Strings.isNullOrEmpty(pts[1]) ? -1 : Integer.parseInt(pts[1]) - 1;
					int norm = pts.length < 3 || Strings.isNullOrEmpty(pts[2]) ? -1 : Integer.parseInt(pts[2]) - 1;
					verts.add(new HideVertex(pos, tex, norm));
				}

				//3角に変換
				for (int i = 0; i < count - 2; i++) {
					HideVertex v0 = verts.get(start);
					HideVertex v1 = verts.get(start + i + 1);
					HideVertex v2 = verts.get(start + i + 2);

					index.add(start);
					index.add(start + i + 1);
					index.add(start + i + 2);

					//ノーマル自動計算
					if (v0.norm == -1 || v1.norm == -1 || v2.norm == -1) {
						Vector3f p0 = position.get(v0.pos);
						Vector3f p1 = position.get(v1.pos);
						Vector3f p2 = position.get(v2.pos);

						vec0.sub(p0, p1);
						vec1.sub(p2, p1);

						Vector3f norm = new Vector3f();

						norm.cross(vec0, vec1);
						norm.normalize();

						int newNorm = normals.size();
						boolean use = false;

						if (v0.norm != -1) {
							Vector3f vec = normals.get(v0.norm);
							vec.add(vec, norm);
						} else {
							v0.norm = newNorm;
							use = true;
						}

						if (v1.norm != -1) {
							Vector3f vec = normals.get(v1.norm);
							vec.add(vec, norm);
						} else {
							v1.norm = newNorm;
							use = true;
						}

						if (v2.norm != -1) {
							Vector3f vec = normals.get(v2.norm);
							vec.add(vec, norm);
						} else {
							v2.norm = newNorm;
							use = true;
						}

						if (use)
							normals.add(norm);
					}

					//テクスチャパディング
					if (v0.tex == -1 || v1.tex == -1 || v2.tex == -1) {
						Vector2f p0 = texCoords.get(v0.tex);
						Vector2f p1 = texCoords.get(v1.tex);
						Vector2f p2 = texCoords.get(v2.tex);

						v0.tex = v1.tex = v2.tex = texCoords.size();
						texCoords.add(new Vector2f(0, 0));
					}
				}
			}
		}

		//ノーマルのノーマライズ
		normals.forEach(v -> {
			v.normalize();
		});

		// フォーマットをまとめる
		ByteBuffer posByteBuf = BufferUtil.createByteBuffer(verts.size() * 4 * 3);
		ByteBuffer texByteBuf = BufferUtil.createByteBuffer(verts.size() * 4 * 2);
		ByteBuffer normByteBuf = BufferUtil.createByteBuffer(verts.size() * 4 * 3);
		ByteBuffer indexByteBuf = BufferUtil.createByteBuffer(index.size() * 4);

		Vector3f center = new Vector3f();
		verts.forEach(v -> {
			Vector3f pos = position.get(v.pos);
			posByteBuf.putFloat(pos.x);
			posByteBuf.putFloat(pos.y);
			posByteBuf.putFloat(pos.z);
			center.add(center, pos);

			Vector2f tex = texCoords.get(v.tex);
			texByteBuf.putFloat(tex.x);
			texByteBuf.putFloat(tex.y);

			Vector3f norm = normals.get(v.norm);
			normByteBuf.putFloat(norm.x);
			normByteBuf.putFloat(norm.y);
			normByteBuf.putFloat(norm.z);
		});

		center.x = center.x / position.size();
		center.y = center.y / position.size();
		center.z = center.z / position.size();

		index.forEach(v -> {
			indexByteBuf.putInt(v);
		});

		posByteBuf.rewind();
		texByteBuf.rewind();
		normByteBuf.rewind();
		indexByteBuf.rewind();

		List<Node> list = new ArrayList<>();
		list.add(new Node(posByteBuf, texByteBuf, normByteBuf, indexByteBuf));
		return new Model(list).postInit();
	}

	static class Accessor extends AccessorImpl {

		public Accessor(ComponentType component, ElementType element, ByteBuffer buf) {
			buffer = new BufferView(buf);
			componentType = component;
			elementType = element;
			byteOffset = 0;
			count = buffer.getByteLength() / componentType.size / elementType.size;
		}
	}

	static class BufferView extends BufferViewImpl {
		public BufferView(ByteBuffer buf) {
			buffer = buf;
			byteLength = buf.capacity();
			byteOffset = byteStride = 0;
		}
	}

	static class MeshPrimitives extends MeshPrimitivesImpl {
		public MeshPrimitives(ByteBuffer pos, ByteBuffer tex, ByteBuffer norm, ByteBuffer index) {
			attributes.put(Attribute.POSITION, new Accessor(ComponentType.FLOAT, ElementType.VEC3, pos));
			attributes.put(Attribute.TEXCOORD_0, new Accessor(ComponentType.FLOAT, ElementType.VEC2, tex));
			attributes.put(Attribute.NORMAL, new Accessor(ComponentType.FLOAT, ElementType.VEC3, norm));
			indices = new Accessor(ComponentType.UNSIGNED_INT, ElementType.SCALAR, index);
		}
	}

	static class Mesh extends MeshImpl {

		private MeshPrimitives[] primitives;

		public void setMaterial(IMaterial mat) {
			for (MeshPrimitives primitive : primitives) {
				primitive.setMaterial(mat);
			}
		}

		public Mesh(ByteBuffer pos, ByteBuffer tex, ByteBuffer norm, ByteBuffer index) {
			primitives = new MeshPrimitives[] { new MeshPrimitives(pos, tex, norm, index) };
		}

		@Override
		protected MeshPrimitivesImpl[] getPrimitives() {
			return primitives;
		}
	}

	static class Node extends NodeImpl {
		public void setMaterial(IMaterial mat) {
			((Mesh) mesh).setMaterial(mat);
		}

		public Node(ByteBuffer pos, ByteBuffer tex, ByteBuffer norm, ByteBuffer index) {
			mesh = new Mesh(pos, tex, norm, index);
		}
	}

	static class Model extends ModelImpl {
		@Override
		public ModelImpl setSystemName(String name) {
			nodes.forEach(e -> {
				((Node) e).setMaterial(new Material(name));
			});
			return super.setSystemName(name);
		}

		public Model(List<? extends NodeImpl> nodes) {
			this.nodes = Collections.unmodifiableList(nodes);
			for (NodeImpl node : nodes)
				meshRoot.add(node);
		}
	}

	static class Material implements IMaterial {

		public Material(String name) {
			texture = new ResourceLocation(HideMod.MOD_ID, "skin/" + name);
		}

		@Override
		public void dispose() {

		}

		private ResourceLocation texture;

		@Override
		public int getBaseColorTexture() {
			if (FMLCommonHandler.instance().getSide().isClient()) {
				return getTexID();
			}
			return 0;
		}

		@SideOnly(Side.CLIENT)
		private int getTexID() {
			TextureManager tm = Minecraft.getMinecraft().getTextureManager();
			ITextureObject tex = tm.getTexture(texture);
			if (tex == null) {
				tex = new SimpleTexture(texture);
				tm.loadTexture(texture, tex);
			}
			return tex != null ? tex.getGlTextureId() : 0;
		}

		@Override
		public String toString() {
			return texture.toString();
		}
	}

	private static Vector3f parseVec3f(String[] data) {
		Vector3f ret = new Vector3f();
		ret.x = Float.parseFloat(data[0]);
		ret.y = Float.parseFloat(data[1]);
		ret.z = Float.parseFloat(data[2]);
		return ret;
	}

	private static Vector2f parseVec2f(String[] data) {
		Vector2f ret = new Vector2f();
		ret.x = Float.parseFloat(data[0]);
		ret.y = Float.parseFloat(data[1]);
		return ret;
	}

	public static class HideVertex {
		public int pos, tex, norm;

		public HideVertex(int pos, int tex, int norm) {
			this.pos = pos;
			this.tex = tex;
			this.norm = norm;
		}

		@Override
		public String toString() {
			return "[" + pos + "," + tex + "," + norm + "]";
		}
	}
}
