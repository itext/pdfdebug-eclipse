package com.itextpdf.samwell.plugin.utilities;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;

public class DebugUtilities {
	
	public static String getVariableName(IStructuredSelection selection) {
        IJavaVariable var = getIJavaVariable(selection);
        if (var != null) {
        	try {
				return var.getName();
			} catch (DebugException ignored) {
			}
        }
        return null;
    }
	
	public static IJavaObject getIJavaObject(IStructuredSelection selection) {
        IJavaVariable var = getIJavaVariable(selection);
        return getIJavaObject(var);
    }
	
	public static IJavaObject getIJavaObject(IJavaVariable variable) {
		if (variable != null) {
			IValue value;
			try {
				value = variable.getValue();
				if (value instanceof IJavaObject) {
					return (IJavaObject) value;
				}
			} catch (DebugException ignored) {
			}
		}
		return null;
	}
	
	public static IJavaVariable getIJavaVariable(IStructuredSelection selection) {
		if (selection != null && selection.size() != 0) {
        	Object obj = selection.getFirstElement();
        	if (obj instanceof IJavaVariable) {
        		return (IJavaVariable) obj;
        	}
        }
        return null;
	}
}
