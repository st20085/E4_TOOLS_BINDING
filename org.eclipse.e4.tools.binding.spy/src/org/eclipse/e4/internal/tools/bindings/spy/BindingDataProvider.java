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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.e4.core.commands.internal.HandlerServiceImpl;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The column Label and content Provider used to display information in context
 * data TreeViewer. Two instances for label provider are created : one for key,
 * one for values
 *
 * @see BindingDataPart
 */
public class BindingDataProvider extends ColumnLabelProvider implements ITreeContentProvider, IStyledLabelProvider {

	private static final Color COLOR_IF_FOUND = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
	private static final Color COLOR_IF_EXCEPTION = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
	
	private static final Object[] EMPTY_RESULT = new Object[0];
	
	static final String ACTIVE_BINDINGS = "Active bindings";
	static final String CONFLICT_BINDINGS = "Conflict bindings";
	
	public static enum Column { TRIGGER_SEQUENCE, COMMAND_DESCRIPTION, STATE, CATEGORY, COMMAND_HANDLER, BINDING_CONTEXT_ID, TYPE, OTHERS};
	

	private static final String EXCEPTION = "Exception : ";

	// Image keys constants
//	private static final String INJECT_IMG_KEY = "icons/annotation_obj.png";

	private ImageRegistry imgReg;
	private ColorRegistry colorRegistry;
	
	//
	private Styler cmd_name_styler;
	private Styler cmd_desc_styler;
	private Styler exception_styler;
	private Styler handler_styler;
	private Styler enabled_styler;
	private Styler disabled_styler;
	
	@Inject
	private BindingDataFilter contextFilter;

	/** Store the selected context (initialized in inputChanged) */
	@SuppressWarnings("restriction")
	private static IEclipseContext selectedContext;

	private Font boldFont;

	private Column column = Column.TRIGGER_SEQUENCE;

	@Inject
	public BindingDataProvider() {
		super();
		initFonts();
		initializeImageRegistry();
		initializeColorRegistry();
	}

	@Override
	public void dispose() {
		selectedContext = null;
		imgReg = null;
	}

	@Override
	@SuppressWarnings("restriction")
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		selectedContext = (newInput instanceof IEclipseContext) ? (IEclipseContext) newInput : null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IEclipseContext) {
			IEclipseContext context = (IEclipseContext) inputElement;
			EBindingService bindingService = context.get(EBindingService.class);
			if (bindingService != null) {
				List<String> elements = new ArrayList<>(2);
				if (! bindingService.getActiveBindings().isEmpty())
					elements.add(ACTIVE_BINDINGS);
				if (! bindingService.getAllConflicts().isEmpty())
					elements.add(CONFLICT_BINDINGS);
				
				return elements.toArray();
			}
		}
		
		if (inputElement instanceof Binding) {
			return EMPTY_RESULT;
		}
		
//		System.err.println("inputElement0 "+inputElement);
		return EMPTY_RESULT;
	}
	
	@SuppressWarnings("restriction")
	@Override
	public boolean hasChildren(Object element) {
		if ((element == ACTIVE_BINDINGS) || (element == CONFLICT_BINDINGS)) {
			return true; // Intermediate nodes returns true
		}

		if (element instanceof Binding) {
			return false;
		}
		
//		System.err.println("hasChildren "+element);
		return false;
	}


	@Override
	@SuppressWarnings("restriction")
	public Object[] getChildren(Object inputElement) {
		if (selectedContext == null)
			return EMPTY_RESULT;
		
		if (inputElement == ACTIVE_BINDINGS) {
			EBindingService bindingService = selectedContext.get(EBindingService.class);
			return bindingService.getActiveBindings().toArray();
		}
		if (inputElement == CONFLICT_BINDINGS) {
			EBindingService bindingService = selectedContext.get(EBindingService.class);
			return bindingService.getAllConflicts().toArray();
		}

		if (inputElement instanceof Binding) {
			Binding binding = (Binding) inputElement;
			return new Object[] {binding};
		}
		
//		System.err.println("inputElement1 "+inputElement);
		
		return EMPTY_RESULT;
	}

	public void setColumn(Column column) {
		this.column = column;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public Color getForeground(Object element) {
		// Return red color if exception
		String s = getText(element);
		if ((s != null) && s.startsWith(EXCEPTION))
			return COLOR_IF_EXCEPTION;
		
		// Return blue color if the string matches the search
		return (contextFilter.matchText(s)) ? COLOR_IF_FOUND : null;
	}

	/** Get the bold font for keys that are computed with ContextFunction */
	@Override
	public Font getFont(Object element) {
		return (element == ACTIVE_BINDINGS || element == CONFLICT_BINDINGS)? boldFont : null;
	}

	@SuppressWarnings("restriction")
	@Override
	public Image getImage(Object element) {
		if (column != Column.TRIGGER_SEQUENCE) // No image in value column, only in first column
			return null;

//		return imgReg.get(INJECT_IMG_KEY);

		return null;
	}

	@SuppressWarnings("restriction")
	@Override
	public String getToolTipText(Object element) {
		return super.getToolTipText(element);
	}

	@Override
	public Image getToolTipImage(Object object) {
		return getImage(object);
	}

	@Override
	public int getToolTipStyle(Object object) {
		return SWT.SHADOW_OUT;
	}

	@Override
	public Object getParent(Object element) {
		if (element == ACTIVE_BINDINGS || element == CONFLICT_BINDINGS)
			return null;

		// Not computed
		return null;
	}

	private void initializeImageRegistry() {
		Bundle b = FrameworkUtil.getBundle(this.getClass());
		imgReg = new ImageRegistry();

//		imgReg.put(CONTEXT_FUNCTION_IMG_KEY, ImageDescriptor.createFromURL(b.getEntry(CONTEXT_FUNCTION_IMG_KEY)));
	}
	
	/**
	 * 
	 */
	private void initializeColorRegistry() {
		colorRegistry = JFaceResources.getColorRegistry();
		colorRegistry.put("COUNTER_COLOR", new RGB(0, 127, 174));
		colorRegistry.put("DECORATIONS_COLOR", new RGB(149, 125, 71));
		colorRegistry.put("EXCEPTION_COLOR", new RGB(255, 0, 0));
		colorRegistry.put("ENABLED_COLOR", new RGB(0, 192, 0));
		colorRegistry.put("DISABLED_COLOR", new RGB(128, 128, 128));
		
		cmd_name_styler = StyledString.createColorRegistryStyler("COUNTER_COLOR", null);
		cmd_desc_styler = StyledString.createColorRegistryStyler("DECORATIONS_COLOR", null);
		exception_styler = StyledString.createColorRegistryStyler("EXCEPTION_COLOR", null);
		disabled_styler = StyledString.createColorRegistryStyler("DISABLED_COLOR", null);
		enabled_styler = StyledString.createColorRegistryStyler("ENABLED_COLOR", null);
		handler_styler = StyledString.createColorRegistryStyler("DISABLED_COLOR", null);
	}


	private void initFonts() {
		FontData[] fontData = Display.getCurrent().getSystemFont().getFontData();
		String fontName = fontData[0].getName();
		FontRegistry registry = JFaceResources.getFontRegistry();
		boldFont = registry.getBold(fontName);
	}

	/*
	 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
	 */
	@Override
	public StyledString getStyledText(Object element) {
		StyledString styledString = new StyledString();
		if (selectedContext == null)
			return styledString;
		
		if (element instanceof Binding) {
			Binding binding = (Binding) element;
			ParameterizedCommand parameterizedCommand = binding.getParameterizedCommand();
			Command cmd = parameterizedCommand.getCommand();

			switch (column) {
				case TRIGGER_SEQUENCE:
					styledString.append(String.valueOf(binding.getTriggerSequence()));
					break;
				case COMMAND_DESCRIPTION:
					try {
						styledString.append(cmd.getId());
						styledString.append(" (" + cmd.getName() + ") ", cmd_name_styler);
						if (cmd.getDescription() != null)
							styledString.append(" : " + cmd.getDescription(), cmd_desc_styler);
					} catch (NotDefinedException e) {
						styledString.append(EXCEPTION + e.getMessage(), exception_styler);
					}
					break;
				case COMMAND_HANDLER:
					IHandler handler = cmd.getHandler();
					Object contextHandler = selectedContext.get(HandlerServiceImpl.H_ID + cmd.getId());
					if (contextHandler != null) {
						styledString.append(String.valueOf(contextHandler));
						styledString.append(" : ");
					}
					styledString.append(String.valueOf(handler), handler_styler);
					break;
				case STATE:
					if (cmd.isEnabled())
						styledString.append("enabled", enabled_styler);
					else
						styledString.append("disabled", disabled_styler);
					break;
				case CATEGORY:
					try {
						styledString.append(cmd.getCategory().getName());
					} catch (NotDefinedException e) {
						styledString.append(EXCEPTION + e.getMessage(), exception_styler);
					}
					break;
				case BINDING_CONTEXT_ID:
					styledString.append(binding.getContextId());
					break;
				case TYPE:
					styledString.append(String.valueOf(binding.getType()));
					break;
				default:
					styledString.append(String.valueOf(binding));
					break;
			}
		} else {
			if (column == Column.TRIGGER_SEQUENCE)
				styledString.append(String.valueOf(element));
		}
		
		// do not use style for matching text
		String text = styledString.toString();
		if (contextFilter.matchText(text))
			return new StyledString(text);
		
		return styledString;
	}

}
