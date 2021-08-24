package hide.opengl;

import java.io.File;
import java.lang.reflect.Field;

import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import com.google.common.base.Strings;

import hidemod.HideMod;

/**サーバーサイドでLWJGLで描画を走らせるためのコンテキスト*/
public class ServerRenderContext {
	private static Pbuffer pbuffer;

	public static boolean SUPPORT_CONTEXT = false;

	public static void initContext() {
		try {
			ClassLoader.getSystemClassLoader().loadClass("org.lwjgl.LWJGLUtil");
		} catch (ClassNotFoundException e1) {
			HideMod.LOGGER.error("lwjgl lib not found");
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
			return;
		}

		if (pbuffer != null) {
			HideMod.LOGGER.warn("context has already been created");
			return;
		}

		//Add native
		String paths = System.getProperty("java.library.path");

		File file = new File(HideMod.getModDir(), "lwjgl/" + LWJGLUtil.getPlatformName());

		if (!file.exists()) {
			HideMod.LOGGER.warn("lwjgl lib not found " + file);
			return;
		}

		if (Strings.isNullOrEmpty(paths))
			paths = file.getPath();
		else
			paths += File.pathSeparator + file.getPath();

		System.setProperty("java.library.path", paths);

		// kill classloader cacsh.
		try {
			final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
			sysPathsField.setAccessible(true);
			sysPathsField.set(null, null);
		} catch (Throwable t) {
		}

		try {
			final int width = 512;
			final int height = 512;

			// init pbuffer
			pbuffer = new Pbuffer(512, 512, new PixelFormat(), null, null);
			pbuffer.makeCurrent();

			if (pbuffer.isBufferLost()) {
				pbuffer.destroy();
				HideMod.LOGGER.error("pbuffer was be lost");
				return;
			}
			SUPPORT_CONTEXT = true;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void destroyContext() {
		System.out.println("SUPPORT_CONTEXT " + SUPPORT_CONTEXT);
		if (!SUPPORT_CONTEXT)
			return;
		destroyPBuffer();
	}

	private static void destroyPBuffer() {
		if (pbuffer != null)
			return;
		pbuffer.destroy();
		pbuffer = null;
	}
}
