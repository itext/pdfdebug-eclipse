package com.itextpdf.samwell.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.itextpdf.licensekey.LicenseKey;

public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static String pluginId;

	// The shared instance
	private static Activator plugin;
	
	private static final String LICENSE_KEY = "license";
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		pluginId = getBundle().getSymbolicName();
		initLicense();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getInstance() {
		return plugin;
	}
	
	public static String getPluginId() {
		return pluginId;
	}
	
	public static void checkLicense() {
		
	}

	private void initLicense() {
    	IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(pluginId);
    	ByteArrayInputStream bais = null;
    	byte[] licenseBytes = prefs.getByteArray(LICENSE_KEY, null);
    	try {
    		if (licenseBytes != null) {
        		bais = new ByteArrayInputStream(licenseBytes);
        		LicenseKey.loadLicenseFile(bais);
        	}
    	} finally {
    		if (bais != null) {
    			try {
					bais.close();
				} catch (IOException ignored) {
				}
    		}
    	}
    }
}
