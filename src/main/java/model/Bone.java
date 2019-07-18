package model;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import hidemod.HideMod;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import types.base.DataBase;

/** モデルのアニメーション用 */
public class Bone extends DataBase implements IBone {
	transient private static final ScriptEngineManager EngineManager = new ScriptEngineManager();
	transient protected ScriptEngine scriptEngine = EngineManager.getEngineByName("nashorn");
	transient protected CompiledScript animation;
	transient private Map<String, Supplier<Float>> Propertis = new HashMap<>();

	public List<Bone> children = new ArrayList<>();
	public List<ModelSelector> models = new ArrayList<>();

	public String script = "";

	/** プロパティの取得元 */
	transient public IRenderProperty rootProperty;

	public Bone() {
		getClass().getClassLoader();
		URLClassLoader.newInstance(new URL[] {new File(HideMod.getModDir().getAbsolutePath()+"\nashorn.jar").toURI().toURL()}, );
		System.out.println(new NashornScriptEngineFactory());
		EngineManager.getEngineFactories().forEach(m->System.out.println("Engine "+m.getEngineName()+" "+m.getLanguageName()));
		System.out.println("AAAAAAAAAAAAAAAAAAA"+scriptEngine.ENGINE+" : "+scriptEngine.NAME+" - "+scriptEngine);

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