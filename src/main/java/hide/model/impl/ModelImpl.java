package hide.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import net.minecraft.profiler.Profiler;

public class ModelImpl implements IDisposable {

	public static Profiler profiler = new Profiler();

	protected List<NodeImpl> nodes = new ArrayList<>();
	protected Map<String, IAnimation> animations = new HashMap<>();

	protected List<NodeImpl> debugRoot = new ArrayList<>();
	protected List<NodeImpl> meshRoot = new ArrayList<>();
	protected List<NodeImpl> skinRoot = new ArrayList<>();

	public ModelImpl setSystemName(String name) {
		return this;
	}

	public ModelImpl postInit() {
		nodes.forEach(n -> n.postInit());
		return this;
	}

	public void render() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);

		GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);

		//GlStateManager.translate(0, 2, 0);

		for (NodeImpl node : meshRoot) {
			node.render(false);
		}

		for (NodeImpl node : debugRoot) {
			node.render(true);
		}

		for (NodeImpl node : skinRoot) {
			node.renderSkin();
		}

		GL20.glDisableVertexAttribArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

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
}
