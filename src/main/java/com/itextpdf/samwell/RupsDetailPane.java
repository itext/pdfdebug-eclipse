package com.itextpdf.samwell;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaPrimitiveValue;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPartSite;
import com.itextpdf.rups.Rups;
import com.itextpdf.rups.controller.RupsController;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.pdf.*;


public class RupsDetailPane implements IDetailPane {
	
	private Composite comp;
	private JLabel label;
	private Rups rups;
	private JPanel panel;
	private java.awt.Frame frame;
	
	public static final String ID = "SamwellDetailPane";
	public static final String NAME = "PdfDocument Detail Pane";
	public static final String DESCRIPTION = "Detail pane that displays an end structure of PdfDocument object.";
	public static final String CLASS_TYPE = "com.itextpdf.kernel.pdf.PdfDocument";
	public static final String METHOD_SIGNATURE = "()[B";
	public static final String METHOD_NAME = "getSerializedBytes";
	
	@Override
	public Control createControl(Composite parent) {
		// TODO Auto-generated method stub
		panel = new JPanel(new BorderLayout());
		comp = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		frame = SWT_AWT.new_Frame(comp);
		frame.add(panel, BorderLayout.CENTER);
		label = new JLabel("plugin is in other window");
		//panel.add(label);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd);
		Dimension dim = new Dimension(parent.getSize().x, parent.getSize().y);
		frame.setSize(dim);
		rups = Rups.startNewPlugin(panel, dim);
		return comp;
	}

	@Override
	public void dispose() {
		rups.closeDocument();
		comp.dispose();
		// TODO Auto-generated method stub
	}

	@Override
	public void display(IStructuredSelection selection) {
		ByteArrayOutputStream baos = null;
		ByteArrayInputStream bais = null;
		try {
			if (isPdfDocument(selection)) {
				PdfDocument doc = getPdfDocument(selection);
				baos = doc.getWriter().getByteArrayOutputStream();
				doc.getWriter().setCloseStream(true);
				doc.setCloseWriter(true);
				doc.close();
		    	bais = new ByteArrayInputStream(baos.toByteArray());
		    	rups.loadDocumentFromStream(bais, getVariableName(selection), null, true);
			} else {
				bais = null;
			}
		} catch (DebugException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PdfException | com.itextpdf.io.IOException e) {
			rups.closeDocument();
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return ID;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
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
	
	private PdfDocument getPdfDocument(IStructuredSelection selection)
			throws DebugException, ClassNotFoundException, IOException{
		byte[] bytes = null;
		IJavaValue byteArr = null;
		IJavaObject obj = getIJavaObject(selection);
		if (obj != null && obj.getJavaType().getName().equals(CLASS_TYPE)) {
			IJavaVariable var = (IJavaVariable)selection.getFirstElement();
			
			IThread owningThread = obj.getOwningThread();
			if (owningThread instanceof IJavaThread) {
				byteArr = obj.sendMessage(METHOD_NAME, METHOD_SIGNATURE, null, (IJavaThread)owningThread, false);
			} else {
				for (IThread th : obj.getDebugTarget().getThreads()) {
					IJavaVariable newVar = ((IJavaThread)th).findVariable(var.getName());
					if (var.equals(newVar)) {
						byteArr = obj.sendMessage(METHOD_NAME, METHOD_SIGNATURE, null, (IJavaThread)th, false);
					}	
				}
			}
		}
		bytes = getByteArray(byteArr);
		return createDocumentFromBytes(bytes);
	}
	
	private byte[] getByteArray(IJavaValue byteArr) throws DebugException{
		byte[] res = null; 
		if (byteArr instanceof IJavaArray) {
			IJavaValue[] arr = ((IJavaArray)byteArr).getValues();
			res = new byte[arr.length];
			if (arr.length != 0 && arr[0] instanceof IJavaPrimitiveValue) {
				for (int i = 0; i < arr.length; ++i) {
					res[i] = ((IJavaPrimitiveValue)arr[i]).getByteValue();
				}
			}
		}
		return res;
	}
	
	private PdfDocument createDocumentFromBytes(byte[] bytes) throws ClassNotFoundException, IOException {
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
	
	private static IJavaObject getIJavaObject(IStructuredSelection selection) throws DebugException{
		IJavaObject res = null;
		if (selection != null && selection.size() != 0 && selection.getFirstElement() instanceof IJavaVariable) {
			IJavaVariable var = (IJavaVariable)selection.getFirstElement();
			IValue value;
			value = var.getValue();
			if (value instanceof IJavaObject) {
				res = (IJavaObject)value;
			}
		}				
		return res;
	}
	
	private static String getVariableName(IStructuredSelection selection) throws DebugException{
		String res = null;
		if (selection != null && selection.size() != 0 && selection.getFirstElement() instanceof IJavaVariable) {
			IJavaVariable var = (IJavaVariable)selection.getFirstElement();
			res = var.getName();
		}				
		return res;
	} 
}