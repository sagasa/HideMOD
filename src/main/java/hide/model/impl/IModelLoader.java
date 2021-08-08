package hide.model.impl;

import java.io.InputStream;

public interface IModelLoader {
	ModelImpl load(InputStream stream);
}
