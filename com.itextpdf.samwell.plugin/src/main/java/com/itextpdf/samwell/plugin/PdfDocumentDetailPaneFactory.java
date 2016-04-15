package com.itextpdf.samwell.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

public class PdfDocumentDetailPaneFactory implements IDetailPaneFactory {

    protected static final HashSet<String> ids = new HashSet<>();
    protected static final HashMap<String, String> names = new HashMap<>();
    protected static final HashMap<String, String> descriptions = new HashMap<>();
    protected static final HashMap<String, Class<? extends IDetailPane> > classes = new HashMap<>();

    static {
        ids.add(RupsDetailPane.ID);
        names.put(RupsDetailPane.ID, RupsDetailPane.NAME);
        descriptions.put(RupsDetailPane.ID, RupsDetailPane.DESCRIPTION);
        classes.put(RupsDetailPane.ID, RupsDetailPane.class);
    }

    @Override
    public IDetailPane createDetailPane(String paneID) {
    	Class<? extends IDetailPane> paneClass = classes.get(paneID);
    	if (paneClass != null) {
    		try {
				return paneClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
    	}
    	return null;
    }

    @Override
    public String getDetailPaneName(String paneID) {
        return names.get(paneID);
    }

    @Override
    public String getDetailPaneDescription(String paneID) {
        return descriptions.get(paneID);
    }

    @Override
    public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
        if (RupsDetailPane.isPdfDocument(selection)) {
            return ids;
        }
        return Collections.emptySet();
    }

    @Override
    public String getDefaultDetailPane(IStructuredSelection selection) {
        if (RupsDetailPane.isPdfDocument(selection)) {
        	return RupsDetailPane.ID;
        }
        return null;
    }
}