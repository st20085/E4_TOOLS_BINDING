/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     OPCoach - initial API and implementation for bug #437478
 *******************************************************************************/
package org.eclipse.e4.internal.tools.bindings.spy;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

@Creatable
@Singleton
public class BindingDataFilter extends ViewerFilter {

	@Inject
	Logger log;

	private String pattern;

	// Implements the filter for the data table content
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if ((element == BindingDataProvider.ACTIVE_BINDINGS)
				|| (element == BindingDataProvider.CONFLICT_BINDINGS))
			return true;

		TreeViewer tv = (TreeViewer) viewer;

		// Must only select objects matching the pattern
		for (int col = 0; col < tv.getTree().getColumnCount(); col++) {
			BindingDataProvider bindingDataProvider = getBindingDataProvider(tv.getLabelProvider(col));
			
			// If the text matches in one of the column, must keep it...
			String text = bindingDataProvider.getText(element);
			if (matchText(text))
				return true;
		}
		return false;
	}

	/**
	 * @param labelProvider
	 */
	private BindingDataProvider getBindingDataProvider(Object labelProvider) {
		if (labelProvider instanceof DelegatingStyledCellLabelProvider) {
			labelProvider = ((DelegatingStyledCellLabelProvider) labelProvider).getStyledStringProvider();
		}
		return (BindingDataProvider) labelProvider;
	}

	/** Set the pattern and use it as lowercase */
	public void setPattern(String newPattern) {
		if ((newPattern == null) || (newPattern.length() == 0))
			pattern = null;
		else
			pattern = newPattern.toLowerCase();
	}

	/**
	 * This method search for an object and check if it contains the text or a
	 * pattern matching this text
	 */
	boolean containsText(IEclipseContext ctx, TreeViewer bindingDataViewer) {
		
		EBindingService bindingService = ctx.get(EBindingService.class);
		if (bindingService != null) {
			for(Binding binding : bindingService.getActiveBindings()) {
				if (select(bindingDataViewer, null, binding))
					return true;
			}
			for(Binding binding : bindingService.getAllConflicts()) {
				if (select(bindingDataViewer, null, binding))
					return true;
			}
		}
		
		return false;
	}

	public boolean matchText(String text) {
		return ((text == null) || (pattern == null)) ? false : text.toLowerCase().contains(pattern);
	}
}
