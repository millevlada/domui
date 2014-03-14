package to.etc.domui.component.tbl;

import javax.annotation.*;

import to.etc.domui.util.*;

abstract public class SelectableTabularComponent<T> extends TabularComponentBase<T> implements ISelectionListener<T>, ISelectableTableComponent<T> {
	abstract protected void createSelectionUI() throws Exception;


	public SelectableTabularComponent(@Nonnull ITableModel<T> model) {
		super(model);
	}

	public SelectableTabularComponent() {}

	/*--------------------------------------------------------------*/
	/*	CODING:	Handling selections.								*/
	/*--------------------------------------------------------------*/
	/** If this table allows selection of rows, this model maintains the selections. */
	@Nullable
	private ISelectionModel<T> m_selectionModel;

	@Nullable
	private ISelectionAllHandler m_selectionAllHandler;

	private boolean m_showSelectionAlways = true;

	private boolean m_multiSelectMode;

	@Override
	@Nullable
	public ISelectionAllHandler getSelectionAllHandler() {
		return m_selectionAllHandler;
	}

	public void setSelectionAllHandler(@Nullable ISelectionAllHandler selectionAllHandler) {
		if(m_selectionAllHandler == selectionAllHandler)
			return;
		m_selectionAllHandler = selectionAllHandler;
		fireSelectionUIChanged();
	}

	/**
	 * Return the model used for table selections, if applicable.
	 * @return
	 */
	@Override
	@Nullable
	public ISelectionModel<T> getSelectionModel() {
		return m_selectionModel;
	}

	/**
	 * Set the model to maintain selections, if this table allows selections.
	 *
	 * @param selectionModel
	 */
	public void setSelectionModel(@Nullable ISelectionModel<T> selectionModel) {
		if(DomUtil.isEqual(m_selectionModel, selectionModel))
			return;
		if(m_selectionModel != null) {
			m_selectionModel.removeListener(this);
		}
		m_selectionModel = selectionModel;
		if(null != selectionModel) {
			setDisableClipboardSelection(true);
			selectionModel.addListener(this);
		}
//		m_lastSelectionLocation = -1;
		forceRebuild();
	}

	/**
	 * When T and a selection model in multiselect mode is present, this causes the
	 * checkboxes to be rendered initially even when no selection is made.
	 * @return
	 */
	public boolean isShowSelectionAlways() {
		return m_showSelectionAlways;
	}

	/**
	 * When T and a selection model in multiselect mode is present, this causes the
	 * checkboxes to be rendered initially even when no selection is made.
	 * @param showSelectionAlways
	 * @throws Exception
	 */
	@Override
	public void setShowSelection(boolean showSelectionAlways) throws Exception {
		if(m_showSelectionAlways == showSelectionAlways || getModel() == null || getModel().getRows() == 0)
			return;
		m_showSelectionAlways = showSelectionAlways;
		ISelectionModel<T> sm = getSelectionModel();
		if(sm == null)
			throw new IllegalStateException("Selection model is empty?");
		if(!isBuilt() || m_multiSelectMode || getSelectionModel() == null || !sm.isMultiSelect())
			return;

		createSelectionUI();
	}


}
