/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.util;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;

public class DefaultControlLabelFactory implements IControlLabelFactory {
	@Override
	public Label createControlLabel(NodeBase control, String text, boolean editable, boolean mandatory, PropertyMetaModel< ? > pmm) {
		if(text == null)
			return null;
		if(mandatory && editable)
			text = getMandatoryLabelText(text);
		if(pmm != null && editable && NumericPresentation.isMonetary(pmm.getNumericPresentation()))
			text = text + " \u20ac";

		Label l = new Label(control, text);
		l.setCssClass("ui-f-lbl");
		return l;
	}

	public String getMandatoryLabelText(String text) {
		return "* " + text;
	}
}
