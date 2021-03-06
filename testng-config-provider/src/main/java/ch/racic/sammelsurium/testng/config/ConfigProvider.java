/*
 * Copyleft (c) 2015. This code is for learning purposes only.
 * Do whatever you like with it but don't take it as perfect code.
 * //Michel Racic (http://rac.su/+)//
 */

package ch.racic.sammelsurium.testng.config;

import ch.racic.sammelsurium.testng.config.annotation.ClassConfig;
import ch.racic.sammelsurium.testng.config.guice.ConfigModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by rac on 05.04.15.
 */
public class ConfigProvider {

    private static final Logger log = LogManager.getLogger(ConfigProvider.class);

    private ConfigEnvironment environment;
    private String clazz;

    private static final String CONFIG_BASE_FOLDER = "config";
    private static final String CONFIG_GLOBAL_BASE_FOLDER = "global";
    private static final String CONFIG_CLASS_FOLDER = "class";

    private AggregatedResourceBundle propsGlobal, propsEnv, propsGlobalClass, propsEnvClass;

    private FilenameFilter propertiesFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if (
            /** Filter locale bundles as we will construct the ResourceBundle with the default file **/
                    name.matches("^([a-zA-Z0-9.-]*\\.properties)$")
                    ) return true;
            else return false;
        }
    };

    public ConfigProvider(ConfigEnvironment environment, Class clazz) {
        this.environment = environment;
        // Extract class name for loading if annotation present
        ClassConfig classConfig = (ClassConfig) clazz.getAnnotation(ClassConfig.class);
        if (classConfig != null && classConfig.value() != null) this.clazz = classConfig.value().getSimpleName();
        else if (classConfig != null && classConfig.fileName().contentEquals("")) this.clazz = classConfig.fileName();
        else this.clazz = clazz.getSimpleName();

        loadProperties();
    }

    private void loadProperties() {
        // check that base config directory exists to be used as a base for all following actions
        File configBaseFolder = FileUtils.toFile(getClass().getClassLoader().getResource(CONFIG_BASE_FOLDER));
        if (!(configBaseFolder != null && configBaseFolder.exists() && configBaseFolder.isDirectory())) {
            log.error("Configuration initialisation error", new IOException("Config folder not existing or not a directory"));
        }

        // Load global base properties
        File configGlobalBaseFolder = new File(configBaseFolder, CONFIG_GLOBAL_BASE_FOLDER);
        if (configGlobalBaseFolder.exists() && configGlobalBaseFolder.isDirectory()) {
            propsGlobal = new AggregatedResourceBundle();
            // Get list of files in global folder and load it into props
            for (File f : configGlobalBaseFolder.listFiles(propertiesFilter)) {

                log.debug("Loading PropertiesResourceBundle from " + f.getPath() + ((environment.getLocale() != null) ? " using locale " + environment.getLocale() : ""));
                if (environment != null && environment.getLocale() != null)
                    propsGlobal.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + CONFIG_GLOBAL_BASE_FOLDER + "." + f.getName().replace(".properties", ""), environment.getLocale()));
                else
                    propsGlobal.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + CONFIG_GLOBAL_BASE_FOLDER + "." + f.getName().replace(".properties", "")));
            }
        } else {
            log.warn("global folder not existing, can't load default values");
        }

        // Load environment base properties
        File configEnvironmentBaseFolder = null;
        if (environment != null) {
            configEnvironmentBaseFolder = new File(configBaseFolder, environment.getCode());
            if (!(configEnvironmentBaseFolder.exists() && configEnvironmentBaseFolder.isDirectory())) {
                log.error("Configuration initialisation error", new IOException("Environment specific configuration folder does not exist for " + environment));
            }
            propsEnv = new AggregatedResourceBundle();
            // Get list of files in environment folder and load it into props, overriding existing global properties
            for (File f : configEnvironmentBaseFolder.listFiles(propertiesFilter)) {
                log.debug("Loading PropertiesResourceBundle from " + f.getPath() + ((environment.getLocale() != null) ? " using locale " + environment.getLocale() : ""));
                if (environment.getLocale() != null)
                    propsEnv.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + environment.getCode() + "." + f.getName().replace(".properties", ""), environment.getLocale()));
                else
                    propsEnv.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + environment.getCode() + "." + f.getName().replace(".properties", "")));
            }
        }

        // Load global class properties
        if (configGlobalBaseFolder.exists() && configGlobalBaseFolder.isDirectory() && clazz != null) {
            // Check if a class file exists
            File classProps = new File(configGlobalBaseFolder, CONFIG_CLASS_FOLDER + "/" + clazz + ".properties");
            if (classProps.exists() && classProps.isFile()) {
                propsGlobalClass = new AggregatedResourceBundle();
                // It exists, load it into props
                log.debug("Loading PropertiesResourceBundle from " + classProps.getPath() + ((environment.getLocale() != null) ? " using locale " + environment.getLocale() : ""));
                if (environment.getLocale() != null)
                    propsGlobalClass.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + CONFIG_GLOBAL_BASE_FOLDER + "." + CONFIG_CLASS_FOLDER + "." + clazz, environment.getLocale()));
                else
                    propsGlobalClass.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + CONFIG_GLOBAL_BASE_FOLDER + "." + CONFIG_CLASS_FOLDER + "." + clazz));
            }
        }

        // Load environment class properties
        if (configEnvironmentBaseFolder != null && clazz != null) {
            // Check if a class file exists
            File classProps = new File(configEnvironmentBaseFolder, CONFIG_CLASS_FOLDER + "/" + clazz + ".properties");
            if (classProps.exists() && classProps.isFile()) {
                propsEnvClass = new AggregatedResourceBundle();
                // It exists, load it into props
                log.debug("Loading PropertiesResourceBundle from " + classProps.getPath() + ((environment.getLocale() != null) ? " using locale " + environment.getLocale() : ""));
                if (environment.getLocale() != null)
                    propsEnvClass.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + environment.getCode() + "." + CONFIG_CLASS_FOLDER + "." + clazz, environment.getLocale()));
                else
                    propsEnvClass.merge(ResourceBundle.getBundle(CONFIG_BASE_FOLDER + "." + environment.getCode() + "." + CONFIG_CLASS_FOLDER + "." + clazz));
            }
        }

    }

    public ConfigEnvironment getEnvironment() {
        return environment;
    }

    public String getClazz() {
        return clazz;
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        if (propsEnvClass != null && propsEnvClass.containsKey(key)) {
            log.debug("Retrieved property [" + key + "] from Environment class properties");
            return propsEnvClass.getString(key);
        } else if (propsGlobalClass != null && propsGlobalClass.containsKey(key)) {
            log.debug("Retrieved property [" + key + "] from Global class properties");
            return propsGlobalClass.getString(key);
        } else if (propsEnv != null && propsEnv.containsKey(key)) {
            log.debug("Retrieved property [" + key + "] from Environment properties");
            return propsEnv.getString(key);
        } else if (propsGlobal != null && propsGlobal.containsKey(key)) {
            log.debug("Retrieved property [" + key + "] from Global properties");
            return propsGlobal.getString(key);
        } else {
            log.warn("Property [" + key + "] has not been found, returning default value");
            return defaultValue;
        }
    }

    public void logAvailableProperties() {
        logProperties("Global", propsGlobal);
        if (environment != null) logProperties("Environment " + environment, propsEnv);
        logProperties("Global class", propsGlobalClass);
        if (environment != null) logProperties("Environment class " + environment, propsEnvClass);
    }

    private void logProperties(String title, AggregatedResourceBundle props) {
        if (props == null) return;
        log.info("CM Properties available from " + title);
        for (String key : props.keySet())
            log.info("\tKey[" + key + "], Value[" + props.getString(key) + "]");
    }

    /**
     * Create an instance of a class using the config injector and any guice modules given in parameters.
     *
     * @param type
     * @param modules ...
     * @param <T>
     * @return
     */
    public <T> T create(Class<T> type, Module... modules) {
        List<Module> mList = new ArrayList<Module>(Arrays.asList(modules));
        mList.add(new ConfigModule(environment, type));
        Injector injector = com.google.inject.Guice.createInjector(mList);
        return injector.getInstance(type);
    }

}
