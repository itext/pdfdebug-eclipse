package com.itextpdf.samwell.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.ByteArrayInputStream;

import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;

import com.itextpdf.rups.Rups;
import com.itextpdf.rups.model.SwingHelper;
import com.itextpdf.samwell.plugin.utilities.DebugUtilities;
import com.itextpdf.samwell.plugin.utilities.PdfDocumentUtilities;
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
    
    
    private volatile PdfDocument prevDoc;

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
    	IJavaVariable var = DebugUtilities.getIJavaVariable(selection);
        try {
        	if (PdfDocumentUtilities.isPdfDocument(var)) {
            	comp.setVisible(true);
                byte[] documentRawBytes = PdfDocumentUtilities.getDocumentDebugBytes(var);
                if (documentRawBytes != null) {
                	bais = new ByteArrayInputStream(documentRawBytes);
                    PdfReader reader = new PdfReader(bais);
                    PdfDocument tempDoc = new PdfDocument(reader);
                    rups.loadDocumentFromRawContent(documentRawBytes, DebugUtilities.getVariableName(selection), null, true);
                    if (prevDoc != null) {
                    	rups.compareWithDocument(prevDoc);
                    }
                    prevDoc = tempDoc;
                }
            } else {
            	closeRoutine();
                comp.setVisible(false);    
            }
        } catch (final IOException | PdfException | com.itextpdf.io.IOException e) {
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
            } catch (IOException ignored) {
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

    

    
}