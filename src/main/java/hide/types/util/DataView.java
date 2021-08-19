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

	int staticModifierSize;

	public DataView(Class<T> clazz, int staticModifierSize) {
		cache = new ViewCache<>(clazz);
		this.staticModifierSize = staticModifierSize;
		cache.staticModifier = new Object[staticModifierSize];
	}

	public void setBase(IDataHolder base) {
		if (cache.baseData == base)
			return;
		dep();
		cache.setBase(base);
	}

	public void setModifier(int index, IDataHolder value) {
		if (cache.staticModifier[index] == value)
			return;
		//System.out.println("setMod " + index + " " + value);
		dep();
		cache.setModifier(index, value);
	}

	public void setModifier(List<IDataHolder> modifier) {
		if (cache.modifier.equals(modifier))
			return;
		cache.setModifier(modifier);
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
		cache = cache.dep();
	}

	public static class ViewCache<T extends DataBase> implements IDataHolder {
		private Class<T> target;
		private IDataHolder baseData;
		private DataMap<ViewEntry> dataMap;
		private Object[] staticModifier;
		private List<? extends IDataHolder> modifier = Collections.EMPTY_LIST;

		public ViewCache(Class<T> clazz) {
			target = clazz;
			dataMap = new DataMap<>(clazz);
		}

		void setBase(IDataHolder base) {
			if (baseData == base)
				return;
			clearMap(baseData);
			baseData = base;
			dataMap.forEach((k, v) -> {
				if (k.Default instanceof DataBase)
					((ViewCache) v.value).setBase(base == null ? null : (IDataHolder) base.get(k));
			});
			clearMap(baseData);
		}

		void setModifier(int index, IDataHolder value) {
			if (staticModifier[index] == value)
				return;
			clearMap((IDataHolder) staticModifier[index]);
			staticModifier[index] = value;
			dataMap.forEach((k, v) -> {
				if (k.Default instanceof DataBase)
					((ViewCache) v.value).setModifier(index, value == null ? null : (IDataHolder) value.get(k));
			});
			clearMap(value);
		}

		void setModifier(List<IDataHolder> mod) {
			if (mod == null)
				mod = Collections.EMPTY_LIST;
			if (modifier.equals(mod))
				return;

			modifier.forEach(m -> clearMap(m));
			modifier = mod;
			dataMap.forEach((k, v) -> {
				if (k.Default instanceof DataBase) {
					((ViewCache) v.value).setModifier(modifier.stream().map(m -> m.get(k)).collect(Collectors.toList()));
				}
			});
			modifier.forEach(m -> clearMap(m));
		}

		private ViewCache<T> dep() {
			ViewCache<T> cache = new ViewCache<>(target);
			cache.baseData = baseData;
			cache.modifier = modifier;
			cache.staticModifier = staticModifier.clone();
			dataMap.forEach((k, v) -> {
				if (k.Default instanceof DataBase)
					cache.dataMap.put(k, new ViewEntry(((ViewCache) v.value).dep(), null));
				else
					cache.dataMap.put(k, new ViewEntry(v.value, v.base));
			});
			return cache;
		}

		@Override
		public String toString() {
			return super.toString();
		}

		/**キャッシュを削除*/
		private void clearMap(IDataHolder value) {
			if (value == null) {

				//return;
			}
			//System.out.println("clear req " + value);
			//DataBase以外なら全削除
			//value instanceof DataBase ? ((DataBase) value).getKeySet() :
			for (DataEntry<?> key : dataMap.keySet()) {
				if (key.Default instanceof DataBase) {
					if (dataMap.containsKey(key)) {
						//System.out.println("clear " + key);
						((ViewCache) dataMap.get(key).value).clearMap(value == null ? null : (IDataHolder) value.get(key));
					}
				} else
					dataMap.remove(key);
			}
		}

		public <R extends DataBase> ViewCache<R> getData(DataEntry<R> key) {
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
