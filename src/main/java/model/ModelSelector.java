package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import types.base.DataBase;

/** 条件によって表示モデルを変更するためのクラス */
public class ModelSelector extends DataBase {
	/** 指定がない場合のモデル 非表示は空String */
	public String defaultModel = "default";
	/** カスタムスロット内のアイテム名とモデルパーツ名のMap */
	public Map<String, String> item_model = new HashMap<>();

	public ModelSelector() {
	}

	public String getModel(Set<String> prop) {
		//パーツが指定されていなければ
		if (item_model.size() == 0) {
			return defaultModel;
		}
		for (String name : prop) {
			if (item_model.containsKey(name)) {
				return item_model.get(name);
			}
		}
		return defaultModel;
	}
}