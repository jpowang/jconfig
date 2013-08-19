package org.commons.jconfig.configloader;

/**
 * Test PimCacheConfig interface 
 * 
 * @author aabed
 */

public interface PimCacheConfigMXBean {

    public String getPimLmaSendTimeout();
    public String getPimLmaRecvTimeout();
    public String getPimEmdSendTimeout();
    public String getPimEmdRecvTimeout();
    public String    getPimRetry();
    
    public void setPimLmaSendTimeout(String msec);
    public void setPimLmaRecvTimeout(String msec);
    public void setPimEmdSendTimeout(String msec);
    public void setPimEmdRecvTimeout(String msec);
    public void setPimRetry(String retry);
    
    public String getLoaderAdapter();
    
}