package hide.model.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

public class HideTexture {

	int[] dynamicTextureData;

	int texID = -1;

	private int width;

	private int height;

	public int getTexID() {
		if (texID == -1)
			texID = GL11.glGenTextures();
		return texID;
	}

	public void bindTexture() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, getTexID());

	}

	private static final int BUFFER_SIZE = 4194304;
	private static final IntBuffer DATA_BUFFER = BufferUtils.createByteBuffer(BUFFER_SIZE << 2).asIntBuffer();

	private static void copyToBuffer(int[] data, int offset, int length) {
		//System.out.println(" " + offset + " " + length);
		DATA_BUFFER.clear();
		DATA_BUFFER.put(data, offset, length);
		DATA_BUFFER.position(0).limit(length);
	}

	public HideTexture(BufferedImage bufferedImage) {
		this(bufferedImage.getWidth(), bufferedImage.getHeight());
		bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.dynamicTextureData, 0, bufferedImage.getWidth());
		uploadTexture(this.getTexID(), this.dynamicTextureData, this.width, this.height);

		//GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
		//		GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, DATA_BUFFER);

		//GL11.glTexImage2D(GL11.GL_TEXTURE_2D, getTexID(), GL11.GL_RGBA, width, height, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
		//uploadTextureSub(getTexID(), dynamicTextureData, width, height, 0, 0);
	}

	public HideTexture(int textureWidth, int textureHeight) {
		this.width = textureWidth;
		this.height = textureHeight;
		this.dynamicTextureData = new int[textureWidth * textureHeight];
		allocateTexture(getTexID(), textureWidth, textureHeight);
	}

	public static void allocateTexture(int textureId, int width, int height) {
		allocateTextureImpl(textureId, 0, width, height);
	}

	public static void allocateTextureImpl(int glTextureId, int mipmapLevels, int width, int height) {
		synchronized (net.minecraftforge.fml.client.SplashProgress.class) {
			GL11.glDeleteTextures(glTextureId);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
		}
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		if (mipmapLevels >= 0) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, mipmapLevels);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, mipmapLevels);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
		}

		for (int i = 0; i <= mipmapLevels; ++i) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA, width >> i, height >> i, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
		}
	}

	public static void uploadTexture(int id, int[] data, int w, int h) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		uploadTextureSub(0, data, w, h, 0, 0);
	}

	private static void uploadTextureSub(int id, int[] data, int w, int h, int xoff, int yoff) {
		int i = BUFFER_SIZE / w;
		int l;
		//System.out.println("start upload " + w + " x " + h + " " + id);
		for (int j = 0; j < w * h; j += w * l) {
			int k = j / w;
			l = Math.min(i, h - k);
			int i1 = w * l;
			//System.out.println("try upload " + (yoff + k) + " " + w + " " + l);
			copyToBuffer(data, j, i1);
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, id, xoff, yoff + k, w, l, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, DATA_BUFFER);
		}
	}

	public static HideTexture load(InputStream imageStream) {
		try {
			return new HideTexture(ImageIO.read(imageStream));
		} catch (IOException e) {
			e.printStackTrace();
			return null;//TODO
		} finally {
			IOUtils.closeQuietly(imageStream);
		}

	}
}
