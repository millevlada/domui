package to.etc.domui.dom.html;

import to.etc.domui.dom.errors.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;

public class TextArea extends InputNodeContainer implements IInputNode<String> {
	private int m_cols = -1;

	private int m_rows = -1;

	private String m_value;

	private boolean m_disabled;

	public TextArea() {
		super("textarea");
	}

	public TextArea(int cols, int rows) {
		this();
		m_cols = cols;
		m_rows = rows;
	}


	@Override
	public void visit(INodeVisitor v) throws Exception {
		v.visitTextArea(this);
	}

	public int getCols() {
		return m_cols;
	}

	public void setCols(int cols) {
		if(m_cols == cols)
			return;
		changed();
		m_cols = cols;
	}

	public int getRows() {
		return m_rows;
	}

	public void setRows(int rows) {
		if(m_rows == rows)
			return;
		changed();
		m_rows = rows;
	}

	@Override
	public boolean validate() {
		if(m_value == null || m_value.length() == 0) {
			if(isMandatory()) {
				setMessage(MsgType.ERROR, Msgs.MANDATORY);
				return false;
			}
		}
		return true;
	}

	public String getValue() {
		if(!validate())
			throw new ValidationException(Msgs.NOT_VALID, m_value);
		return m_value;
	}

	public boolean isDisabled() {
		return m_disabled;
	}

	public void setDisabled(boolean disabled) {
		if(m_disabled == disabled)
			return;
		changed();
		m_disabled = disabled;
	}

	public void setValue(String v) {
		if(DomUtil.isEqual(v, m_value))
			return;
		m_value = v;
		setText(v);
	}

	@Override
	public void acceptRequestParameter(String[] values) throws Exception {
		if(values == null || values.length != 1)
			setValue(null);
		else
			setValue(values[0]);
	}
}
