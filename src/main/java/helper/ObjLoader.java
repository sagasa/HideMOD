package helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

import hideMod.model.ModelPart;
import hideMod.model.Polygon;
import hideMod.model.VertexUV;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.client.model.obj.OBJModel.OBJBakedModel;

public class ObjLoader {
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
	private static final Logger LOGGER = LogManager.getLogger();

	private List<Vec3d> vertices = new ArrayList<>();
	private List<Vec2f> texCoords = new ArrayList<>();
	private List<Vec3d> normals = new ArrayList<>();
	private Map<String, List<Polygon>> poly = new HashMap<>();


	/** リソースロケーションからグループ名-ポリゴン配列のMapを返す */
	public static Map<String, ModelPart> LoadModel(InputStream model) {
		try {
			return new ObjLoader().Load(model);
		} catch (IOException e) {
			LOGGER.warn(e.getMessage());
		}
		return null;
	}

	/**
	 * Streamからグループ名-ポリゴン配列のMapを返す
	 * @throws IOException
	 */
	private Map<String, ModelPart> Load(InputStream model) throws IOException {
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
				if (splitData.length > 4 || group == null) {
					LOGGER.warn("頂点多すぎ");
					continue;
				}
				List<VertexUV> verts = new ArrayList<>();
				for (String str : splitData) {
					String[] pts = str.split("/");

					int vert = Integer.parseInt(pts[0]);
					Integer texture = pts.length < 2 || Strings.isNullOrEmpty(pts[1]) ? null : Integer.parseInt(pts[1]);
					Integer normal = pts.length < 3 || Strings.isNullOrEmpty(pts[2]) ? null : Integer.parseInt(pts[2]);
					Vec3d pos = vertices.get(vert - 1);
					VertexUV vertexuv;
					if (texture != null) {
						Vec2f tex = texCoords.get(texture - 1);
						vertexuv = new VertexUV((float) pos.x, (float) pos.y, (float) pos.z, tex.x, tex.y);
					} else {
						vertexuv = new VertexUV((float) pos.x, (float) pos.y, (float) pos.z, 0, 0);
					}
					verts.add(vertexuv);
				}
				if (!poly.containsKey(group)) {
					poly.put(group, new ArrayList<>());
				}
				poly.get(group).add(new Polygon(verts.toArray(new VertexUV[verts.size()])));
			}
		}
		// フォーマットをまとめる
		Map<String, ModelPart> res = new HashMap<>();
		for (String str : poly.keySet()) {
			res.put(str, new ModelPart(poly.get(str).toArray(new Polygon[poly.get(str).size()])));
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
