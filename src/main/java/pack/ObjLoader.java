package pack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

import model.HideModel.HideVertex;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class ObjLoader {
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
	private static final Logger LOGGER = LogManager.getLogger();

	private List<Vec3d> vertices = new ArrayList<>();
	private List<Vec2f> texCoords = new ArrayList<>();
	private List<Vec3d> normals = new ArrayList<>();
	private Map<String, List<HideVertex>> poly = new HashMap<>();

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
				float[] value = parseFloats(splitData);
				vertices.add(new Vec3d(value[0], value[1], value[2]));
			} else if (key.equalsIgnoreCase("vt")) {
				float[] value = parseFloats(splitData);
				texCoords.add(new Vec2f(value[0], value[1]));
			} else if (key.equalsIgnoreCase("vn")) {
				float[] value = parseFloats(splitData);
				normals.add(new Vec3d(value[0], value[1], value[2]));
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

					int vert = Integer.parseInt(pts[0]);
					Integer texture = pts.length < 2 || Strings.isNullOrEmpty(pts[1]) ? null : Integer.parseInt(pts[1]);
					Integer normal = pts.length < 3 || Strings.isNullOrEmpty(pts[2]) ? null : Integer.parseInt(pts[2]);
					Vec3d pos = vertices.get(vert - 1);

					HideVertex vertexuv;
					if (texture != null) {
						Vec2f tex = texCoords.get(texture - 1);
						vertexuv = new HideVertex((float) pos.x, (float) pos.y, (float) pos.z, tex.x, tex.y);
					} else {
						vertexuv = new HideVertex((float) pos.x, (float) pos.y, (float) pos.z, 0, 0);
					}
					verts.add(vertexuv);
				}
				if (!poly.containsKey(group)) {
					poly.put(group, new ArrayList<>());
				}
				//3角に変換
				for (int i = 0; i < verts.size() - 2; i++) {
					poly.get(group).add(verts.get(0));
					poly.get(group).add(verts.get(i + 1));
					poly.get(group).add(verts.get(i + 2));
				}
			}
		}
		// フォーマットをまとめる
		Map<String, HideVertex[]> res = new HashMap<>();
		for (String str : poly.keySet()) {
			res.put(str, poly.get(str).toArray(new HideVertex[poly.get(str).size()]));
		}
		return res;
	}

	private float[] parseFloats(String[] data) {
		float[] ret = new float[data.length];
		for (int i = 0; i < data.length; i++)
			ret[i] = Float.parseFloat(data[i]);
		return ret;
	}
}
