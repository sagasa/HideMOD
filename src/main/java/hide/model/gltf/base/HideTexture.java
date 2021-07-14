package hide.model.gltf.base;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.texture.TextureUtil;

public class HideTexture {

	int[] dynamicTextureData ;

	int texID = -1;

	public int getTexID() {
		if(texID == -1)
			texID = GL11.glGenTextures();
		return texID;
	}

	  public HideTexture(BufferedImage bufferedImage)
	    {
	        this(bufferedImage.getWidth(), bufferedImage.getHeight());
	        bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.dynamicTextureData, 0, bufferedImage.getWidth());
	        this.updateDynamicTexture();
	    }

	    public HideTexture(int textureWidth, int textureHeight)
	    {
	        this.width = textureWidth;
	        this.height = textureHeight;
	        this.dynamicTextureData = new int[textureWidth * textureHeight];
	        TextureUtil.allocateTexture(getTexID(), textureWidth, textureHeight);
	    }


	public static void load(InputStream imageStream) {
        BufferedImage bufferedimage;

        try
        {
            bufferedimage = ImageIO.read(imageStream);
        }
        finally
        {
            IOUtils.closeQuietly(imageStream);
        }

	}
}
