package to.etc.log.handler;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.annotation.*;

import org.w3c.dom.*;

import to.etc.log.*;
import to.etc.log.EtcLoggerFactory.LoggerConfigException;
import to.etc.log.event.*;

class FileLogHandler implements ILogHandler {
	/**
	 * Defines where to write log output.  
	 */
	private final String			m_out;

	private final File				m_logRoot;
	/**
	 * Defines matchers to calculate on which logger handler applies. To apply on logger, matcher closest to logger name must match with logEvent.  
	 */
	private final List<LogMatcher>		m_matchers	= new ArrayList<LogMatcher>();

	/**
	 * Defines filters on which handler applies. To apply on logger, all filters must be matched.  
	 */
	private List<LogFilter>			m_filters	= Collections.EMPTY_LIST;

	/**
	 * Keeps list of loggers that are marked as handled by handler. 
	 */
	private final Map<EtcLogger, Boolean[]>	m_loggers	= new HashMap<EtcLogger, Boolean[]>();

	private final Object					m_writeLock	= new Object();

	private static final DateFormat	m_df		= new SimpleDateFormat("yyMMdd");

	public FileLogHandler(@Nonnull File logRoot, @Nullable String out) {
		super();
		m_logRoot = logRoot;
		m_out = out;
	}

	public static FileLogHandler createDefaultHandler(@Nonnull File logRoot, @Nonnull Level level) {
		FileLogHandler handler = new FileLogHandler(logRoot, null);
		LogMatcher matcher = new LogMatcher("", level);
		handler.addMatcher(matcher);
		return handler;
	}

	public void addMatcher(LogMatcher matcher) {
		m_matchers.add(matcher);
		m_loggers.clear();
	}

	public void addFilter(LogFilter filter) {
		if(m_filters == Collections.EMPTY_LIST) {
			m_filters = new ArrayList<LogFilter>();
		}
		m_filters.add(filter);
	}

	@Override
	public void handle(LogEvent event) {
		Boolean[] applicablePerLevels = m_loggers.get(event.getLogger());
		if(null == applicablePerLevels) {
			applicablePerLevels = new Boolean[Level.values().length];
			m_loggers.put(event.getLogger(), applicablePerLevels);
		}
		Boolean isApplicable = applicablePerLevels[event.getLevel().getCode()];
		if(isApplicable == null) {
			isApplicable = decideOnMatchers(event);
			applicablePerLevels[event.getLevel().getCode()] = isApplicable;
		}
		if(isApplicable.booleanValue()) {
			if(checkFilters(event)) {
				log(event);
			}
		}
	}

	private synchronized void log(@Nonnull LogEvent event) {
		String line = LogFormatter.format(event, getLogPartFromFilters());

		synchronized(m_writeLock) {
			if(m_out == null) {
				System.out.println(line);
			} else {
				BufferedWriter w = null;
				String fileName = null;
				if(m_out.contains(":")) {
					fileName = m_out;
				} else {
					fileName = m_logRoot + File.separator + m_out;
				}

				fileName += "_" + m_df.format(new Date()) + ".log";

				File outFile = new File(fileName);
				outFile.getParentFile().mkdirs();
				try {
					outFile.createNewFile();
					w = new BufferedWriter(new FileWriter(outFile, true));
					w.write(line);
					w.newLine();
				} catch(IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} finally {
					if(w != null) {
						try {
							w.close();
						} catch(IOException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}
		
	private @Nullable
	String getLogPartFromFilters() {
		if(m_filters.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(LogFilter filter : m_filters) {
			sb.append("[").append(filter.getKey()).append("=").append(filter.getValue()).append("]");
		}
		return sb.toString();
	}

	private Boolean decideOnMatchers(LogEvent event) {
		LogMatcher closest = null;
		for(LogMatcher matcher : m_matchers) {
			if(matcher.matches(event)) {
				if(closest == null || matcher.isSubmatcherOf(closest)) {
					closest = matcher;
				}
			}
		}
		if(closest != null && closest.getLevel().includes(event.getLevel())) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	private boolean checkFilters(LogEvent event) {
		for(LogFilter filter : m_filters) {
			if(!filter.accept(event)) {
				return false;
			}
		}
		return true;
	}

	public File getLogRoot() {
		return m_logRoot;
	}

	@Override
	@Nullable
	public Level listenAt(@Nonnull String key) {
		LogMatcher closest = null;
		for(LogMatcher matcher : m_matchers) {
			if(matcher.matchesName(key)) {
				if(closest == null || matcher.isSubmatcherOf(closest)) {
					closest = matcher;
				}
			}
		}
		return closest != null ? closest.getLevel() : null;
	}

	public static FileLogHandler createFromFileTypeConfig(@Nonnull File logRoot, @Nonnull Node handlerNode) throws LoggerConfigException {
		Node file = handlerNode.getAttributes().getNamedItem("file");
		if(file == null) {
			throw new EtcLoggerFactory.LoggerConfigException("Missing file attribute inside file type handler.");
		}
		FileLogHandler res = new FileLogHandler(logRoot, file.getNodeValue());
		res.load(handlerNode);
		return res;
	}

	void load(Node handlerNode) throws LoggerConfigException {
		NodeList nodes = handlerNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if("log".equals(node.getNodeName())) {
				addMatcher(LogMatcher.createFromXml(node));
			} else if("filter".equals(node.getNodeName())) {
				addFilter(LogFilter.createFromXml(node));
			}
		}
	}

	public static FileLogHandler createFromStdoutTypeConfig(@Nonnull File logRoot, @Nonnull Node handlerNode) throws LoggerConfigException {
		FileLogHandler res = new FileLogHandler(logRoot, null);
		res.load(handlerNode);
		return res;
	}

	@Override
	public void saveToXml(Document doc, Element handlerNode, boolean includeNonPerstistable) {
		handlerNode.setAttribute("type", m_out == null ? "stdout" : "file");
		if(m_out != null) {
			handlerNode.setAttribute("file", m_out);
		}
		for(LogMatcher matcher : m_matchers) {
			Element logNode = doc.createElement("log");
			handlerNode.appendChild(logNode);
			matcher.saveToXml(doc, logNode);
		}
		for(LogFilter filter : m_filters) {
			if(includeNonPerstistable || filter.getType().isPersistable()) {
				Element filterNode = doc.createElement("filter");
				handlerNode.appendChild(filterNode);
				filter.saveToXml(doc, filterNode);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("handler ").append(m_out != null ? "file: " + m_out : "stdout");
		if(!m_matchers.isEmpty()) {
			sb.append("\nmatchers: ");
			for(LogMatcher matcher : m_matchers) {
				sb.append("[").append(matcher.toString()).append("]");
			}
		}
		if(!m_filters.isEmpty()) {
			sb.append("\nfilters: ");
			for(LogFilter filter : m_filters) {
				sb.append("[").append(filter.toString()).append("]");
			}
		}
		return sb.toString();
	};

}
