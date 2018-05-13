package model;

import java.io.IOException;

import hideMod.HideMod;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

public class HideModelLoader implements ICustomModelLoader {

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
	}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		if(modelLocation.getResourceDomain().equals(HideMod.MOD_ID)){
			if(modelLocation.getResourcePath().equals("models/item/gun")){
				return true;
			}
		}
		return false;
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws IOException {
		 return new GunModelLoader();
	}
}
