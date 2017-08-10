package com.itextpdf.pdfdebug.plugin.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.rups.model.LoggerHelper;

/**
 * This class contains some static utility methods that act as wrappers around the iText core functionality
 */
public class PdfDocumentUtilities {

    public static final String CLASS_TYPE = "com.itextpdf.kernel.pdf.PdfDocument";
    public static final String METHOD_SIGNATURE = "()[B";
    public static final String METHOD_NAME = "getSerializedBytes";

    private static final String NOT_READY_FOR_PLUGIN_MESSAGE = "Cannot get PdfDocument. "
            + "\nMake sure you create reader from stream or string and writer is set to DebugMode.";

    private static final String DOCUMENT_IS_CLOASED_MESSAGE = "The document was closed.";

    private static final String DEBUG_BYTES_METHOD_NAME = "getDebugBytes";
    private static Method getDebugBytesMethod;

    static {
        try {
            getDebugBytesMethod = PdfWriter.class.getDeclaredMethod(DEBUG_BYTES_METHOD_NAME);
            getDebugBytesMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
    }

    /**
     * Return whether a given IJavaVariable represents a PdfDocument object
     *
     * @param var the input variable
     * @return true if the input variable is a PdfDocument object, false otherwise
     */
    public static boolean isPdfDocument(IJavaVariable var) {
        try {
            IJavaObject obj = DebugUtilities.getIJavaObject(var);
            if (obj != null && obj.getJavaType().getName().equals(CLASS_TYPE)) {
                return true;
            }
        } catch (DebugException ignored) {
        }
        return false;
    }

    /**
     * Get the bytes representing the PdfDocument in a given IJavaVariable
     *
     * @param var the input variable
     * @return the bytes representing the PdfDocument
     */
    public static byte[] getDocumentDebugBytes(IJavaVariable var) {
        PdfDocument doc = null;
        doc = getPdfDocument(var);
        if (doc == null) {
        	LoggerHelper.error(NOT_READY_FOR_PLUGIN_MESSAGE, PdfDocumentUtilities.class);
        	return null;
        }
        if (doc.isClosed()) {
            LoggerHelper.warn(DOCUMENT_IS_CLOASED_MESSAGE, PdfDocumentUtilities.class);
            return null;
        }
        PdfWriter writer = doc.getWriter();
        writer.setCloseStream(true);
        doc.setCloseWriter(false);
        doc.close();
        byte[] documentCopyBytes = null;
        try {
            documentCopyBytes = (byte[]) getDebugBytesMethod.invoke(writer);
        } catch (IllegalAccessException ignored) {
        } catch (InvocationTargetException ignored) {
        }
        try {
            writer.close();
        } catch (IOException e) {
            LoggerHelper.error("Writer cloasing error", e, PdfDocumentUtilities.class);
        }
        return documentCopyBytes;
    }

    /**
     * Get the PdfDocument object represented by a given IJavaVariable
     *
     * @param var the input variable
     * @return the PdfDocument object represented by the input variable
     */
    private static PdfDocument getPdfDocument(IJavaVariable var) {
        byte[] bytes = null;
        IJavaValue byteArr = null;
        IJavaObject obj = DebugUtilities.getIJavaObject(var);
        try {
            if (obj != null && obj.getJavaType().getName().equals(CLASS_TYPE)) {
                IThread owningThread = obj.getOwningThread();
                if (owningThread instanceof IJavaThread) {
                    byteArr = obj.sendMessage(METHOD_NAME, METHOD_SIGNATURE, null, (IJavaThread) owningThread, false);
                } else {
                    for (IThread th : obj.getDebugTarget().getThreads()) {
                        if (th.isSuspended()) {
                            IJavaVariable newVar = ((IJavaThread) th).findVariable(var.getName());
                            if (var.equals(newVar)) {
                                byteArr = obj.sendMessage(METHOD_NAME, METHOD_SIGNATURE, null, (IJavaThread) th, false);
                            }
                        }
                    }
                }
            }
        } catch (DebugException ignored) {
        }
        bytes = getByteArray(byteArr);
        return createDocumentFromBytes(bytes);
    }

    /**
     * Get the bytes representing the PdfDocument in a given IJavaValue
     *
     * @param byteArr the input value
     * @return the bytes representing the PdfDocument
     */
    private static byte[] getByteArray(IJavaValue byteArr) {
        byte[] res = null;
        try {
            if (byteArr instanceof IJavaArray) {
                IJavaValue[] arr = ((IJavaArray) byteArr).getValues();
                res = new byte[arr.length];
                if (arr.length != 0 && arr[0] instanceof IJavaPrimitiveValue) {
                    for (int i = 0; i < arr.length; ++i) {
                        res[i] = ((IJavaPrimitiveValue) arr[i]).getByteValue();
                    }
                }
            }
        } catch (DebugException ignored) {
        }
        return res;
    }

    /**
     * Creates a PdfDocument from a byte[]
     *
     * @param bytes input byte[]
     * @return a PdfDocument object
     */
    private static PdfDocument createDocumentFromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        PdfDocument doc = null;
        try {
            doc = (PdfDocument) new ObjectInputStream(bais).readObject();
        } catch (ClassNotFoundException ignored) {
        } catch (IOException ignored) {
        } finally {
            try {
                bais.close();
            } catch (IOException ignored) {
            }
        }
        return doc;
    }

}
