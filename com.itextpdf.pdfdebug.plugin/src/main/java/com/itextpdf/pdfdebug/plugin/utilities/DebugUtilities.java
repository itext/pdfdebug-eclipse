package com.itextpdf.pdfdebug.plugin.utilities;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * This class contains some static utility methods that act as wrappers around the eclipse debug core
 */
public class DebugUtilities {

    /**
     * Get the name of the variable based on a selection
     *
     * @param selection the selected text
     * @return the name of the variable
     */
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

    /**
     * Get the java object based on a selection
     *
     * @param selection the selected text
     * @return a representation of the selected object
     */
    public static IJavaObject getIJavaObject(IStructuredSelection selection) {
        IJavaVariable var = getIJavaVariable(selection);
        return getIJavaObject(var);
    }

    /**
     * Get the java object based on a given variable
     *
     * @param variable the input variable
     * @return a representation of the selected object
     */
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

    /**
     * Get the java variable based on a given selection
     *
     * @param selection the selected text
     * @return a representation of the selected variable
     */
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
