package item.model;

import item.render.ModelRender;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import types.render.VertexUV;

public class GunModel{

	ModelRender[] GunModel;

	public GunModel() {
		GunModel = new ModelRender[1];
		VertexUV ver1 = new VertexUV(0, 0, 0, 0, 0);
		VertexUV ver2 = new VertexUV(1, 0, 0, 64, 0);
		VertexUV ver3 = new VertexUV(1, 1, 0, 64, 64);
		VertexUV ver4 = new VertexUV(0, 1, 0, 0, 64);
		GunModel[0] = new ModelRender(ver1, ver2, ver3,ver4, 64, 64, new ResourceLocation("hidemod", "dummy.png"));
	}

	public void render(){
		Tessellator tessellator = Tessellator.getInstance();
		for (ModelRender modelRender : GunModel) {
			modelRender.render(tessellator);
		}
	}
}
