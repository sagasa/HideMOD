package hide.types.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import hide.types.base.DataBase;
import hide.types.base.Info;

/** 一番上に位置するDataに必要 */
public abstract class NamedData extends DataBase {

	/** 親の登録名 String */
	public static final DataEntry<String> ParentName = of("", new Info().IsName(true));

	protected void setParent(NamedData data) {
		parent = data;
		initParent();
	}

	/** 依存関係の解決 */
	public static void resolvParent(Collection<? extends NamedData> collection) {
		Map<String, NamedData> map = new HashMap<>();
		collection.forEach(data -> map.put(data.getSystemName(), data));
		collection.forEach(data -> {
			data.setParent(map.get(data.get(NamedData.ParentName, null)));
			data.initParent();
		});
	}

	public abstract DataEntry<String> displayName();

	/** 全部小文字[a-z][0-9] */
	public abstract DataEntry<String> systemName();

	public String getSystemName() {
		return get(systemName(), null);
	}
}
