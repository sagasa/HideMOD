package hide.model.gltf;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import hide.model.impl.IAnimation;
import hide.model.impl.IMaterial;
import hide.model.impl.NodeImpl;
import hide.model.impl.ModelImpl;

class Model extends ModelImpl {

	public Model(List<? extends NodeImpl> nodes, List<NodeImpl> rootNodes, HashMap<String, IAnimation> animations, List<? extends IMaterial> materials) {
		this.nodes = Collections.unmodifiableList(nodes);
		this.animations = animations;

		for (NodeImpl node : nodes)
			if (node.hasSkin())
				skinRoot.add(node);

		for (NodeImpl node : rootNodes)
			if (node.hasMesh())
				meshRoot.add(node);
			else
				debugRoot.add(node);

		for (IAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(0.5f);
		}
	}

	float anim = 0;

	@Override
	public void render() {
		for (IAnimation hideAnimation : animations.values()) {
			hideAnimation.apply(anim);
		}
		anim += 0.001f;
		anim %= 1;
		super.render();
	}

}
