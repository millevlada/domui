package to.etc.domui.util.images.converters;

import java.io.*;

import to.etc.domui.util.images.cache.*;
import to.etc.domui.util.images.machines.*;

public interface IImageIdentifier {
	public OriginalImageData identifyImage(File src, String mime);
}