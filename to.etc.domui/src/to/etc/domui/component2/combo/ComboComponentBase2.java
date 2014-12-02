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
package to.etc.domui.component2.combo;

import to.etc.domui.component.buttons.*;
import to.etc.domui.component.input.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;
import to.etc.util.*;
import to.etc.webapp.query.*;

import javax.annotation.*;
import java.util.*;

/**
 * Alternate version of the combobox that wraps a select instead of being one. This version properly
 * handles "readonly" and extra buttons.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 27, 2014
 */
public class ComboComponentBase2<T, V> extends Div implements IControl<V>, IHasModifiedIndication {
	/** The properties bindable for this component. */
	static private final Set<String> BINDABLE_SET = createNameSet("value", "disabled", "message");

	private String m_emptyText;

	private V m_currentValue;

	@Nonnull
	final private Select m_select = new Select() {
		@Override
		protected boolean internalOnUserInput(int oldindex, int nindex) {
			return ComboComponentBase2.this.internalOnUserInput(oldindex, nindex);
		}
	};

	/**
	 * If this combobox has a "unselected" option currently this contains that option. When present it
	 * means that indexes in the <i>combo</i> list are one <i>higher</i> than indexes in the backing
	 * dataset (because this empty option is always choice# 0).
	 */
	private SelectOption m_emptyOption;

	private List<T> m_data;

	/** The specified ComboRenderer used. */
	private INodeContentRenderer<T> m_contentRenderer;

	private INodeContentRenderer<T> m_actualContentRenderer;

	private Class< ? extends INodeContentRenderer<T>> m_contentRendererClass;

	private PropertyMetaModel< ? > m_propertyMetaModel;

	/** When set this maker will be used to provide a list of values for this combo. */
	private IListMaker<T> m_listMaker;

	private Class< ? extends IComboDataSet<T>> m_dataSetClass;

	private IComboDataSet<T> m_dataSet;

	private IValueTransformer<V> m_valueTransformer;

	private boolean m_readOnly;

	private List<SmallImgButton> m_buttonList = Collections.EMPTY_LIST;

	public ComboComponentBase2() {}

	public ComboComponentBase2(@Nonnull IListMaker<T> maker) {
		m_listMaker = maker;
	}

	public ComboComponentBase2(@Nonnull IComboDataSet<T> dataSet) {
		m_dataSet = dataSet;
	}

	public ComboComponentBase2(@Nonnull QCriteria<T> query) {
		m_dataSet = new CriteriaComboDataSet<T>(query);
	}

	public ComboComponentBase2(Class< ? extends IComboDataSet<T>> dataSetClass) {
		m_dataSetClass = dataSetClass;
	}

	public ComboComponentBase2(@Nonnull List<T> in) {
		m_data = in;
	}

	public ComboComponentBase2(Class< ? extends IComboDataSet<T>> set, INodeContentRenderer<T> r) {
		m_dataSetClass = set;
		m_contentRenderer = r;
	}

	/**
	 * Render the actual combobox. This renders the value domain as follows:
	 * <ul>
	 *	<li>If the combobox is <i>optional</i> then the value list will always start with an "unselected" option
	 *		which will be shown if the value is null.</li>
	 *	<li>If the combobox is mandatory <i>but</i> it's current value is not part of the value domain (i.e. it
	 *		is null, or the value cannot be found in the list of values) then it <i>also</i> renders an initial
	 *		"unselected" option value which will become selected.</li>
	 *	<li>For a mandatory combo with a valid value the "empty" choice will not be rendered.</li>
	 * </ul>
	 * Fixes bug# 790.
	 */
	@Override
	public void createContent() throws Exception {
		if(m_readOnly) {
			renderReadOnly();
		} else {
			renderEditable();
		}
		for(SmallImgButton sib : m_buttonList) {
			add(sib);
		}
	}

	/**
	 * Render the display-only presentation of this combo box.
	 * @throws Exception
	 */
	private void renderReadOnly() throws Exception {
		setCssClass("ui-cbb2 ui-cbb2-ro");

		//-- Append shtuff to the combo
		List<T> list = getData();
		V raw = internalGetCurrentValue();
		int index = findListIndexForValue(raw);
		if(index == -1) {
			//-- not found in list / null -> just render 'nothing'
			if(!StringTool.isBlank(m_emptyText))
				add(m_emptyText);
			return;
		}

		//-- Render the value
		T item = list.get(index);
		renderOptionLabel(this, item);
	}

	private void renderEditable() throws Exception {
		setCssClass("ui-cbb2 ui-cbb2-rw");
		add(m_select);

		//-- Append shtuff to the combo
		List<T>	list = getData();
		V raw = internalGetCurrentValue();

		//-- First loop over all values to find out if current value is part of value domain.
		boolean isvalidselection = false;
		int ix = 0;
		ClassMetaModel cmm = null;
		for(T val : list) {
			SelectOption o = new SelectOption();
			m_select.add(o);
			renderOptionLabel(o, val);
			if(null != raw) {
				V res = listToValue(val);
				if(cmm == null)
					//if we are caching cmm, then at least it should always be for one of compared values,
					//otherwise we can get situation that we are sending cmm of type that does not have any relation to any of compared values
					cmm = MetaManager.findClassMeta(raw.getClass());
				boolean eq = MetaManager.areObjectsEqual(res, raw, cmm);
				if(eq) {
					o.setSelected(eq);
					m_select.internalSetSelectedIndex(ix);
					isvalidselection = true;
				}
			}
			ix++;
		}

		//-- Decide if an "unselected" option needs to be present, and add it at index 0 if so.
		setEmptyOption(null);
		if(!isMandatory() || !isvalidselection) {
			//-- We need the "unselected" option.
			SelectOption o = new SelectOption();
			if(getEmptyText() != null)
				o.setText(getEmptyText());
			m_select.add(0, o); // Insert as the very 1st item
			setEmptyOption(o); // Save this to mark it in-use.
			if(!isvalidselection) {
				o.setSelected(true);
				m_select.internalSetSelectedIndex(0);
			} else
				m_select.internalSetSelectedIndex(m_select.getSelectedIndex() + 1); // Increment selected index thingy
		}
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	value setting logic.								*/
	/*--------------------------------------------------------------*/
	/**
	 * @see to.etc.domui.dom.html.IControl#getValue()
	 */
	@Override
	final public V getValue() {
		try {
			validate();
			clearMessage();
			return m_currentValue;
		} catch(ValidationException vx) {
			setMessage(UIMessage.error(vx));
			throw vx;
		}
	}

	final public V getBindValue() {
		validate();
		return m_currentValue;
	}

	final public void setBindValue(V value) {
		setValue(value);
	}

	private void validate() {
		if(isMandatory() && m_currentValue == null) {
			throw new ValidationException(Msgs.MANDATORY);
		}
	}

	/**
	 * Set the combo to the specified value. The value <i>must</i> be in the
	 * domain specified by the data list and must be findable in that list; if
	 * not <b>the results are undefined</b>.
	 * If the value set is null and the combobox is a mandatory one the code will
	 * check if an "unselected" item is present to select. If not the unselected
	 * item will be added by this call(!).
	 * @see to.etc.domui.dom.html.IControl#setValue(java.lang.Object)
	 */
	@Override
	final public void setValue(@Nullable V v) {
		ClassMetaModel cmm = v != null ? MetaManager.findClassMeta(v.getClass()) : null;
		V currentValue = m_currentValue;
		if(MetaManager.areObjectsEqual(v, currentValue, cmm))
			return;
		m_currentValue = v;
		fireModified("value", currentValue, v);
		if(!isBuilt())
			return;

		if(m_readOnly) {
			forceRebuild();
			return;
		}


		//-- If the value is NULL we MUST have an unselected option: add it if needed and select that one.
		int ix = findListIndexForValue(v);
		if(null == v || ix < 0) { // Also create "unselected" if the value is not part of the domain.
			if(getEmptyOption() == null) {
				//-- No empty option yet!! Create one;
				SelectOption o = new SelectOption();
				if(getEmptyText() != null)
					o.setText(getEmptyText());
				add(0, o); // Insert as the very 1st item
				setEmptyOption(o); // Save this to mark it in-use.
			}
			m_select.setSelectedIndex(0);
			return;
		}

		//-- Value is not null. Find the index of the option in the dataset
		if(getEmptyOption() != null)
			ix++;
		m_select.setSelectedIndex(ix);
	}

	/**
	 * The user selected a different option.
	 * @see to.etc.domui.dom.html.Select#internalOnUserInput(int, int)
	 */
	final protected boolean internalOnUserInput(int oldindex, int nindex) {
		V newval;

		if(nindex < 0) {
			newval = null; // Should never happen
		} else if(getEmptyOption() != null) {
			//-- We have an "unselected" choice @ index 0. Is that one selected?
			if(nindex <= 0) // Empty value chosen?
				newval = null;
			else {
				nindex--;
				newval = findListValueByIndex(nindex);
			}
		} else {
			newval = findListValueByIndex(nindex);
		}

		ClassMetaModel cmm = newval == null ? null : MetaManager.findClassMeta(newval.getClass());
		V currentValue = m_currentValue;
		if(MetaManager.areObjectsEqual(newval, currentValue, cmm))
			return false;

		m_currentValue = newval;
		fireModified("value", currentValue, newval);
		return true;
	}

	/**
	 * Return the index in the data list for the specified value, or -1 if not found or if the value is null.
	 * @param newvalue
	 * @return
	 */
	private int findListIndexForValue(V newvalue) {
		if(null == newvalue)
			return -1;
		try {
			ClassMetaModel cmm = MetaManager.findClassMeta(newvalue.getClass());
			List<T> data = getData();
			for(int ix = 0; ix < data.size(); ix++) {
				V	value = listToValue(data.get(ix));
				if(MetaManager.areObjectsEqual(value, newvalue, cmm))
					return ix;
			}
			return -1;
		} catch(Exception x) { // Need to wrap; checked exceptions are idiotic
			throw WrappedException.wrap(x);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see to.etc.domui.component.input.SelectBasedControl#findListValueByIndex(int)
	 */
	private V findListValueByIndex(int ix) {
		try {
			List<T> data = getData();
			if(ix < 0 || ix >= data.size())
				return null;
			return listToValue(data.get(ix));
		} catch(Exception x) { // Need to wrap; checked exceptions are idiotic
			throw WrappedException.wrap(x);
		}
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	List - value conversions and list management		*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param in
	 * @return
	 * @throws Exception
	 */
	protected V listToValue(T in) throws Exception {
		if(m_valueTransformer == null)
			return (V) in;
		return m_valueTransformer.getValue(in);
	}

	private INodeContentRenderer<T> calculateContentRenderer(Object val) {
		if(m_contentRenderer != null)
			return m_contentRenderer;
		if(m_contentRendererClass != null)
			return DomApplication.get().createInstance(m_contentRendererClass);

		if(val == null)
			throw new IllegalStateException("Cannot calculate content renderer for null value");
		ClassMetaModel cmm = MetaManager.findClassMeta(val.getClass());
		return (INodeContentRenderer<T>) MetaManager.createDefaultComboRenderer(m_propertyMetaModel, cmm);
	}

	final protected void renderOptionLabel(NodeContainer o, T object) throws Exception {
		if(m_actualContentRenderer == null)
			m_actualContentRenderer = calculateContentRenderer(object);
		m_actualContentRenderer.renderNodeContent(this, o, object, this);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	All the myriad ways of providing data.				*/
	/*--------------------------------------------------------------*/
	/**
	 * Can be used to set a specific list-of-values. When called this clears the existing dataset.
	 * @param data
	 */
	public void setData(List<T> data) {
		if(m_data != data) {
			forceRebuild();
			m_select.forceRebuild();
			m_actualContentRenderer = null;
		}
		m_data = data;
	}

	/**
	 * Returns the data to use as the list-of-values of this combo. This must contain actual selectable
	 * values only, it may not contain a "no selection made" value thingerydoo.
	 * @return
	 * @throws Exception
	 */
	public List<T> getData() throws Exception {
		if(m_data == null)
			m_data = provideData();
		return m_data;
	}

	/**
	 * Creates the list-of-values that is to be used if no specific lov is set using setData(). The
	 * default implementation walks the data providers to see if one is present.
	 * @return
	 * @throws Exception
	 */
	protected List<T> provideData() throws Exception {
		if(m_listMaker != null)
			return DomApplication.get().getCachedList(m_listMaker);

		//-- Try datasets,
		IComboDataSet<T> builder = m_dataSet;
		if(builder == null && m_dataSetClass != null)
			builder = DomApplication.get().createInstance(m_dataSetClass);
		if(builder != null)
			return builder.getComboDataSet(getPage().getBody());
		return Collections.EMPTY_LIST;
		//
		//		throw new IllegalStateException("I have no way to get data to show in my combo..");
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Code to add extra stuff after this combo.			*/
	/*--------------------------------------------------------------*/
	/**
	 * Add a small image button after the combo.
	 * @param img
	 * @param title
	 * @param click
	 */
	public void addExtraButton(String img, String title, final IClicked<NodeBase> click) {
		if(m_buttonList == Collections.EMPTY_LIST)
			m_buttonList = new ArrayList<SmallImgButton>();
		SmallImgButton si = new SmallImgButton(img);
		if(click != null) {
			si.setClicked(new IClicked<SmallImgButton>() {
				@Override
				public void clicked(@Nonnull SmallImgButton b) throws Exception {
					click.clicked(ComboComponentBase2.this);
				}
			});
		}
		if(title != null)
			si.setTitle(title);
		si.addCssClass("ui-cl2-btn");
		m_buttonList.add(si);

		if(isBuilt())
			forceRebuild();
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	IControl<T> implementation.						*/
	/*--------------------------------------------------------------*/
	/**
	 * @see to.etc.domui.dom.html.IControl#getValueSafe()
	 */
	@Override
	public V getValueSafe() {
		return DomUtil.getValueSafe(this);
	}

	/**
	 * @see to.etc.domui.dom.html.IControl#hasError()
	 */
	@Override
	public boolean hasError() {
		getValueSafe();
		return super.hasError();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IBindable interface.								*/
	/*--------------------------------------------------------------*/

	@Nullable
	private List<SimpleBinder> m_bindingList;

	@Override
	public @Nonnull IBinder bind() {
		return bind("bindValue");
	}

	@Override
	@Nonnull
	public IBinder bind(@Nonnull String componentProperty) {
		List<SimpleBinder> list = m_bindingList;
		if(list == null)
			list = m_bindingList = new ArrayList<SimpleBinder>(1);
		SimpleBinder binder = new SimpleBinder(this, componentProperty);
		list.add(binder);
		return binder;
	}

	@Override
	@Nullable
	public List<SimpleBinder> getBindingList() {
		return m_bindingList;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Getters, setters and other boring crud.				*/
	/*--------------------------------------------------------------*/
	public INodeContentRenderer<T> getContentRenderer() {
		return m_contentRenderer;
	}

	public void setContentRenderer(INodeContentRenderer<T> contentRenderer) {
		m_contentRenderer = contentRenderer;
	}

	public Class< ? extends INodeContentRenderer<T>> getContentRendererClass() {
		return m_contentRendererClass;
	}

	public void setContentRendererClass(Class< ? extends INodeContentRenderer<T>> contentRendererClass) {
		m_contentRendererClass = contentRendererClass;
	}

	public PropertyMetaModel< ? > getPropertyMetaModel() {
		return m_propertyMetaModel;
	}

	public void setPropertyMetaModel(PropertyMetaModel< ? > propertyMetaModel) {
		m_propertyMetaModel = propertyMetaModel;
	}

	public IListMaker<T> getListMaker() {
		return m_listMaker;
	}

	public void setListMaker(IListMaker<T> listMaker) {
		m_listMaker = listMaker;
	}

	public IValueTransformer<V> getValueTransformer() {
		return m_valueTransformer;
	}

	public void setValueTransformer(IValueTransformer<V> valueTransformer) {
		m_valueTransformer = valueTransformer;
	}

	public String getEmptyText() {
		return m_emptyText;
	}

	public void setEmptyText(String emptyText) {
		m_emptyText = emptyText;
	}

	/**
	 * If this combobox has a "unselected" option currently this contains that option. When present it
	 * means that indexes in the <i>combo</i> list are one <i>higher</i> than indexes in the backing
	 * dataset (because this empty option is always choice# 0).
	 * @return
	 */
	protected SelectOption getEmptyOption() {
		return m_emptyOption;
	}

	/**
	 * See getter.
	 * @param emptyOption
	 */
	protected void setEmptyOption(SelectOption emptyOption) {
		m_emptyOption = emptyOption;
	}

	protected V internalGetCurrentValue() {
		return m_currentValue;
	}

	@Override
	public void setMandatory(boolean mandatory) {
		if(isMandatory() == mandatory)
			return;
		m_select.setMandatory(mandatory); // Switch flag
		forceRebuild(); // The "empty option" might have changed
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Hard data binding support.							*/
	/*--------------------------------------------------------------*/

	@Nullable
	private IValueChanged<?> m_onValueChanged;

	@Override
	public IValueChanged< ? > getOnValueChanged() {
		return m_onValueChanged;
	}

	@Override
	public void setOnValueChanged(IValueChanged< ? > onValueChanged) {
		if(m_onValueChanged == onValueChanged)
			return;
		m_onValueChanged = onValueChanged;
		if(null == onValueChanged) {
			m_select.setOnValueChanged(null);
		} else {
			m_select.setOnValueChanged(new IValueChanged<Select>() {
				@Override public void onValueChanged(@Nonnull Select component) throws Exception {
					IValueChanged<ComboComponentBase2<T, V>> vc = (IValueChanged<ComboComponentBase2<T, V>>) m_onValueChanged;
					if(null != vc)
						vc.onValueChanged(ComboComponentBase2.this);
				}
			});
		}
	}

	@Override
	public void setDisabled(boolean d) {
		m_select.setDisabled(d);
	}

	@Override
	public boolean isModified() {
		return m_select.isModified();
	}

	@Override
	public void setModified(boolean as) {
		m_select.setModified(as);
	}

	@Override
	public boolean isReadOnly() {
		return m_readOnly;
	}

	@Override
	public void setReadOnly(boolean ro) {
		if(m_readOnly == ro)
			return;
		m_readOnly = ro;
		forceRebuild();
	}

	@Override
	public boolean isDisabled() {
		return m_select.isDisabled();
	}

	@Override
	public boolean isMandatory() {
		return m_select.isMandatory();
	}

	@Override
	@Nonnull
	public Set<String> getBindableProperties() {
		return BINDABLE_SET;
	}
}
