package types.base;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * EnumDataInfoを利用してデータを取得できるクラス クローン可能 publicフィールドはすべてクローン可能なクラスにしてください
 */
public abstract class DataBase implements Cloneable {

	/** JsonObjectを作成 */
	public String MakeJsonData() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	/**
	 * 指定されたデータベースクラスから指定された型のフィールドをリストに追加する
	 *
	 * @param target
	 *            検索元
	 * @param key
	 *            検索対象
	 *            nullの場合全フィールドを対象に
	 * @param list
	 *            結果
	 * @param deep
	 *            データベース型のフィールド内も検索するか
	 */
	public static List<Field> getFieldsByType(Class<? extends DataBase> target, Class<?> key, List<Field> list,
			boolean deep) {
		try {
			for (Field f : target.getFields()) {
				if (key==null||key.isAssignableFrom(f.getType()))
					list.add(f);
				if (DataBase.class.isAssignableFrom(f.getType())&&deep)
					getFieldsByType((Class<? extends DataBase>) f.getType(), key, list, deep);
			}
		} catch (IllegalArgumentException e) {
		}
		return list;
	}

	/**
	 * 全てのパブリックフィールドを上書き 成功したらtrue クローンはされません！！
	 */
	public boolean overwrite(DataBase data) {
		// 型を比較
		if (!data.getClass().isAssignableFrom(this.getClass())) {
			return false;
		}
		Class<? extends DataBase> clazz = data.getClass();
		// 全てのパブリックフィールドを上書き
		try {
			for (Field f : clazz.getFields()) {
				f.set(this, f.get(data));
				System.out.println("overwrite" + f.getName());
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return false;
		}
		return true;
	}

	/** 全てのパブリックフィールドに引数の各フィールドの値を加算 */
	public boolean overadd(DataBase data) {
		// 型を比較
		if (!data.getClass().isAssignableFrom(this.getClass())) {
			return false;
		}
		Class<? extends DataBase> clazz = data.getClass();
		// フィールドが数値型なら加算 DataBaseならoveradd実行
		try {
			for (Field f : clazz.getFields()) {
				if (f.getType().isAssignableFrom(float.class) || f.getType().isAssignableFrom(Float.class)) {
					f.set(this, f.getFloat(data) + f.getFloat(this));
				} else if (f.getType().isAssignableFrom(int.class) || f.getType().isAssignableFrom(Integer.class)) {
					f.set(this, f.getInt(data) + f.getInt(this));
				} else if (f.getType().isAssignableFrom(DataBase.class)) {
					((DataBase) f.get(this)).overadd((DataBase) f.get(data));
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return false;
		}
		return true;
	}

	/** 全てのパブリックフィールドに引数の各フィールドの値を乗算 */
	public boolean overdia(DataBase data) {
		// 型を比較
		if (!data.getClass().isAssignableFrom(this.getClass())) {
			return false;
		}
		Class<? extends DataBase> clazz = data.getClass();
		// フィールドが数値型なら加算 DataBaseならoveradd実行
		try {
			for (Field f : clazz.getFields()) {
				if (f.getType().isAssignableFrom(float.class) || f.getType().isAssignableFrom(Float.class)) {
					f.set(this, f.getFloat(data) * f.getFloat(this));
				} else if (f.getType().isAssignableFrom(int.class) || f.getType().isAssignableFrom(Integer.class)) {
					f.set(this, f.getInt(data) + f.getInt(this));
				} else if (f.getType().isAssignableFrom(DataBase.class)) {
					((DataBase) f.get(this)).overdia((DataBase) f.get(data));
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return false;
		}
		return true;
	}

	/**
	 * 全ての数値のパブリックフィールドに引数の値を設定 ネストしたDataBase内も含む
	 */
	public boolean setValue(int value) {
		Class<? extends DataBase> clazz = this.getClass();
		// フィールドが数値型なら加算 DataBaseならoveradd実行
		try {
			for (Field f : clazz.getFields()) {
				if (f.getType().isAssignableFrom(float.class) || f.getType().isAssignableFrom(Float.class)) {
					f.set(this, value);
				} else if (f.getType().isAssignableFrom(int.class) || f.getType().isAssignableFrom(Integer.class)) {
					f.set(this, value);
				} else if (f.getType().isAssignableFrom(DataBase.class)) {
					((DataBase) f.get(this)).setValue(value);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			return false;
		}
		return true;
	}

	@Override
	public DataBase clone() {
		DataBase clone;
		try {
			clone = (DataBase) super.clone();
			for (Field f : super.getClass().getFields()) {
				if (!f.getType().isPrimitive()) {
					try {
						f.set(clone, f.get(this));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			return clone;
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
			return null;
		}

	}
}
