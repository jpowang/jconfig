package org.commons.jconfig.loader.adapters;

import org.codehaus.jackson.JsonNode;
import org.commons.jconfig.config.ConfigException;


/**
 * interface for ConfigLoader Module adapters.
 * 
 * Note : register new ConfigModuleAdapters with @ConfigLoader constructor
 * 
 * @author aabed
 */
public interface Adapter {

    /**
     * 
     * @return the uri of the adapter
     */
    String getUri();

    /**
     * Load configurations for given application module and return a Json node that adheres to the
     * standard autoConf Json syntax
     *
     * @return autoConf Json module node
     */
    JsonNode getModuleNode(String appName, String moduleName) throws ConfigException;
}
