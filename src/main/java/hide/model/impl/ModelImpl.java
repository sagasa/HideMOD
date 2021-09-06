package hide.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector3f;

import hide.model.util.TransformMatUtil;
import net.minecraft.profiler.Profiler;

public class ModelImpl implements IDisposable {

	public static Profiler profiler = new Profiler();

	protected List<NodeImpl> nodes = new ArrayList<>();
	protected Map<String, IAnimation> animations = new HashMap<>();

	protected List<NodeImpl> debugRoot = new ArrayList<>();
	protected List<NodeImpl> meshRoot = new ArrayList<>();
	protected List<NodeImpl> skinRoot = new ArrayList<>();
	protected List<SkinImpl> skins = new ArrayList<>();

	transient protected Map<String, NodeImpl> nodeMap = new HashMap<>();

	public ModelImpl setSystemName(String name) {
		return this;
	}

	public ModelImpl postInit() {
		skins.forEach(s -> {
			s.postInit();
		});
		nodes.forEach(n -> {
			n.postInit();
			if (n.name != null) {
				nodeMap.put(n.name, n);
			}
		});
		return this;
	}

	public void render() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);

		GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);

		//GL11.glScalef(1, -1, 1);
		//GlStateManager.translate(0, 2, 0);

		for (NodeImpl node : meshRoot) {
			node.render(false);
		}

		for (NodeImpl node : debugRoot) {
			node.render(false);
		}

		for (NodeImpl node : skinRoot) {
			node.renderSkin();
		}

		GL20.glDisableVertexAttribArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		/*
		for (NodeImpl nodeImpl : nodes) {
			Vector3f vec = TransformMatUtil.mul(nodeImpl.getGlobalMat(), new Vector3f(0, 0, 0));
			GlStateManager.disableDepth();
			DebugDraw.drawString(nodeImpl.name, vec.x, vec.y, vec.z, 0.05f, 0xFFFFFF);
			GlStateManager.enableDepth();

		}//*/

		GL11.glPopAttrib();
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
	}

	@Override
	public void dispose() {
		for (NodeImpl hideNode : nodes) {
			hideNode.dispose();
		}
	}

	public NodeImpl getNode(String key) {
		return nodeMap.get(key);
	}

	public Vector3f getNodePos(String key) {
		return nodeMap.containsKey(key) ? TransformMatUtil.mul(nodeMap.get(key).getGlobalMat(), new Vector3f()) : new Vector3f();
	}

	public void showNodeMap() {
		System.out.println(nodeMap);
	}

	public IAnimation getAnimation(String name) {
		return animations.get(name);
	}
}
