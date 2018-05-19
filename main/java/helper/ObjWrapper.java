package helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import hideMod.HideMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.OBJModel;

/**オブジェモデルを読むパーサー*/
public class ObjWrapper {
	private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
	public ObjWrapper(ModelResourceLocation resource){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream(), "utf-8"));
			String currentLine;
			while((currentLine = reader.readLine())!=null){
				//コメントはスキップ
				if (currentLine.isEmpty() || currentLine.startsWith("#")) continue;
				System.out.println(currentLine);
				String[] fields = WHITE_SPACE.split(currentLine, 2);
                String key = fields[0];
                String data = fields[1];

























			}

		} catch (IOException e) {
			HideMod.log("IOException while loading model : "+resource.getResourcePath());
			e.printStackTrace();
		}
	}
}
