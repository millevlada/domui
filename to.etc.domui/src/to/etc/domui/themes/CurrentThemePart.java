package to.etc.domui.themes;

import java.io.*;
import java.util.*;

import javax.annotation.*;

import to.etc.domui.server.*;
import to.etc.domui.server.parts.*;
import to.etc.domui.util.resources.*;

/**
 * This handles all URLs that start with "currentTheme/", and locates the appropriate resources
 * that belong there.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 12, 2011
 */
public class CurrentThemePart implements IBufferedPartFactory, IUrlPart {
	static public final String CURRENTTHEME = "currentTheme/";

	static public final String SHEETNAME = "style.css";

	static private class Key {
		private String m_browserID;

		private BrowserVersion m_bv;

		private String m_name;

		public Key(@Nonnull String name, @Nullable BrowserVersion bv) {
			m_name = name;
			m_bv = bv;
			if(null != bv)
				m_browserID = bv.getBrowserName() + "/" + bv.getMajorVersion();
		}

		@Override
		public String toString() {
			return "[stylesheet: browser=" + m_bv + "]";
		}

		public BrowserVersion getBrowserVersion() {
			return m_bv;
		}
		public String getName() {
			return m_name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_browserID == null) ? 0 : m_browserID.hashCode());
			result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if(m_browserID == null) {
				if(other.m_browserID != null)
					return false;
			} else if(!m_browserID.equals(other.m_browserID))
				return false;
			if(m_name == null) {
				if(other.m_name != null)
					return false;
			} else if(!m_name.equals(other.m_name))
				return false;
			return true;
		}
	}

	/**
	 * Accept all RURLs that start with "currentTheme/".
	 * @see to.etc.domui.server.parts.IUrlPart#accepts(java.lang.String)
	 */
	@Override
	public boolean accepts(String rurl) {
		return rurl.startsWith(CURRENTTHEME);
	}

	@Override
	public Object decodeKey(String rurl, IExtendedParameterInfo param) throws Exception {
		String name = rurl.substring(CURRENTTHEME.length());
		BrowserVersion	bv = null;
		if(name.equals(SHEETNAME)) {
			bv = param.getBrowserVersion();
		}
		return new Key(name, bv);
	}

	@Override
	public void generate(PartResponse pr, DomApplication da, Object key, ResourceDependencyList rdl) throws Exception {
		Key k = (Key) key;
		if(k.getName().equals(SHEETNAME)) {
			//-- Export
			if(!da.inDevelopmentMode()) { // Not gotten from WebContent or not in DEBUG mode? Then we may cache!
				pr.setCacheTime(da.getDefaultExpiryTime());
			}

			DefaultThemeStore dts = (DefaultThemeStore) da.getTheme(rdl); // Get theme store and add it's dependencies to rdl
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(pr.getOutputStream()));
			Map<String, Object>	map = new HashMap<String, Object>(dts.getThemeProperties());
			map.put("browser", k.getBrowserVersion());
			dts.getStylesheetTemplate().execute(pw, map);
			pw.close();
			pr.setMime("text/css");
			return;
		}
	}
}