package com.itextpdf.samwell.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.rups.Rups;
import com.itextpdf.rups.model.SwingHelper;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

public class RupsDetailPane implements IDetailPane {

    private volatile Composite comp;
    private Rups rups;
    private volatile JPanel panel;
    private volatile Frame frame;

    public static final String ID = "SamwellDetailPane";
    public static final String NAME = "PdfDocument Detail Pane";
    public static final String DESCRIPTION = "Detail pane that displays an end structure of PdfDocument object.";
    public static final String CLASS_TYPE = "com.itextpdf.kernel.pdf.PdfDocument";
    public static final String METHOD_SIGNATURE = "()[B";
    public static final String METHOD_NAME = "getSerializedBytes";
    
    private static final String ERROR_MESSAGE = "Cannot get PdfDocument. "
    		+ "\nMake sure you create reader from stream and writer is set to DebugMode."; 

    private static final String DEBUG_BYTES_METHOD_NAME = "getDebugBytes";
    private static Method getDebugBytesMethod;
    
    private volatile PdfDocument prevDoc;

    static {
        try {
            getDebugBytesMethod = PdfWriter.class.getDeclaredMethod(DEBUG_BYTES_METHOD_NAME);
            getDebugBytesMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
    }

    @Override
    public Control createControl(Composite parent) {
    	comp = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
    	GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        comp.setLayoutData(gd);
        frame = SWT_AWT.new_Frame(comp);
        frame = SWT_AWT.getFrame(comp);
        SwingHelper.invokeSync(new Runnable() {
			public void run() {
				panel = new JPanel(new BorderLayout());
		        frame.add(panel, BorderLayout.CENTER);			        
			}
		});
        Dimension dim = new Dimension(parent.getSize().x, parent.getSize().y);
        rups = Rups.startNewPlugin(panel, dim, SWT_AWT.getFrame(comp));
        return comp;
    }

    @Override
    public void dispose() {
    	closeRoutine();
    	SwingHelper.invokeSync(new Runnable() {
			public void run() {
				frame.dispose();
			}
		});
    	comp.dispose();
    }

    @Override
    public void display(IStructuredSelection selection) {
    	ByteArrayInputStream bais = null;
        try {
            if (isPdfDocument(selection)) {
            	comp.setVisible(true);
                PdfDocument doc = getPdfDocument(selection);
                if (doc == null) {
                	SwingUtilities.invokeLater(new Runnable() {
                		public void run() {
                			System.err.println(ERROR_MESSAGE);
                		}
                	});
                	return;
                }
                PdfWriter writer = doc.getWriter();
                writer.setCloseStream(true);
                doc.setCloseWriter(false);
                doc.close();
                byte[] documentCopyBytes = null;
                try {
                    documentCopyBytes = (byte[]) getDebugBytesMethod.invoke(writer);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                writer.close();
                bais = new ByteArrayInputStream(documentCopyBytes);
                PdfReader reader = new PdfReader(bais);
                PdfDocument tempDoc = new PdfDocument(reader);
                rups.loadDocumentFromRawContent(documentCopyBytes, getVariableName(selection), null, true);
                if (prevDoc != null) {
                	rups.compareWith(prevDoc);
                }
                prevDoc = tempDoc;
            } else {
            	closeRoutine();
                comp.setVisible(false);    
            }
        } catch (DebugException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (final PdfException | com.itextpdf.io.IOException e) {
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			e.printStackTrace();
        		}
        	});
        } catch (final Exception e) {
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			e.printStackTrace();
        		}
        	});
        } finally {
            try {
                if (bais != null) {
                    bais.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean setFocus() {
        return false;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void init(IWorkbenchPartSite partSite) {
        // TODO Auto-generated method stub

    }

    public static boolean isPdfDocument(IStructuredSelection selection) {
        try {
            IJavaObject obj = getIJavaObject(selection);
            if (obj != null && obj.getJavaType().getName().equals(CLASS_TYPE)) {
                return true;
            }
        } catch (DebugException e) {
            //e.printStackTrace();
        }
        return false;
    }
    
    private void closeRoutine() {
    	try {
    		rups.closeDocument();
        	if (prevDoc != null) {
        		prevDoc.close();
        		prevDoc = null;
        	}
    	} catch (Exception any) {
    		any.printStackTrace();
    	}
    }

    private PdfDocument getPdfDocument(IStructuredSelection selection)
            throws DebugException, ClassNotFoundException, IOException {
        byte[] bytes = null;
        IJavaValue byteArr = null;
        IJavaObject obj = getIJavaObject(selection);
        if (obj != null && obj.getJavaType().getName().equals(CLASS_TYPE)) {
            IJavaVariable var = (IJavaVariable) selection.getFirstElement();

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
        bytes = getByteArray(byteArr);
        return createDocumentFromBytes(bytes);
    }

    private byte[] getByteArray(IJavaValue byteArr) throws DebugException {
        byte[] res = null;
        if (byteArr instanceof IJavaArray) {
            IJavaValue[] arr = ((IJavaArray) byteArr).getValues();
            res = new byte[arr.length];
            if (arr.length != 0 && arr[0] instanceof IJavaPrimitiveValue) {
                for (int i = 0; i < arr.length; ++i) {
                    res[i] = ((IJavaPrimitiveValue) arr[i]).getByteValue();
                }
            }
        }
        return res;
    }

    private PdfDocument createDocumentFromBytes(byte[] bytes) throws ClassNotFoundException, IOException {
    	if (bytes == null) {
    		return null;
    	}
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        PdfDocument doc = null;
        try {
            doc = (PdfDocument) new ObjectInputStream(bais).readObject();
        } finally {
            try {
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    private static IJavaObject getIJavaObject(IStructuredSelection selection) throws DebugException {
        IJavaObject res = null;
        if (selection != null && selection.size() != 0 && selection.getFirstElement() instanceof IJavaVariable) {
            IJavaVariable var = (IJavaVariable) selection.getFirstElement();
            IValue value;
            value = var.getValue();
            if (value instanceof IJavaObject) {
                res = (IJavaObject) value;
            }
        }
        return res;
    }

    private static String getVariableName(IStructuredSelection selection) throws DebugException {
        String res = null;
        if (selection != null && selection.size() != 0 && selection.getFirstElement() instanceof IJavaVariable) {
            IJavaVariable var = (IJavaVariable) selection.getFirstElement();
            res = var.getName();
        }
        return res;
    }
}