package model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import types.base.DataBase;

/** モデルのアニメーション用 */
public class Bone extends DataBase {
	/** プロパティの取得元 兵器の場合はシートから取得するように*/
	transient private Map<String, Supplier<Float>> Propertis = new HashMap<>();

	public List<Bone> children = new ArrayList<>();
	public List<ModelSelector> models = new ArrayList<>();

	public Map<AnimationType, List<AnimationKey>> animation = new EnumMap<>(AnimationType.class);

	void init(HideModel model) {
		children.forEach(c -> c.init(model));
	}

	public void render(Map<AnimationType, Float> property) {
		for(AnimationType type:animation.keySet()) {
			if(property.containsKey(type)) {
				AnimationKey.applyAnimation(animation.get(type), property.get(type));
			}
		}
	}
}