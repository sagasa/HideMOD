package hide.model.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import com.google.common.base.Strings;

public class ObjLoader {
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
	private static final Logger LOGGER = LogManager.getLogger();

	/** リソースロケーションからグループ名-ポリゴン配列のMapを返す */
	public static Map<String, HideVertex[]> LoadModel(InputStream model) {
		try {
			return new ObjLoader().Load(model);
		} catch (Throwable e) {
			LOGGER.warn(e.getMessage());
		}
		return null;
	}

	/**
	 * Streamからグループ名-ポリゴン配列のMapを返す
	 * @throws IOException
	 */
	private Map<String, HideVertex[]> Load(InputStream model) throws Throwable {

		List<Vector3f> position = new ArrayList<>();
		List<Vector2f> texCoords = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();
		List<HideVertex> index = new ArrayList<>();

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
				List<HideVertex> verts = new ArrayList<>();
				for (String str : splitData) {
					String[] pts = str.split("/");
					int pos = Integer.parseInt(pts[0]) - 1;
					int tex = pts.length < 2 || Strings.isNullOrEmpty(pts[1]) ? -1 : Integer.parseInt(pts[1]) - 1;
					int norm = pts.length < 3 || Strings.isNullOrEmpty(pts[2]) ? -1 : Integer.parseInt(pts[2]) - 1;
					verts.add(new HideVertex(pos, tex, norm));
				}
				//3角に変換
				for (int i = 0; i < verts.size() - 2; i++) {
					HideVertex v0 = verts.get(0);
					HideVertex v1 = verts.get(i + 1);
					HideVertex v2 = verts.get(i + 2);
					//ノーマル自動計算
					if (v0.norm == -1 || v1.norm == -1 || v2.norm == -1) {
						Vector3f p0 = position.get(v0.pos);
						Vector3f p1 = position.get(v1.pos);
						Vector3f p2 = position.get(v2.pos);

						Vector3f vec0 = new Vector3f();
						Vector3f vec1 = new Vector3f();

						Vector3f.sub(p0, p1, vec0);
						Vector3f.sub(p2, p1, vec1);

						Vector3f.cross(vec0, vec1, vec0);
						vec0.normalise();

						v0.norm = v1.norm = v2.norm = normals.size();
						normals.add(vec0);
					}

					//テクスチャパディング
					if (v0.tex == -1 || v1.tex == -1 || v2.tex == -1) {
						Vector2f p0 = texCoords.get(v0.tex);
						Vector2f p1 = texCoords.get(v1.tex);
						Vector2f p2 = texCoords.get(v2.tex);

						v0.tex = v1.tex = v2.tex = texCoords.size();
						texCoords.add(new Vector2f(0, 0));
					}

					index.add(v0);
					index.add(v1);
					index.add(v2);
				}
			}
		}
		// フォーマットをまとめる
		ByteBuffer posByteBuf = BufferUtils.createByteBuffer(position.size() * 4 * 3);
		ByteBuffer texByteBuf = BufferUtils.createByteBuffer(texCoords.size() * 4 * 2);
		ByteBuffer normByteBuf = BufferUtils.createByteBuffer(normals.size() * 4 * 3);
		ByteBuffer indexByteBuf = BufferUtils.createByteBuffer(index.size() * 4 * 3);

		Vector3f center = new Vector3f();
		position.forEach(v -> {
			posByteBuf.putFloat(v.x);
			posByteBuf.putFloat(v.y);
			posByteBuf.putFloat(v.z);
			Vector3f.add(center, v, center);
		});
		center.x = center.x / position.size();
		center.y = center.y / position.size();
		center.z = center.z / position.size();

		texCoords.forEach(v -> {
			texByteBuf.putFloat(v.x);
			texByteBuf.putFloat(v.y);
		});
		normals.forEach(v -> {
			normByteBuf.putFloat(v.x);
			normByteBuf.putFloat(v.y);
			normByteBuf.putFloat(v.z);
		});
		index.forEach(v -> {
			normByteBuf.putInt(v.pos);
			normByteBuf.putInt(v.tex);
			normByteBuf.putInt(v.norm);
		});

		Map<String, HideVertex[]> res = new HashMap<>();
		for (String str : index.keySet()) {
			res.put(str, index.get(str).toArray(new HideVertex[index.get(str).size()]));
		}
		return res;
	}

	private Vector3f parseVec3f(String[] data) {
		Vector3f ret = new Vector3f();
		ret.x = Float.parseFloat(data[0]);
		ret.y = Float.parseFloat(data[1]);
		ret.z = Float.parseFloat(data[2]);
		return ret;
	}

	private Vector2f parseVec2f(String[] data) {
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
