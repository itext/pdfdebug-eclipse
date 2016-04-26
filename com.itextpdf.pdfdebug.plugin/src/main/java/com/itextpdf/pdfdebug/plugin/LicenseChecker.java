package com.itextpdf.pdfdebug.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.osgi.service.prefs.BackingStoreException;

import com.itextpdf.licensekey.LicenseKey;
import com.itextpdf.licensekey.LicenseKeyException;
import com.itextpdf.licensekey.LicenseKeyProduct;
import com.itextpdf.licensekey.LicenseKeyProductFeature;


public class LicenseChecker {
	
	private static final String LICENSE_KEY = "license";
	private static final String PRODUCT_NAME = "pdfDebug";
    private static final int PRODUCT_MAJOR = 1;
    private static final int PRODUCT_MINOR = 0;
	
	public static void initLicense() {
    	IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.getPluginId());
    	ByteArrayInputStream bais = null;
    	try {
    		prefs.sync();
        	byte[] licenseBytes = prefs.getByteArray(LICENSE_KEY, null);
    		if (licenseBytes != null) {
        		bais = new ByteArrayInputStream(licenseBytes);
        		LicenseKey.loadLicenseFile(bais);
        	}
    	} catch (BackingStoreException e) {
			e.printStackTrace();
		} finally {
    		if (bais != null) {
    			try {
					bais.close();
				} catch (IOException ignored) {
				}
    		}
    	}
    }
	
	public static boolean checkLicense() {
		try {
			LicenseKey.scheduledCheck(new LicenseKeyProduct(PRODUCT_NAME, PRODUCT_MAJOR, PRODUCT_MINOR, new LicenseKeyProductFeature[] {}));
			return true;
		} catch (LicenseKeyException e) {
			LicenseCheckErrorDialog dlg = new LicenseCheckErrorDialog(e.getMessage());
			int res = dlg.open();
			if (res == LicenseCheckErrorDialog.CANCEL_RESULT) {
				return false;
			} else if (res == LicenseCheckErrorDialog.CHOOSE_RESULT) {
				FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String [] {"*.xml"});
				String path = dialog.open();
				loadLicense(path);
			}
			return checkLicense();
		}
	}
	
	public static void clearLicense() {
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.getPluginId());
		prefs.remove(LICENSE_KEY);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadLicense(String path) {
		ByteArrayOutputStream baos = null;
		try {
			LicenseKey.loadLicenseFile(path);
			baos = new ByteArrayOutputStream();
			File file = new File(path);
			Files.copy(file.toPath(), baos);
			baos.flush();
			IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.getPluginId());
			prefs.putByteArray(LICENSE_KEY, baos.toByteArray());
			prefs.flush();
		} catch (Exception e) {
			MessageDialog.openError(null, "Error while opening license file", e.getMessage());
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException ignored) {
				}
			}
		}
	}
	
	private static class LicenseCheckErrorDialog extends MessageDialog{

		private static final String PRE_MESSAGE = "There was an error duiring license check for PDF Debug plugin:\n";
		private static final String POST_MESSAGE = "\nPlease specify valid license file";
		private static final String TITLE = "License check Error!";
		private static final String[] LABELS = {"Open file", "Cancel"};
		public static final int CANCEL_RESULT = 1;
		public static final int CHOOSE_RESULT = 0;
		
		public LicenseCheckErrorDialog(String errorMessage) {
			super(null, TITLE, null, PRE_MESSAGE + errorMessage + POST_MESSAGE, MessageDialog.ERROR, LABELS, CANCEL_RESULT);
		}
	}
}
