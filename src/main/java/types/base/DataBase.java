package types.base;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * EnumDataInfoを利用してデータを取得できるクラス クローン可能 publicフィールドはすべてクローン可能なクラスにしてください
 */
public abstract class DataBase implements Cloneable {
	private final static Logger log = LogManager.getLogger();

	/** JsonObjectを作成 */
	public String MakeJsonData() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	/** Mapから変更を適応する */
	public void setString(Map<String, String> valueMap) {
		for (String key : valueMap.keySet()) {
			// 型確認
			Class<?> clazz = getType(this, key);
			if (String.class.isAssignableFrom(clazz)) {
				setValue(this, key, valueMap.get(key));
			} else {
				log.error(key + " is not String field");
			}
		}
	}

	/** Mapから変更を適応する */
	public void setFloat(Map<String, Float> valueMap) {
		for (String key : valueMap.keySet()) {
			// 型確認
			Class<?> clazz = getType(this, key);
			if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
				setValue(this, key, valueMap.get(key).intValue());
			} else if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
				setValue(this, key, valueMap.get(key));
			} else {
				log.error(key + " is not int,float field");
			}
		}
	}

	/** Mapから変更を適応する */
	public void addFloat(Map<String, Float> valueMap) {
		for (String key : valueMap.keySet()) {
			// 型確認
			Class<?> clazz = getType(this, key);
			if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
				setValue(this, key, (int) ((int) getValue(this, key) + valueMap.get(key)));
			} else if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
				setValue(this, key, (float) ((float) getValue(this, key) + valueMap.get(key)));
			} else {
				log.error(key + " is not int,float field");
			}
		}
	}

	/** Mapから変更を適応する */
	public void multiplyFloat(Map<String, Float> valueMap) {
		for (String key : valueMap.keySet()) {
			// 型確認
			Class<?> clazz = getType(this, key);
			if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
				setValue(this, key, (int) ((int) getValue(this, key) * valueMap.get(key)));
			} else if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
				setValue(this, key, (float) ((float) getValue(this, key) * valueMap.get(key)));
			} else {
				log.error(key + " is not int,float field");
			}
		}
	}

	/** .区切りのフィールド名のパスにの型取得する */
	public static Class<?> getType(DataBase data, String path) {
		String[] split = path.split("\\.", 2);
		try {
			// フィールド取得
			Field field = data.getClass().getField(split[0]);
			if (split.length == 2) {
				return getType((DataBase) field.get(data), split[1]);
			} else if (split.length == 1) {
				return field.getType();
			}
		} catch (NoSuchFieldException e) {
			log.error("cant find field : " + path + " from " + data.getClass().getSimpleName());
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** .区切りのフィールド名のパスにデータを書き込む */
	public static void setValue(DataBase data, String path, Object value) {
		String[] split = path.split("\\.", 2);
		try {
			// フィールド取得
			Field field = data.getClass().getField(split[0]);
			if (split.length == 2) {
				setValue((DataBase) field.get(data), split[1], value);
			} else if (split.length == 1) {
				field.set(data, value);
			}
		} catch (NoSuchFieldException e) {
			log.error("cant find field : " + path + " from " + data.getClass().getSimpleName());
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/** .区切りのフィールド名のパスからデータを取得する */
	public static Object getValue(DataBase data, String path) {
		String[] split = path.split("\\.", 2);
		try {
			// フィールド取得
			Field field = data.getClass().getField(split[0]);
			if (split.length == 2) {
				return getValue((DataBase) field.get(data), split[1]);
			} else if (split.length == 1) {
				return field.get(data);
			}
		} catch (NoSuchFieldException e) {
			log.error("cant find field : " + path + " from " + data.getClass().getSimpleName());
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	/** DataBaseとString[]を個別クローン */
	public DataBase clone() {
		try {
			DataBase clone = (DataBase) super.clone();
			for (Field f : super.getClass().getFields()) {
				if (DataBase.class.isAssignableFrom(f.getType())) {
					f.set(clone, ((DataBase) f.get(this)).clone());
				} else if (String[].class.isAssignableFrom(f.getType())) {
					f.set(clone, ((String[]) f.get(this)).clone());
				}
			}
			return clone;
		} catch (CloneNotSupportedException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 指定されたデータベースクラスから指定された型のフィールドをリストに追加する
	 *
	 * @param target
	 *            検索元
	 * @param key
	 *            検索対象 nullの場合全フィールドを対象に
	 * @param list
	 *            結果
	 * @param deep
	 *            データベース型のフィールド内も検索するか
	 */
	public static List<Field> getFieldsByType(Class<? extends DataBase> target, Class<?> key, List<Field> list,
			boolean deep) {
		try {
			for (Field f : target.getFields()) {
				if (key == null || key.isAssignableFrom(f.getType()))
					list.add(f);
				if (DataBase.class.isAssignableFrom(f.getType()) && deep)
					getFieldsByType((Class<? extends DataBase>) f.getType(), key, list, deep);
			}
		} catch (IllegalArgumentException e) {
		}
		return list;
	}
}
