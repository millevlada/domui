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
package to.etc.domui.component.form3;

import javax.annotation.*;

import org.slf4j.*;

import to.etc.domui.component.form.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.util.*;

/**
 * Base class for form builder engines.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 13, 2009
 */
abstract public class AbstractFormBuilder {
	static protected final Logger LOG = LoggerFactory.getLogger(AbstractFormBuilder.class);

	//	@Nonnull
	//	private ModelBindings m_bindings = new ModelBindings();

	@Nullable
	private ControlBuilder m_builder;

	@Nullable
	private IControlLabelFactory m_controlLabelFactory;

	private Object m_lastBuilderThingy;

	private FormData< ? > m_lastBuilder;

	@Nonnull
	private AllowedRight m_allowedRight = AllowedRight.ALL;

	/**
	 * Handle adding nodes generated by the form builder to the page.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Jun 13, 2012
	 */
	interface IAppender {
		void add(@Nonnull NodeBase formNode);
	}

	@Nonnull
	final private IAppender m_appender;

	abstract protected void addControl(@Nullable NodeBase label, @Nullable NodeBase labelnode, @Nonnull NodeBase[] list, boolean mandatory, boolean editable, PropertyMetaModel< ? > pmm);

	abstract protected void addContent(@Nullable NodeBase label, @Nonnull NodeBase[] control, boolean editable);

	abstract protected void startBulkLayout();

	abstract protected void endBulkLayout();

	/**
	 * Attach all nodes through the {@link IAppender} - all nodes are added using the
	 * {@link IAppender#add(NodeContainer)} method.
	 * @param a
	 */
	protected AbstractFormBuilder(@Nonnull IAppender a) {
		m_appender = a;
	}

	/**
	 *
	 * @param target
	 */
	protected AbstractFormBuilder(@Nonnull final NodeContainer target) {
		this(new IAppender() {
			@Override
			public void add(@Nonnull NodeBase formNode) {
				target.add(formNode);
			}
		});
	}

	/**
	 * Set rights for components created on this form.
	 * @param r
	 */
	public void setRight(@Nonnull AllowedRight r) {
		m_allowedRight = r;
	}

	@Nonnull
	public AllowedRight getRight() {
		return m_allowedRight;
	}

	final protected void addControl(@Nullable String label, @Nullable NodeBase labelnode, @Nonnull NodeBase[] list, boolean mandatory, boolean editable, @Nonnull PropertyMetaModel< ? > pmm) {
		IControlLabelFactory clf = getControlLabelFactory();
		if(clf == null) {
			clf = getControlBuilder().getControlLabelFactory();
			if(clf == null)
				throw new IllegalStateException("Programmer error: the DomApplication instance returned a null IControlLabelFactory!?!?!?!?");
		}
		Label l = clf.createControlLabel(labelnode, label, editable, mandatory, pmm);
		addControl(l, labelnode, list, mandatory, editable, pmm);
	}

	/**
	 * Called by builders to add nodes that form the form.
	 * @param node
	 */
	public void appendFormNode(@Nonnull NodeBase node) {
		m_appender.add(node);
	}

	@Nonnull
	final public ControlBuilder getControlBuilder() {
		ControlBuilder cb = m_builder;
		if(cb == null)
			cb = m_builder = DomApplication.get().getControlBuilder();
		return cb;
	}

	/**
	 * Create the optimal control for the specified thingy, and return the binding for it.
	 *
	 * @param container		This will receive all nodes forming the control.
	 * @param model 		The content model used to obtain the Object instance whose property is being edited, for binding purposes.
	 * @param pmm			The property meta for the property to find an editor for.
	 * @param editable		When false this must make a displayonly control.
	 * @return				The binding to bind the control to it's valueset
	 */
	@Nonnull
	protected ControlFactoryResult createControlFor(@Nonnull final IReadOnlyModel< ? > model, @Nonnull final PropertyMetaModel< ? > pmm, final boolean editable) {
		return getControlBuilder().createControlFor(model, pmm, editable, null); // Delegate
	}

	/**
	 * Add a fully manually specified label and control to the layout. This does not create any binding.
	 * @param label
	 * @param control
	 * @param mandatory
	 */
	public void addLabelAndControl(final String label, final NodeBase control, final boolean mandatory) {
		//-- jal 20090924 Bug 624 Assign the control label to all it's node so it can specify it in error messages
		if(label != null)
			control.setErrorLocation(label);

		// FIXME Kludge to determine if the control is meant to be editable!
		boolean editable = control instanceof IControl< ? >;
		Label lbl = new Label(label);

		addContent(lbl, new NodeBase[]{control}, editable);
		lbl.setForNode(control);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Simple getters and internal stuff.					*/
	/*--------------------------------------------------------------*/

	/**
	 * Return the factory to use for creating control labels from metadata.
	 * @return
	 */
	@Nullable
	public IControlLabelFactory getControlLabelFactory() {
		return m_controlLabelFactory;
	}

	public void setControlLabelFactory(@Nullable final IControlLabelFactory controlLabelFactory) {
		m_controlLabelFactory = controlLabelFactory;
	}

	@Nonnull
	public <T> FormData<T> data(@Nonnull T instance) {
		if(m_lastBuilderThingy == instance) {
			return (FormData<T>) m_lastBuilder;
		}
		FormData<T> b = new FormData<T>(this, instance);
		m_lastBuilder = b;
		m_lastBuilderThingy = instance;
		return b;
	}
}
