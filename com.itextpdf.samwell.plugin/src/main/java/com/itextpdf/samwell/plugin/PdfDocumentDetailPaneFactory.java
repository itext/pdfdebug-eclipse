package com.itextpdf.samwell.plugin;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

public class PdfDocumentDetailPaneFactory implements IDetailPaneFactory {

    protected static final HashSet<String> ids = new HashSet<String>();
    protected static final HashSet<String> empty = new HashSet<String>();

    static {
        ids.add(RupsDetailPane.ID);
    }

    @Override
    public IDetailPane createDetailPane(String paneID) {
        return new RupsDetailPane();
    }

    @Override
    public String getDetailPaneName(String paneID) {
        return RupsDetailPane.NAME;
    }

    @Override
    public String getDetailPaneDescription(String paneID) {
        return RupsDetailPane.DESCRIPTION;
    }

    @Override
    public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
        if (RupsDetailPane.isPdfDocument(selection)) {
            return ids;
        }
        return empty;
    }

    @Override
    public String getDefaultDetailPane(IStructuredSelection selection) {
        return RupsDetailPane.ID;
    }
}