package hide.types.pack;

import hide.types.base.NamedData;

/** 親子関係は利用しない */
public class PackInfo extends NamedData {
	/** パックの登録名 ファイル名ではない */
	public static final DataEntry<String> PackName = of("sample");
	/** パックのバージョン */
	public static final DataEntry<String> PackVar = of("0");
	/** 登録時の名称 */
	public static final DataEntry<String> PackDomain = of("default");

	@Override
	public DataEntry<String> displayName() {
		return PackName;
	}

	@Override
	public DataEntry<String> systemName() {
		return PackDomain;
	}
}
