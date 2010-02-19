package to.etc.domui.server;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;

import to.etc.domui.util.*;
import to.etc.log.*;
import to.etc.net.*;
import to.etc.util.*;
import to.etc.webapp.nls.*;

/**
 * Base filter which accepts requests to the dom windows. This accepts all URLs that end with a special
 * suffix and redigates them to the appropriate handler.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on May 22, 2008
 */
public class AppFilter implements Filter {
	static final Logger LOG = Logger.getLogger(AppFilter.class.getName());

	private ConfigParameters m_config;

	private String m_applicationClassName;

	private boolean m_logRequest;

	static private String m_appContext;

	/**
	 * If a reloader is needed for debug/development pps this will hold the reloader.
	 */

	private IContextMaker m_contextMaker;

	public void destroy() {}

	static public String minitime() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.HOUR_OF_DAY) + StringTool.intToStr(cal.get(Calendar.MINUTE), 10, 2) + StringTool.intToStr(cal.get(Calendar.SECOND), 10, 2) + "." + cal.get(Calendar.MILLISECOND);
	}

	public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
		try {
			HttpServletRequest rq = (HttpServletRequest) req;
			rq.setCharacterEncoding("UTF-8"); // FIXME jal 20080804 Encoding of input was incorrect?
			if(m_logRequest) {
				String rs = rq.getQueryString();
				rs = rs == null ? "" : "?" + rs;
				System.out.println(minitime() + " rq=" + rq.getRequestURI() + rs);
			}
			NlsContext.setLocale(rq.getLocale());
			//			NlsContext.setLocale(new Locale("nl", "NL"));
			initContext(req);

			if(m_contextMaker.handleRequest(rq, (HttpServletResponse) res, chain))
				return;
		} catch(RuntimeException x) {
			DomUtil.dumpException(x);
			throw x;
		} catch(ServletException x) {
			DomUtil.dumpException(x);
			throw x;
		} catch(IOException x) {
			DomUtil.dumpException(x);
			throw x;
		} catch(Exception x) {
			DomUtil.dumpException(x);
			throw new WrappedException(x); // James Gosling is an idiot
		} catch(Error x) {
			x.printStackTrace();
			throw x;
		}
	}

	static synchronized private void initContext(ServletRequest req) {
		if(m_appContext != null || !(req instanceof HttpServletRequest))
			return;

		m_appContext = NetTools.getApplicationContext((HttpServletRequest) req);
	}

	static synchronized public String internalGetWebappContext() {
		return m_appContext;
	}


	/**
	 * Initialize by reading config from the web.xml.
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(final FilterConfig config) throws ServletException {
		try {
			java.util.logging.LogManager.getLogManager().reset();
			java.util.logging.LogManager.getLogManager().readConfiguration(AppFilter.class.getResourceAsStream("logging.properties"));

			Logger root = LogManager.getLogManager().getLogger("");
			for(Handler h : root.getHandlers()) {
				if(h instanceof ConsoleHandler) {
					ConsoleHandler ch = (ConsoleHandler) h;
					ch.setFormatter(new NonStupidLogFormatter());
					System.out.println("**** changed ConsoleHandler logger.");
				}
			}

		} catch(Exception x) {
			x.printStackTrace();
			throw WrappedException.wrap(x);
		} catch(Error x) {
			x.printStackTrace();
			throw x;
		}
		try {
			m_logRequest = DeveloperOptions.getBool("domui.logurl", false);

			//-- Get the root for all files in the webapp
			File approot = new File(config.getServletContext().getRealPath("/"));
			System.out.println("WebApp root=" + approot);
			if(!approot.exists() || !approot.isDirectory())
				throw new IllegalStateException("Internal: cannot get webapp root directory");

			m_config = new ConfigParameters(config, approot);

			//-- Handle application construction
			m_applicationClassName = getApplicationClassName(m_config);
			if(m_applicationClassName == null)
				throw new UnavailableException("The application class name is not set. Use 'application' in the Filter parameters to set a main class.");

			String autoload = m_config.getString("auto-reload");
			if(autoload != null && autoload.trim().length() > 0)
				m_contextMaker = new ReloadingContextMaker(m_applicationClassName, m_config, autoload);
			else
				m_contextMaker = new NormalContextMaker(m_applicationClassName, m_config);
		} catch(RuntimeException x) {
			DomUtil.dumpException(x);
			throw x;
		} catch(ServletException x) {
			DomUtil.dumpException(x);
			throw x;
		} catch(Exception x) {
			DomUtil.dumpException(x);
			throw new RuntimeException(x); // James Gosling is an idiot
		} catch(Error x) {
			x.printStackTrace();
			throw x;
		}
	}

	public String getApplicationClassName(final ConfigParameters p) {
		return p.getString("application");
	}
}
