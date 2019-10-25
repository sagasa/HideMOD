package types.base;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import types.base.ValueChange.Operater;

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

	/** 値の変更を行う */
	public void applyChange(ValueChange change) {

		if ((change.OPERATER == Operater.Add || change.OPERATER == Operater.Multiply)&&getType(this, change.PATH)==int.class) {

		}

			switch (change.OPERATER) {
			case Add:

				break;
			case ListAdd:
				break;
			case ListRemove:
				break;
			case Multiply:
				break;
			case Set:
				break;
			default:
				break;

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
	public static List<DataPath> getFieldsByType(Class<? extends DataBase> target, Class<?> key, List<DataPath> list,
			boolean deep) {
		getFieldsByType(target, "", key, list, deep);
		return list;
	}

	private static List<DataPath> getFieldsByType(Class<? extends DataBase> target, String path, Class<?> key,
			List<DataPath> list, boolean deep) {
		try {
			for (Field f : target.getFields()) {
				if (key == null || key.isAssignableFrom(f.getType()))
					list.add(new DataPath(f, path + f.getName()));
				if (DataBase.class.isAssignableFrom(f.getType()) && deep)
					getFieldsByType((Class<? extends DataBase>) f.getType(), path + f.getName() + ".", key, list, deep);
			}
		} catch (IllegalArgumentException e) {
		}
		return list;
	}

	public static <V> void changeFieldsByType(DataBase target, Class<V> key, BiFunction<V, Field, V> change,
			boolean deep) {
		try {
			for (Field f : target.getClass().getFields()) {
				if (key == null || key.isAssignableFrom(f.getType()))
					f.set(target, change.apply((V) f.get(target), f));
				if (DataBase.class.isAssignableFrom(f.getType()) && deep)
					changeFieldsByType((DataBase) f.get(target), key, change, deep);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
		}
	}

	/***/
	public static class DataPath {
		public DataPath(Field field, String path) {
			this.field = field;
			this.path = path;
		}

		public Field field;
		public String path;
	}
}
