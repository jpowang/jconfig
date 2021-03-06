package org.commons.jconfig.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.log4j.Logger;
import org.commons.jconfig.config.ConfigContext;
import org.commons.jconfig.config.ConfigManager;
import org.commons.jconfig.config.ConfigRuntimeException;
import org.commons.jconfig.internal.ConfigAdapterJson.CONST;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Encapsulation for parsing and saving config values from ConfigLoader which
 * calls via JMX operations
 */
@ThreadSafe
public class ConfigManagerCache {
    private final Logger logger = Logger.getLogger(ConfigManagerCache.class);

    private final AtomicReference<Map<String, ClassMemConfig>> readableCacheRef = new AtomicReference<Map<String, ClassMemConfig>>(
            new ConcurrentHashMap<String, ClassMemConfig>());
    private final ConcurrentHashMap<String, ClassMemConfig> writableCache = new ConcurrentHashMap<String, ClassMemConfig>();
    private final ConfigManager configManager;

    private class ClassMemConfig {


        @Override
        public String toString() {
            return keyMap.toString();
        }

        /**
         * setType = FARM
         * 
         * Structure of keyMap:
         * 
         * { FARM1 : { a:b, d:e }, FARM2 : {f:d, z:y },_Defs_ : {} }
         */
        private final Map<String, Map<String, JsonElement>> keyMap = new HashMap<String, Map<String, JsonElement>>();

        /** setType defined for this module or null if no setType is defined */
        private final TreeSet<String> contextTypes = new TreeSet<String>();
        private String setType = null;

        public ClassMemConfig(final JsonObject configValue) {
            parseAndSaveValues(configValue);
        }
        public JsonElement get(final ConfigContext context, final String fileId) {
            JsonElement value = null;
            if (setType != null) {
                Map<String, JsonElement> values = keyMap.get(context.get(setType));
                if (values != null) {
                    value = values.get(fileId);
                }
            }
            if (value == null) {
                Map<String, JsonElement> values = keyMap.get(CONST.DEFAULTS.toString());
                value = values.get(fileId);
            }
            if (value != null) {
                return value;
            } else {
                return null;
            }
        }

        /**
         * Parse and Save config values
         * @param value Json data to be parsed and inserted. e.g below:
            {
                "_Sets_Type_": "COLO",
                "_Sets_": [
                    {
                        "key": [ "ne1"],
                        "keyList": {
                            "SonoraHostname" : "google.com"
                        }

                    }

                ],
                "SonoraHostname" : "localhost"
                }
            }
         */
        public void parseAndSaveValues(final JsonObject value) {
            JsonArray sets = value.getAsJsonArray(CONST.SETS.toString());
            if (sets != null) {
                JsonPrimitive localSetType = value.getAsJsonPrimitive(CONST.SETS_TYPE.toString());
                if (localSetType != null) {
                    if (setType != null && !setType.equals(localSetType.getAsString())) {
                        throw new ConfigRuntimeException("Cannot override registered " + "_Sets_Type_ " + setType + " by " + localSetType.getAsString());
                    } else {
                        setType = localSetType.getAsString();
                        contextTypes.add(setType);
                    }
                } else {
                    throw new ConfigRuntimeException("Config object is missing " + CONST.SETS_TYPE.toString() + " property : " + value);
                }
                for (JsonElement node : sets) {
                    Map<String, JsonElement> values = new HashMap<String, JsonElement>();
                    if (node.isJsonObject() && node.getAsJsonObject().getAsJsonObject("keyList") != null) {
                        for (Entry<String, JsonElement> entry : node.getAsJsonObject().getAsJsonObject("keyList")
                                .entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(CONST.SETS.toString()) || entry.getKey().equalsIgnoreCase(CONST.SETS_TYPE.toString())) {
                                continue;
                            }
                            if (entry.getValue().isJsonPrimitive()) {
                                values.put(entry.getKey(), entry.getValue());
                            } else if (entry.getValue().isJsonObject()) {
                                values.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    if (node.isJsonObject() && node.getAsJsonObject().getAsJsonArray("key") != null) {
                        for (JsonElement key : node.getAsJsonObject().getAsJsonArray("key")) {
                            if (keyMap.containsKey(key.getAsString())) {
                                /*
                                 * If key is present than "merge" new values
                                 * with values already present in keyMap
                                 */
                                Map<String, JsonElement> setValues = keyMap.get(key.getAsString());
                                for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                                    setValues.put(entry.getKey(), entry.getValue());
                                }
                            } else {
                                /*
                                 * create new hashmap for every element in list.
                                 * this eliminates editing same map for
                                 * different keys
                                 */
                                keyMap.put(key.getAsString(), new HashMap<String, JsonElement>(values));
                            }
                        }
                    }
                }
            }

            // parse and save defaults
            Map<String, JsonElement> values = new HashMap<String, JsonElement>();
            if (value.isJsonObject()) {
                for (Entry<String, JsonElement> entry : value.entrySet()) {
                    // sets and set_type are already being processed above
                    if (entry.getKey().equalsIgnoreCase(CONST.SETS.toString()) || entry.getKey().equalsIgnoreCase(CONST.SETS_TYPE.toString())) {
                        continue;
                    }
                    if (entry.getValue().isJsonPrimitive()) {
                        values.put(entry.getKey(), entry.getValue());
                    } else if (entry.getValue().isJsonObject()) {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (keyMap.containsKey(CONST.DEFAULTS.toString())) {
                /*
                 * If key is present than "merge" new values
                 * with values already present in keyMap
                 */
                Map<String, JsonElement> defaultValues = keyMap.get(CONST.DEFAULTS.toString());
                for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                    defaultValues.put(entry.getKey(), entry.getValue());
                }
            } else {
                keyMap.put(CONST.DEFAULTS.toString(), values);
            }
        }

        /**
         * Returns the list of content types used by this config object
         * 
         * @return
         */
        public SortedSet<String> getContextTypes() {
            return contextTypes;
        }
    }
    public ConfigManagerCache(final ConfigManager configManager) {
        this.configManager = configManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see common.config.internal.ConfigFormat#get(java.lang.Object,
     * common.config.ConfigContext, java.lang.String,
     * java.lang.String)
     */
    public String get(final Object config, final ConfigContext context, final String field, final String defaultValue) {
        Map<String, ClassMemConfig> localCache = readableCacheRef.get();
        // TODO: Find which application it is and if application is not present
        // that use modules under DEFAULT_APP
        String configName = config.getClass().getName();

        ClassMemConfig classConfig = null;

        if (localCache.containsKey(configName)) {
            classConfig = localCache.get(configName);
        } else if (localCache.containsKey(CONST._PROP_.toString())) {
            classConfig = localCache.get(CONST._PROP_.toString());
        } else {
            return defaultValue;
        }
        JsonElement value = classConfig.get(context, field);
        if (value == null) {
            return defaultValue;
        } else {
            if (value.isJsonPrimitive()) {
                return value.getAsJsonPrimitive().getAsString();
            } else if (value.isJsonObject()) {
                return value.getAsJsonObject().toString();
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the list of Context Types used by current ConfigManager cache
     * 
     * @param config
     * @return
     */
    public <T> SortedSet<String> getContextTypes(final Class<T> classDefinition) {
        Map<String, ClassMemConfig> localCache = readableCacheRef.get();
        // TODO: Find which application it is and if application is not present
        // that use modules under DEFAULT_APP
        String configName = classDefinition.getName();

        ClassMemConfig classConfig = null;

        if (localCache.containsKey(configName)) {
            classConfig = localCache.get(configName);
        } else if (localCache.containsKey(CONST._PROP_.toString())) {
            classConfig = localCache.get(CONST._PROP_.toString());
        } else {
            return EMPTY_SET;
        }
        return classConfig.getContextTypes();
    }

    private final SortedSet<String> EMPTY_SET = Collections.unmodifiableSortedSet(new TreeSet<String>());

    /**
     * /** Add/update an autoConf value to our memory structure. Gets called via
     * ConfigObjectMBean setAttribute JMX calls.
     * 
     * e.g string for value parameter
        {
            "_Sets_Type_": "COLO",
            "_Sets_": [
                {
                    "key": [ "ne1" ],
                    "keyList": {
                        "SonoraHostname" : "google.com"
                    }
                }
            ],
            "SonoraHostname" : "localhost"
            }
        }

     * @param moduleName
     *            Module for which value needs to be set
     * @param jsonValue
     *            Config value in json format
     */
    public void insertValue(@Nonnull final String moduleName, @Nonnull final String jsonValue) {
        // convert JSON into java object
        if (logger.isDebugEnabled()) {
            logger.debug("Set value " + jsonValue + " for module " + moduleName);
        }
        JsonParser parser = new JsonParser();
        JsonObject json = (JsonObject) parser.parse(jsonValue);

        // Block all writers on a flipcache operation
        synchronized (writeLock) {
            if (writableCache.containsKey(moduleName)) {
                writableCache.get(moduleName).parseAndSaveValues(json);
            } else {
                writableCache.put(moduleName, new ClassMemConfig(json));
            }
        }
    }

    private final Object writeLock = new Object();

    public void flipCache() {
        synchronized (writeLock) {
            ConcurrentHashMap<String, ClassMemConfig> newReadableCache = new ConcurrentHashMap<String, ClassMemConfig>(
                    writableCache);
            writableCache.clear();
            readableCacheRef.lazySet(newReadableCache);
            configManager.setLoadingDone();
        }
        logger.info("Loading new config values from JMX. " + readableCacheRef.get().toString());
    }

    /**
     * Checks if module is loaded
     * @param config
     * @return true if Module is loaded else return false
     */
    public boolean isModuleLoaded(Object config) {
        return readableCacheRef.get().containsKey(config.getClass().getName());
    }
}
