package to.etc.domui.component.image;

import javax.annotation.*;
import javax.annotation.concurrent.*;
import java.io.*;

/**
 * An image that was updated/loaded by the user and has not yet been stored in wherever.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 5, 2014
 */
@Immutable
public class LoadedImage implements IUIImage {
	@Nonnull
	final private IUIImageInstance m_original;

	@Nonnull
	final private IUIImageInstance m_thumbnail;

	@Nonnull
	private final File m_source;

	public LoadedImage(@Nonnull IUIImageInstance original, @Nonnull IUIImageInstance thumbnail, @Nonnull File source) {
		m_original = original;
		m_thumbnail = thumbnail;
		m_source = source;
	}

	@Override
	public IUIImageInstance getImage() throws Exception {
		return m_original;
	}

	@Override
	public IUIImageInstance getThumbnail() throws Exception {
		return m_thumbnail;
	}

	@Nonnull
	public File getSource() {
		return m_source;
	}
}
