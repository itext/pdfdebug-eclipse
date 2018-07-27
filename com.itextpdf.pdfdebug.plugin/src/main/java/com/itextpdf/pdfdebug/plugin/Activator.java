package com.itextpdf.pdfdebug.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * This class is the main entrypoint for the pdfDebug plugin
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    private static String pluginId;

    // The shared instance
    private static Activator plugin;

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

    /**
     * Returns the pluginID
     *
     * @return the pluginID
     */
    public static String getPluginId() {
        return pluginId;
    }
}
