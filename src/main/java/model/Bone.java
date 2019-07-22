package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import types.base.DataBase;

/** モデルのアニメーション用 */
public class Bone extends DataBase implements IBone {
	transient private static final ScriptEngineManager EngineManager;
	transient private static final boolean ScriptAnimation;
	transient protected ScriptEngine scriptEngine = EngineManager.getEngineByName("nashorn");
	transient protected CompiledScript animation;
	transient private Map<String, Supplier<Float>> Propertis = new HashMap<>();

	public List<Bone> children = new ArrayList<>();
	public List<ModelSelector> models = new ArrayList<>();

	public String script = "";

	static {
		EngineManager = new ScriptEngineManager();
		ScriptAnimation = EngineManager.getEngineByName("nashorn") != null;
	}

	/** プロパティの取得元 */
	transient public IRenderProperty rootProperty;

	public Bone() {
		if (ScriptAnimation)
			scriptEngine.put("bone", this);
	}

	@Override
	public void loadIdentity() {
		setPivot(0, 0, 0);
		setRotate(0, 0);
		setScale(1, 1, 1);
		setTranslate(0, 0, 0);
	}

	@Override
	public void setPivot(float x, float y, float z) {
	}

	@Override
	public void setRotate(float yaw, float pitch) {
	}

	@Override
	public void setTranslate(float x, float y, float z) {
	}

	@Override
	public void setScale(float x, float y, float z) {
	}

	@Override
	public void setVisible(boolean visible) {
	}

	@Override
	public void update() {
		if (ScriptAnimation)
			try {
				animation.eval();
			} catch (ScriptException e) {
			}
	}

	@Override
	public float getRenderPropery(String name) {
		return 0;
	}
}