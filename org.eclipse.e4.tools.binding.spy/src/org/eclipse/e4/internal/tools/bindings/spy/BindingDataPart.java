/*******************************************************************************
 * Copyright (c) 2013 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.internal.tools.bindings.spy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * This part listen to selection, and if it is an EclipseContext, it displays
 * its information It is used in the integrated BindingSpyPart and (in the
 * future) it could be used outside to display the context of focused part for
 * instance
 */
public class BindingDataPart {
	public TreeViewer bindingDataViewer;

	private BindingDataProvider dataProvider;

	private BindingEntryComparator comparator;

	/**
	 * Create contents of the view part.
	 */
	@PostConstruct
	public void createControls(Composite parent, IEclipseContext ctx) {

		parent.setLayout(new GridLayout(1, false));

		// TreeViewer on the top
		bindingDataViewer = new TreeViewer(parent);
		dataProvider = ContextInjectionFactory.make(BindingDataProvider.class, ctx);
		bindingDataViewer.setContentProvider(dataProvider);
		bindingDataViewer.setLabelProvider(dataProvider);
		// contextContentTv.setSorter(new ViewerSorter());

		final Tree cTree = bindingDataViewer.getTree();
		cTree.setHeaderVisible(true);
		cTree.setLinesVisible(true);
		cTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// tv.setInput(a);
		bindingDataViewer.setInput("Foo"); // getElements starts alone

		// Add columns in the tree
		createColumn(ctx, "Key", 100, BindingDataProvider.Column.TRIGGER_SEQUENCE);
		createColumn(ctx, "Command id/name/description", 250, BindingDataProvider.Column.COMMAND_DESCRIPTION);
		createColumn(ctx, "Command handler", 250, BindingDataProvider.Column.COMMAND_HANDLER);
		createColumn(ctx, "State", 60, BindingDataProvider.Column.STATE);
		createColumn(ctx, "Category", 60, BindingDataProvider.Column.CATEGORY);
		createColumn(ctx, "Binding context id", 250, BindingDataProvider.Column.BINDING_CONTEXT_ID);

		// Open all the tree
		bindingDataViewer.expandAll();

		ColumnViewerToolTipSupport.enableFor(bindingDataViewer);
	}

	/**
	 * Create column
	 * @param ctx
	 */
	private void createColumn(IEclipseContext ctx, String text, int width, BindingDataProvider.Column column) {
		TreeViewerColumn treeCol = new TreeViewerColumn(bindingDataViewer, SWT.NONE);
		treeCol.getColumn().setWidth(width);
		treeCol.getColumn().setText(text);
		BindingDataProvider labelProvider = ContextInjectionFactory.make(BindingDataProvider.class, ctx);
		labelProvider.setColumn(column);
	
		if (bindingDataViewer.getTree().getColumnCount() == 1) {
			comparator = new BindingEntryComparator(0, labelProvider);
			bindingDataViewer.setComparator(comparator);
			treeCol.setLabelProvider(labelProvider);
		} else {	
			treeCol.setLabelProvider(new DelegatingStyledCellLabelProvider(labelProvider));
		}
		
		treeCol.getColumn().addSelectionListener(
				getHeaderSelectionAdapter(bindingDataViewer, treeCol.getColumn(), bindingDataViewer.getTree().getColumnCount()-1, labelProvider));
	}

	@PreDestroy
	public void dispose() {
	}

	@Focus
	public void setFocus() {
		bindingDataViewer.getControl().setFocus();
	}

	@SuppressWarnings("restriction")
	@Inject
	@Optional
	public void listenToContext(@Named(IServiceConstants.ACTIVE_SELECTION) EclipseContext ctx) {
		// Must check if dataviewer is created or not (when we reopen the window
		// @postconstruct has not been called yet)
		if ((ctx == null) || (bindingDataViewer == null)) {
			return;
		}
		bindingDataViewer.setInput(ctx);
		bindingDataViewer.expandToLevel(2);
		
		packAllColumns();
	}

	/**
	 * 
	 */
	private void packAllColumns() {
		for(TreeColumn treeColumn : bindingDataViewer.getTree().getColumns()) {
			treeColumn.pack();
		}
	}

	/**
	 * An entry comparator for the table, dealing with column index, keys and
	 * values
	 */
	public class BindingEntryComparator extends ViewerComparator {
		private int columnIndex;
		private int direction;
		private ILabelProvider labelProvider;

		public BindingEntryComparator(int columnIndex, ILabelProvider defaultLabelProvider) {
			this.columnIndex = columnIndex;
			direction = SWT.UP;
			labelProvider = defaultLabelProvider;
		}

		public int getDirection() {
			return direction;
		}

		/** Called when click on table header, reverse order */
		public void setColumn(int column) {
			if (column == columnIndex) {
				// Same column as last sort; toggle the direction
				direction = (direction == SWT.UP) ? SWT.DOWN : SWT.UP;
			} else {
				// New column; do a descending sort
				columnIndex = column;
				direction = SWT.DOWN;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			// For root elements at first level, we keep Local before Inherited
			if ((e1 == BindingDataProvider.ACTIVE_BINDINGS) || (e2 == BindingDataProvider.ACTIVE_BINDINGS))
				return -1;
			if (e2 == BindingDataProvider.ACTIVE_BINDINGS)
				return 1;

			// Now can compare the text from label provider.
			String lp1 = labelProvider.getText(e1);
			String lp2 = labelProvider.getText(e2);
			String s1 = lp1 == null ? "" : lp1.toLowerCase();
			String s2 = lp2 == null ? "" : lp2.toLowerCase();
			int rc = s1.compareTo(s2);
			// If descending order, flip the direction
			return (direction == SWT.DOWN) ? -rc : rc;
		}

		public void setLabelProvider(ILabelProvider textProvider) {
			labelProvider = textProvider;
		}

	}

	private SelectionAdapter getHeaderSelectionAdapter(final TreeViewer viewer, final TreeColumn column,
			final int columnIndex, final ILabelProvider textProvider) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setComparator(comparator);
				comparator.setColumn(columnIndex);
				comparator.setLabelProvider(textProvider);
				viewer.getTree().setSortDirection(comparator.getDirection());
				viewer.getTree().setSortColumn(column);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	public void refresh(boolean refreshLabel) {
		bindingDataViewer.refresh(refreshLabel);
	}

	private static final ViewerFilter[] NO_FILTER = new ViewerFilter[0];

	public void setFilter(ViewerFilter filter) {

		bindingDataViewer.setFilters((filter == null) ? NO_FILTER : new ViewerFilter[] { filter });
	}

}
