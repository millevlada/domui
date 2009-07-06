package to.etc.domui.server.parts;

import java.io.*;
import java.util.*;

import to.etc.domui.server.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;
import to.etc.domui.util.LRUHashMap;
import to.etc.domui.util.resources.*;
import to.etc.util.*;

public class PartRequestHandler implements IFilterRequestHandler {
	private final DomApplication m_application;

	private final boolean m_allowExpires;

	/**
	 * Contains a cached instance of some part rendering.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Jun 4, 2008
	 */
	static private class CachedPart {
		public byte[][] m_data;

		public int m_size;

		public ResourceDependencyList m_dependencies;

		public String m_contentType;

		//		public String		m_key;

		/** The time a response may be cached locally, in seconds */
		public int m_cacheTime;

		CachedPart() {}
	}


	public PartRequestHandler(final DomApplication application) {
		m_application = application;

		LRUHashMap.SizeCalculator<CachedPart> sc = new LRUHashMap.SizeCalculator<CachedPart>() {
			public int getObjectSize(final CachedPart item) {
				return item == null ? 4 : item.m_size + 32;
			}
		};

		m_cache = new LRUHashMap<Object, CachedPart>(sc, 16 * 1024 * 1024); // Accept 16MB of resources FIXME Must be parameterized

		m_allowExpires = DeveloperOptions.getBool("domui.expires", true);
	}

	DomApplication getApplication() {
		return m_application;
	}

	public boolean acceptURL(final String in) {
		if(in.endsWith(".part"))
			return true;
		int pos = in.indexOf('/'); // First component
		if(pos < 0)
			return false;
		String seg = in.substring(0, pos);
		if(seg.endsWith(".part"))
			return true;
		return false;
	}

	public void handleRequest(final RequestContextImpl ctx) throws Exception {
		String input = ctx.getInputPath();
		if(input.endsWith(".part"))
			input = input.substring(0, input.length() - 5); // Strip ".part" off the name
		int pos = input.indexOf('/'); // First path component is the factory name,
		String fname, rest;
		if(pos == -1) {
			fname = input;
			rest = "";
		} else {
			fname = input.substring(0, pos);
			rest = input.substring(pos + 1);
		}
		if(fname.endsWith(".part"))
			fname = fname.substring(0, fname.length() - 5);

		//-- Obtain the factory class, then ask it to execute.
		IPartRenderer pr = findPartRenderer(fname);
		if(pr == null)
			throw new ThingyNotFoundException("The part factory '" + fname + "' cannot be located.");
		pr.render(ctx, rest);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Part renderer factories.							*/
	/*--------------------------------------------------------------*/
	/** All part renderer thingies currently known to the system. */
	private final Map<String, IPartRenderer> m_partMap = new HashMap<String, IPartRenderer>();

	static private final IPartFactory makePartInst(final Class< ? > fc) {
		try {
			return (IPartFactory) fc.newInstance();
		} catch(Exception x) {
			throw new IllegalStateException("Cannot instantiate PartFactory '" + fc + "': " + x, x);
		}
	}

	/**
	 * Returns a thingy which knows how to render the part.
	 */
	public synchronized IPartRenderer findPartRenderer(final String name) {
		IPartRenderer pr = m_partMap.get(name);
		if(pr != null)
			return pr;

		//-- Try to locate the factory class passed,
		Class< ? > fc = DomUtil.findClass(getClass().getClassLoader(), name);
		if(fc == null)
			return null;
		if(!IPartFactory.class.isAssignableFrom(fc))
			throw new IllegalArgumentException("The class '" + name
				+ "' does not implement the 'PartFactory' interface (it is not a part, I guess. WHAT ARE YOU DOING!? Access logged to administrator)");

		//-- Create the appropriate renderers depending on the factory type.
		final IPartFactory pf = makePartInst(fc); // Instantiate
		if(pf instanceof IUnbufferedPartFactory) {
			pr = new IPartRenderer() {
				public void render(final RequestContextImpl ctx, final String rest) throws Exception {
					IUnbufferedPartFactory upf = (IUnbufferedPartFactory) pf;
					upf.generate(getApplication(), rest, ctx);
				}
			};
		} else if(pf instanceof IBufferedPartFactory) {
			pr = new IPartRenderer() {
				public void render(final RequestContextImpl ctx, final String rest) throws Exception {
					generate((IBufferedPartFactory) pf, ctx, rest); // Delegate internally
				}
			};
		} else
			throw new IllegalStateException("??Internal: don't know how to handle part factory " + fc);

		m_partMap.put(name, pr);
		return pr;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Buffered parts cache and code.						*/
	/*--------------------------------------------------------------*/
	private final LRUHashMap<Object, CachedPart> m_cache;

	/**
	 * Helper which handles possible cached buffered parts.
	 * @param pf
	 * @param ctx
	 * @param url
	 * @throws Exception
	 */
	public void generate(final IBufferedPartFactory pf, final RequestContextImpl ctx, final String url) throws Exception {
		//-- Convert the data to a key object, then lookup;
		Object key = pf.decodeKey(url, ctx);
		if(key == null)
			throw new ThingyNotFoundException("Cannot get resource for " + pf + " with rurl=" + url);

		/*
		 * Lookup. This part *is* thread-safe but it has a race condition: it may cause multiple
		 * instances of the SAME resource to be generated at the same time and inserted at the
		 * same time. In time we must replace this with the MAKER pattern, but for now this
		 * small problem will be accepted; it will not cause problems since only the last instance
		 * will be kept and stored.
		 */
		CachedPart cp;
		synchronized(m_cache) {
			cp = m_cache.get(key); // Already exists here?
		}
		if(cp != null && m_application.inDevelopmentMode()) {
			if(cp.m_dependencies != null) {
				if(cp.m_dependencies.isModified()) {
					System.out.println("parts: part " + key + " has changed (DEVMODE).. Reloading..");
					cp = null;
				}
			}
		}

		if(cp == null) {
			//-- We're going to create the part
			cp = new CachedPart(); // New one to be done,
			ResourceDependencyList rdl = m_application.inDevelopmentMode() ? new ResourceDependencyList() : null;
			ByteBufferOutputStream os = new ByteBufferOutputStream();
			PartResponse pr = new PartResponse(os);
			pf.generate(pr, m_application, key, rdl);
			cp.m_contentType = pr.getMime();
			if(cp.m_contentType == null)
				throw new IllegalStateException("The part " + pf + " did not set a MIME type");
			os.close();
			cp.m_size = os.getSize();
			cp.m_data = os.getBuffers();
			cp.m_dependencies = rdl;
			cp.m_cacheTime = pr.getCacheTime();
			synchronized(m_cache) {
				m_cache.put(key, cp); // Store (may be done multiple times due to race condition)
			}
		}

		//-- Generate the part
		OutputStream os = null;
		if(cp.m_cacheTime > 0 && m_allowExpires) {
			ServerTools.generateExpiryHeader(ctx.getResponse(), cp.m_cacheTime); // Allow browser-local caching.
		}
		ctx.getResponse().setContentType(cp.m_contentType);
		ctx.getResponse().setContentLength(cp.m_size);

		try {
			os = ctx.getResponse().getOutputStream();
			for(byte[] data : cp.m_data)
				os.write(data);
		} finally {
			try {
				if(os != null)
					os.close();
			} catch(Exception x) {}
		}
	}
}
