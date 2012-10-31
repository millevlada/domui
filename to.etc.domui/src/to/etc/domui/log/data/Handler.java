package to.etc.domui.log.data;

import java.util.*;

import javax.annotation.*;

import org.w3c.dom.*;

import to.etc.domui.component.meta.*;

public class Handler {
	private HandlerType m_type;

	public static final String pTYPE = "type";

	private String m_file;

	public static final String pFILE = "file";

	public List<Matcher> m_matchers = new ArrayList<Matcher>();

	public static final String pMATCHERS = "matchers";

	public List<Filter> m_filters = new ArrayList<Filter>();

	public static final String pFILTERS = "filters";

	public Handler(HandlerType type, String file) {
		super();
		m_type = type;
		m_file = file;
	}

	@MetaProperty(length = 30, required = YesNoType.YES)
	public String getFile() {
		return m_file;
	}

	public void setFile(String file) {
		m_file = file;
	}

	@MetaProperty(required = YesNoType.YES)
	public HandlerType getType() {
		return m_type;
	}

	public void setType(HandlerType type) {
		m_type = type;
	}

	public List<Matcher> getMatchers() {
		return m_matchers;
	}

	public void setMatchers(List<Matcher> matchers) {
		m_matchers = matchers;
	}

	public List<Filter> getFilters() {
		return m_filters;
	}

	public void setFilters(List<Filter> filters) {
		m_filters = filters;
	}

	public void addMatcher(@Nonnull Matcher matcher) {
		m_matchers.add(matcher);
	}

	public void addFilter(@Nonnull Filter filter) {
		m_filters.add(filter);
	}

	public void saveToXml(Document doc, Element handlerNode) {
		handlerNode.setAttribute("type", m_type == HandlerType.STDOUT ? "stdout" : "file");
		if(m_type != HandlerType.STDOUT) {
			handlerNode.setAttribute("file", m_file);
		}
		for(Matcher matcher : m_matchers) {
			Element logNode = doc.createElement("log");
			handlerNode.appendChild(logNode);
			matcher.saveToXml(doc, logNode);
		}
		for(Filter filter : m_filters) {
			Element filterNode = doc.createElement("filter");
			handlerNode.appendChild(filterNode);
			filter.saveToXml(doc, filterNode);
		}
	}
}
