package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import hide.types.base.DataBase;

/** モデルのアニメーション用 */
public class Bone extends DataBase {
	/** プロパティの取得元 このプロパティが優先される*/
	transient private IRenderProperty Propertis = null;

	public List<Bone> children = new ArrayList<>();
	public List<ModelSelector> models = new ArrayList<>();

	public Map<AnimationType, List<AnimationKey>> animation = new EnumMap<>(AnimationType.class);

	HideModel rootModel;

	void init(HideModel model) {
		rootModel = model;
		children.forEach(c -> c.init(model));
	}

	public void render(IRenderProperty property) {
		//Map<AnimationType, Float> renderProp = Propertis != null && Propertis.getAnimationProp() != null ? Propertis.getAnimationProp() : property.getAnimationProp();
		//Map<String, List<String>> parts = Propertis != null && Propertis.getPartPropery() != null ? Propertis.getPartPropery() : property.getPartPropery();
		for (AnimationType type : animation.keySet()) {
			//	AnimationKey.applyAnimation(animation.get(type), renderProp.get(type));
		}
		models.forEach(model -> rootModel.render(model.getModel(Collections.EMPTY_SET)));
		children.forEach(bone -> bone.render(property));
	}
}