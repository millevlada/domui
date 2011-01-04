package to.etc.domui.component.graph;

import to.etc.domui.dom.header.*;
import to.etc.domui.dom.html.*;

/**
 * Color picker using the color picker from: http://www.eyecon.ro/colorpicker
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 4, 2011
 */
public class ColorPicker extends Div {
	private Input m_hidden = new HiddenInput();

	/**
	 * Create the required structure.
	 * @see to.etc.domui.dom.html.NodeBase#createContent()
	 */
	@Override
	public void createContent() throws Exception {
		add(m_hidden);
		if(m_hidden.getRawValue() == null)
			m_hidden.setRawValue("ffffff");
		appendCreateJS("$('#" + getActualID() + "').ColorPicker({flat: true, color:'" + m_hidden.getRawValue() + "' });");
		//		appendCreateJS("$('#" + getActualID() + "').ColorPicker({flat: true, onBeforeShow: function() {$(this).ColorPickerSetColor($('" + m_hidden.getActualID() + "').value)} });");
	}

	@Override
	public void onAddedToPage(Page p) {
		p.addHeaderContributor(HeaderContributor.loadJavascript("$js/colorpicker.js"), 100);
	}

	public String getValue() {
		return m_hidden.getRawValue();
	}

	public void setValue(String value) {
		if(value == null)
			value = "000000"; // We do not allow null here.
		if(value.startsWith("#"))
			value = value.substring(1); // Remove any #
		m_hidden.setRawValue(value); // Set the color value;
		if(!isBuilt())
			return;

		//-- Force update existing value.
		appendJavascript("$('#" + getActualID() + "').ColorPickerSetColor('" + value + "');");
	}
}
