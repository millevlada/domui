package to.etc.domui.component.form;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * Encapsulates basic actions that can be done with all form builder implementations,
 * and delegates the actual parts that require layout decisions to the actual
 * implementation.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 13, 2009
 */
abstract public class GenericFormBuilder extends FormBuilderBase {
	/**
	 * This is the actual workhorse doing the per-builder actual placement and layouting of a {control, label} pair.
	 *
	 * @param label
	 * @param labelnode
	 * @param list
	 * @param mandatory
	 * @param pmm
	 */
	abstract protected void addControl(final String label, final NodeBase labelnode, final NodeBase[] list, final boolean mandatory, PropertyMetaModel pmm);

	/**
	 * Handle placement of a list of property names, all obeying the current mode in effect.
	 * @param editable
	 * @param names
	 */
	abstract protected void addListOfProperties(boolean editable, final String... names);

	/**
	 * Default ctor.
	 */
	public GenericFormBuilder() {}

	/**
	 * Create one primed with a model and class.
	 * @param <T>
	 * @param clz
	 * @param mdl
	 */
	public <T> GenericFormBuilder(Class<T> clz, IReadOnlyModel<T> mdl) {
		super(clz, mdl);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Worker implementation for common tasks.				*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param name
	 * @param label
	 * @param pmm
	 * @param editPossible, when false, the rendered control will be display-only and cannot be changed back to EDITABLE.
	 */
	protected void addPropertyControl(final String name, final String label, final PropertyMetaModel pmm, final boolean editPossible) {
		//-- Check control permissions: does it have view permissions?
		if(!rights().calculate(pmm))
			return;
		final ControlFactory.Result r = createControlFor(getModel(), pmm, editPossible && rights().isEditable()); // Add the proper input control for that type
		addControl(label, r.getLabelNode(), r.getNodeList(), pmm.isRequired(), pmm);
		if(r.getBinding() != null)
			getBindings().add(r.getBinding());
		else
			throw new IllegalStateException("No binding for a " + r);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Core shared public interface - all builders.		*/
	/*--------------------------------------------------------------*/
	/**
	 * Add an input for the specified property. The property is based at the current input
	 * class. The input model is default (using metadata) and the property is labeled using
	 * the metadata-provided label.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param name
	 */
	public void addProp(final String name) {
		addProp(name, (String) null);
	}

	/**
	 * Add an input for the specified property just as <code>addProp(String)</code>,
	 * only this input won't be editable.
	 *
	 * @param name
	 */
	public void addReadOnlyProp(final String name) {
		addReadOnlyProp(name, null);
	}

	/**
	 * Add an input for the specified property. The property is based at the current input
	 * class. The input model is default (using metadata) and the property is labeled.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param name
	 * @param label		The label text to use. Use the empty string to prevent a label from being generated. This still adds an empty cell for the label though.
	 */
	public void addProp(final String name, String label) {
		PropertyMetaModel pmm = resolveProperty(name);
		if(label == null)
			label = pmm.getDefaultLabel();
		boolean edit = true;
		if(pmm.getReadOnly() == YesNoType.YES)
			edit = false;
		addPropertyControl(name, label, pmm, edit);
	}

	/**
	 * Add an input for the specified property just as <code>addProp(String, String)</code>,
	 * only this input won't be editable.
	 *
	 * @param name
	 * @param label
	 */
	public void addReadOnlyProp(final String name, String label) {
		PropertyMetaModel pmm = resolveProperty(name);
		if(label == null)
			label = pmm.getDefaultLabel();
		addPropertyControl(name, label, pmm, false);
	}

	/**
	 * Add a user-specified control for a given property. This adds the control, using
	 * the property-specified label and creates a binding for the property on the
	 * control. <i>If you only want to add the proper structure and find the label for
	 * a property use {@link TabularFormBuilder#addPropertyAndControl(String, NodeBase, boolean)}.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param propertyname
	 * @param ctl
	 */
	public <T extends NodeBase & IInputNode< ? >> void addProp(final String propertyname, final T ctl) {
		PropertyMetaModel pmm = resolveProperty(propertyname);
		String label = pmm.getDefaultLabel();
		addControl(label, ctl, new NodeBase[]{ctl}, ctl.isMandatory(), pmm);
		getBindings().add(new SimpleComponentPropertyBinding(getModel(), pmm, ctl));
	}

	/**
	 * Add a fully manually specified label and control to the layout. This does not create any binding.
	 * @param label
	 * @param control
	 * @param mandatory
	 */
	public void addLabelAndControl(final String label, final NodeBase control, final boolean mandatory) {
		addControl(label, control, new NodeBase[]{control}, mandatory, null);
	}

	/**
	 * Add an input for the specified property. The property is based at the current input
	 * class. The input model is default (using metadata) and the property is labeled using
	 * the metadata-provided label.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param name
	 * @param readOnly In case of readOnly set to true behaves same as addReadOnlyProp.
	 */
	public void addProp(final String name, final boolean readOnly) {
		if(readOnly) {
			addReadOnlyProp(name);
		} else {
			addProp(name);
		}
	}

	/**
	 * This adds a fully user-specified control for a given property with it's default label,
	 * without creating <i>any<i> binding. The only reason the property is passed is to use
	 * it's metadata to define it's access rights and default label.
	 *
	 * @param propertyName
	 * @param nb
	 * @param mandatory
	 */
	public void addPropertyAndControl(final String propertyName, final NodeBase nb, final boolean mandatory) {
		PropertyMetaModel pmm = resolveProperty(propertyName);
		String label = pmm.getDefaultLabel();
		addControl(label, nb, new NodeBase[]{nb}, mandatory, pmm);
	}

	/**
	 * Add the specified properties to the form, in the current mode. Watch out: if a
	 * MODIFIER is in place the modifier is obeyed for <b>all properties</b>, not for
	 * the first one only!! This means that when this gets called like:
	 * <pre>
	 * 	f.append().addProps("a", "b","c");
	 * </pre>
	 * all three fields are appended to the current row.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param names
	 */
	public GenericFormBuilder addProps(final String... names) {
		addListOfProperties(true, names);
		return this;
	}

	/**
	 * Add the specified properties to the form as READONLY properties, in the current
	 * mode. Watch out: if a MODIFIER is in place the modifier is obeyed for <b>all
	 * properties</b>, not for the first one only!! This means that when this gets called
	 * like:
	 * <pre>
	 * 	f.append().addProps("a", "b","c");
	 * </pre>
	 * all three fields are appended to the current row.
	 *
	 * FORMAL-INTERFACE.
	 *
	 * @param names
	 */
	public GenericFormBuilder addReadOnlyProps(final String... names) {
		addListOfProperties(false, names);
		return this;
	}
}
