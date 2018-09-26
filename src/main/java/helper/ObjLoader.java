package helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import types.model.ModelPart;

public class ObjLoader {
	private static final Logger LOGGER = LogManager.getLogger();
	public static ModelPart LoadModel(ResourceLocation model){
		try {
			InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(model).getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			reader.readLine();
		} catch (IOException e) {
			LOGGER.warn("");
		}
		return null;
	}
}
