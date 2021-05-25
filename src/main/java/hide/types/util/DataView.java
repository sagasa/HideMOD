package hide.types.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import hide.types.base.DataBase;
import hide.types.base.DataBase.DataEntry;
import hide.types.base.DataMap;

public class DataView<T extends DataBase> {

	ViewCache<T> cache;

	private Class<T> target;
	int staticModifierSize;

	public DataView(Class<T> clazz, int staticModifierSize) {
		target = clazz;
		cache = new ViewCache<>(target);
		this.staticModifierSize = staticModifierSize;
		cache.staticModifier = new Object[staticModifierSize];
	}

	public void setBase(T base) {
		if (cache.baseData == base)
			return;
		dep();
		cache.baseData = base;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase && base.get(k) != k.Default)
				((ViewCache) v).baseData = (DataBase) base.get(k);
		});
		cache.clearMap(cache.baseData);
	}

	public void setModifier(int index, T value) {
		if (cache.staticModifier[index] == value)
			return;
		dep();
		cache.clearMap((T) cache.staticModifier[index]);
		cache.staticModifier[index] = value;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase && value.get(k) != k.Default)
				((ViewCache) v).staticModifier[index] = value.get(k);
		});
		cache.clearMap(value);
	}

	public void setModifier(List<T> modifier) {
		if (cache.modifier.equals(modifier))
			return;
		clearModifier();
		cache.modifier = modifier;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase) {
				((ViewCache) v).modifier = modifier.stream().filter(mod -> mod.get(k) != k.Default).map(mod -> mod.get(k)).collect(Collectors.toList());
			}
		});
		modifier.forEach(mod -> cache.clearMap(mod));
	}

	public void clearModifier() {
		if (cache.modifier.isEmpty())
			return;
		dep();
		cache.modifier.forEach(mod -> cache.clearMap(mod));
		cache.modifier.clear();
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase) {
				((ViewCache) v).modifier = Collections.EMPTY_LIST;
			}
		});
	}

	public ViewCache<T> getView() {
		return cache;
	}

	public <R extends DataBase> ViewCache<R> getData(DataEntry<R> key) {
		//DataBase型ならモディファイが乗らないので
		if (key.Default instanceof DataBase)
			throw new IllegalArgumentException("use ");
		return cache.getData(key);
	}

	public <R> R get(DataEntry<R> key) {
		//DataBase型ならモディファイが乗らないので
		if (key.Default instanceof DataBase)
			throw new IllegalArgumentException("use ");
		return cache.get(key);
	}

	private void dep() {
		ViewCache<T> old = cache;
		cache = new ViewCache<>(target);
		cache.dataMap.putAll(old.dataMap);
	}

	public static class ViewCache<T extends DataBase> {
		T baseData;
		DataMap<Object> dataMap;
		Object[] staticModifier;
		List<T> modifier = Collections.EMPTY_LIST;

		boolean isDefault = false;

		public ViewCache(Class<T> clazz) {
			dataMap = new DataMap<>(clazz);
		}

		/**キャッシュを削除*/
		private void clearMap(T value) {
			for (DataEntry<?> key : value.getKeySet()) {
				if (key.Default instanceof DataBase) {
					if (dataMap.containsKey(key))
						((ViewCache) dataMap.get(key)).clearMap((DataBase) value.get(key));
				} else
					dataMap.remove(key);
			}
		}

		public <R extends DataBase> ViewCache<R> getData(DataEntry<R> key) {
			if (!(key.Default instanceof DataBase))
				throw new IllegalArgumentException("must use get");
			if (!dataMap.containsKey(key)) {
				ViewCache<R> value = new ViewCache<>((Class) key.Default.getClass());
				if (baseData != null) {
					R r = baseData.get(key);
					if (r != key.Default)
						value.baseData = r;
				}
				value.staticModifier = Arrays.stream((T[]) staticModifier).map(mod -> {
					if (mod != null && mod.get(key) != key.Default)
						return mod.get(key);
					return null;
				}).toArray();
				value.modifier = modifier.stream().filter(mod -> mod.get(key) != key.Default).map(mod -> mod.get(key)).collect(Collectors.toList());
				dataMap.put(key, value);
			}
			return (ViewCache<R>) dataMap.get(key);
		}

		public <R> R get(DataEntry<R> key) {
			if (key.Default instanceof DataBase)
				throw new IllegalArgumentException("must use getData");
			if (!dataMap.containsKey(key)) {
				R value = key.Default;
				if (baseData != null)
					value = baseData.get(key, value);
				for (int i = 0; i < staticModifier.length; i++)
					if (staticModifier[i] != null)
						value = ((DataBase) staticModifier[i]).get(key, value);
				for (T mod : modifier) {
					value = mod.get(key, value);
				}
				dataMap.put(key, value);
			}
			return (R) dataMap.get(key);
		}
	}
}
