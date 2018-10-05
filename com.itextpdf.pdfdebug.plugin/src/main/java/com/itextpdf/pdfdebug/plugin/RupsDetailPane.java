package com.itextpdf.pdfdebug.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;

import com.itextpdf.rups.Rups;
import com.itextpdf.rups.event.RupsEvent;
import com.itextpdf.rups.model.LoggerHelper;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.pdfdebug.plugin.utilities.DebugUtilities;
import com.itextpdf.pdfdebug.plugin.utilities.PdfDocumentUtilities;

public class RupsDetailPane implements IDetailPane {

	private volatile Composite mainComp;
    private volatile StackLayout layout;
	
	private volatile Composite rupsView;
    private volatile Text defaultView;
    
    private volatile Rups rups;
    private volatile JPanel panel;
    private volatile Frame frame;

    public static final String ID = "SamwellDetailPane";
    public static final String NAME = "PdfDocument Detail Pane";
    public static final String DESCRIPTION = "Detail pane that displays an end structure of PdfDocument object.";
    
    
    private volatile PdfDocument prevDoc;

    @Override
    public Control createControl(Composite parent) {
    	layout = new StackLayout();
    	mainComp = new Composite(parent, SWT.NONE);
    	mainComp.setLayout(layout);
    	mainComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        rupsView = new Composite(mainComp, SWT.EMBEDDED);
        rupsView.setLayoutData(new GridData(GridData.FILL_BOTH));
        frame = SWT_AWT.new_Frame(rupsView);
        final Dimension dim = new Dimension(parent.getSize().x, parent.getSize().y);
        defaultView = new Text(mainComp, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL );
        defaultView.setLayoutData(new GridData(GridData.FILL_BOTH));
        layout.topControl = defaultView;
        final JApplet applet = new JApplet();
        frame.add(applet);
        panel = new JPanel(new BorderLayout());
        applet.add(panel, BorderLayout.CENTER);
        rups = Rups.startNewPlugin(panel, dim, frame);
        return mainComp;
    }

    @Override
    public void dispose() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                closeRoutine();
                frame.dispose();
            }
        });
    	rupsView.dispose();
    	defaultView.dispose();
    	mainComp.dispose();
    }

    @Override
    public void display(IStructuredSelection selection) {
    	layout.topControl = defaultView;
    	defaultView.setText("");
    	ByteArrayInputStream bais = null;
    	IJavaVariable var = DebugUtilities.getIJavaVariable(selection);
        try {
        	if (var != null) {
        		defaultView.setText(var.getValue().toString());
        	}
        	if (PdfDocumentUtilities.isPdfDocument(var)) {
                layout.topControl = rupsView;
            	byte[] documentRawBytes = PdfDocumentUtilities.getDocumentDebugBytes(var);
                if (documentRawBytes != null) {
                	bais = new ByteArrayInputStream(documentRawBytes);
                    PdfReader reader = new PdfReader(bais);
                    PdfDocument tempDoc = new PdfDocument(reader);
                    boolean isEqual = false;
                    if (prevDoc != null) {
                    	isEqual = rups.compareWithDocument(tempDoc, true);
                    }
                    if (!isEqual) {
                    	listenOnetimeForHighlight(rups);
                    	rups.loadDocumentFromRawContent(documentRawBytes, DebugUtilities.getVariableName(selection), null, true);
                    }
                    if (prevDoc != null) {
                    	rups.highlightLastSavedChanges();
                    }
                    prevDoc = tempDoc;
                } else {
                	rups.clearHighlights();
                }
            } else {
            	closeRoutine();
            }
        } catch (final IOException | PdfException | com.itextpdf.io.IOException e) {
        	LoggerHelper.error("Error while reading pdf file.", e, getClass());
        } catch (final Exception e) {
        	LoggerHelper.error("Unexpected error.", e, getClass());
        } finally {
            try {
                if (bais != null) {
                    bais.close();
                }
            } catch (IOException ignored) {
            }
        }
        mainComp.layout();
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
    		LoggerHelper.error("Closing error.", any, getClass());
    	}
    }

    private static void listenOnetimeForHighlight(final Rups rups) {
        final Observer openObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (!(arg instanceof RupsEvent)) return;
                RupsEvent re = (RupsEvent) arg;
                // only cares for OPEN_DOCUMENT_POST_EVENT
                if(re.getType()!=RupsEvent.OPEN_DOCUMENT_POST_EVENT) return;
                final Observer listener = this;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        rups.highlightLastSavedChanges();
                        rups.unregisterEventObserver(listener);
                    }
                });
            }
        };

        rups.registerEventObserver(openObserver);
    }
}