package model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import types.base.DataBase;

/** モデルのアニメーション用 */
public class Bone extends DataBase {
	/** プロパティの取得元 このプロパティが優先される*/
	transient private Map<AnimationType, Supplier<Float>> Propertis = new EnumMap<>(AnimationType.class);;

	public List<Bone> children = new ArrayList<>();
	public List<ModelSelector> models = new ArrayList<>();

	public Map<AnimationType, List<AnimationKey>> animation = new EnumMap<>(AnimationType.class);

	HideModel rootModel;

	void init(HideModel model) {
		rootModel = model;
		children.forEach(c -> c.init(model));
	}

	public void render(IRenderProperty property) {
		for (AnimationType type : animation.keySet()) {
			if (Propertis.containsKey(type)) {
				AnimationKey.applyAnimation(animation.get(type), Propertis.get(type).get());
			} else if (property.getRenderPropery().containsKey(type)) {
				AnimationKey.applyAnimation(animation.get(type), property.getRenderPropery().get(type));
			}
		}
		models.forEach(model -> rootModel.render(model.getModel(property.getPartPropery())));
		children.forEach(bone -> bone.render(property));
	}
}