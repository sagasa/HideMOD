package hide.types.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import hide.types.base.DataBase;
import hide.types.base.DataBase.DataEntry;
import hide.types.base.DataMap;
import hide.types.base.IDataHolder;

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

	public void setBase(IDataHolder base) {
		if (cache.baseData == base)
			return;
		dep();
		cache.baseData = base;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase && base.get(k) != k.Default)
				((ViewCache) v.value).baseData = (DataBase) base.get(k);
		});
		cache.clearMap(cache.baseData);
	}

	public void setModifier(int index, IDataHolder value) {
		if (cache.staticModifier[index] == value)
			return;
		dep();
		cache.clearMap((IDataHolder) cache.staticModifier[index]);
		cache.staticModifier[index] = value;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase && value.get(k) != k.Default)
				((ViewCache) v.value).staticModifier[index] = value.get(k);
		});
		cache.clearMap(value);
	}

	public void setModifier(List<IDataHolder> modifier) {
		if (cache.modifier.equals(modifier))
			return;
		clearModifier();
		cache.modifier = modifier;
		cache.dataMap.forEach((k, v) -> {
			if (k.Default instanceof DataBase) {
				((ViewCache) v.value).modifier = modifier.stream().filter(mod -> mod.get(k) != k.Default).map(mod -> mod.get(k)).collect(Collectors.toList());
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
				((ViewCache) v.value).modifier = Collections.EMPTY_LIST;
			}
		});
	}

	public ViewCache<T> getView() {
		return cache;
	}

	public <R extends DataBase> ViewCache<R> getData(DataEntry<R> key) {
		return cache.getData(key);
	}

	public <R> R get(DataEntry<R> key) {
		return cache.get(key);
	}

	private void dep() {
		ViewCache<T> old = cache;
		cache = new ViewCache<>(target);
		cache.baseData = old.baseData;
		cache.modifier = old.modifier;
		cache.staticModifier = old.staticModifier.clone();
		cache.dataMap.putAll(old.dataMap);
	}

	public static class ViewCache<T extends DataBase> implements IDataHolder {
		IDataHolder baseData;
		DataMap<ViewEntry> dataMap;
		Object[] staticModifier;
		List<? extends IDataHolder> modifier = Collections.EMPTY_LIST;

		public ViewCache(Class<T> clazz) {
			dataMap = new DataMap<>(clazz);
		}

		@Override
		public String toString() {
			return super.toString();
		}

		/**キャッシュを削除*/
		private void clearMap(IDataHolder value) {
			if (value == null)
				return;
			//DataBase以外なら全削除
			for (DataEntry<?> key : value instanceof DataBase ? ((DataBase) value).getKeySet() : dataMap.keySet()) {
				//System.out.println("clear " + key);
				if (key.Default instanceof DataBase) {
					if (dataMap.containsKey(key))
						((ViewCache) dataMap.get(key).value).clearMap((DataBase) value.get(key));
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
				value.staticModifier = Arrays.stream(staticModifier).map(m -> {
					IDataHolder mod = (IDataHolder) m;
					if (mod != null && mod.get(key) != key.Default)
						return mod.get(key);
					return null;
				}).toArray();
				value.modifier = modifier.stream().filter(mod -> mod.get(key) != key.Default).map(mod -> mod.get(key)).collect(Collectors.toList());
				dataMap.put(key, new ViewEntry(value, null));
			}
			return (ViewCache<R>) dataMap.get(key).value;
		}

		@Override
		public <R> R get(DataEntry<R> key, R base) {
			if (key.Default instanceof DataBase)
				throw new IllegalArgumentException("must use getData");
			if (!dataMap.containsKey(key) || key.Default == base && dataMap.get(key).base != base) {
				R value = base;
				if (baseData != null)
					value = baseData.get(key, value);
				//System.out.println("calc " + key);
				//System.out.println("base " + value);
				for (int i = 0; i < staticModifier.length; i++)
					if (staticModifier[i] != null) {
						value = ((IDataHolder) staticModifier[i]).get(key, value);
						//System.out.println("staticModifier " + value);
					}
				for (IDataHolder mod : modifier) {
					value = mod.get(key, value);
				}

				//System.out.println("res " + value);
				dataMap.put(key, new ViewEntry(value, base));
			}
			return (R) dataMap.get(key).value;
		}
	}

	private static class ViewEntry {
		Object value;
		Object base;

		public ViewEntry(Object value, Object base) {
			this.base = base;
			this.value = value;
		}
	}
}
