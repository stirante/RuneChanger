package com.stirante.RuneChanger.util;

import com.stirante.RuneChanger.DebugConsts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LangHelper {

    private static ResourceBundle resourceBundle;

    /**
     * Get default resource bundle
     *
     * @return default resource bundle
     */
    @SuppressWarnings("ConstantConditions")
    public static ResourceBundle getLang() {
        if (resourceBundle == null) {
            if (DebugConsts.OVERRIDE_LANGUAGE != null) {
                resourceBundle =
                        ResourceBundle.getBundle("lang.messages", Locale.forLanguageTag(DebugConsts.OVERRIDE_LANGUAGE), new UTF8Control());
            }
            else if (SimplePreferences.getValue("force_english") != null &&
                    SimplePreferences.getValue("force_english").equalsIgnoreCase("true")) {
                resourceBundle =
                        ResourceBundle.getBundle("lang.messages", Locale.ROOT, new UTF8Control());
            }
            else {
                resourceBundle = ResourceBundle.getBundle("lang.messages", new UTF8Control());
            }
        }
        return resourceBundle;
    }

    /**
     * Code from https://stackoverflow.com/a/4660195/6459649
     */
    public static class UTF8Control extends ResourceBundle.Control {
        public ResourceBundle newBundle
                (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException {
            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            }
            else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
