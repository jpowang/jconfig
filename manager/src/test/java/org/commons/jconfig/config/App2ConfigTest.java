package org.commons.jconfig.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.util.concurrent.TimeUnit;

import javax.management.NotificationListener;

import mockit.Deencapsulation;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;

import org.commons.jconfig.annotations.Config;
import org.commons.jconfig.annotations.ConfigResource;
import org.commons.jconfig.config.ConfigContext;
import org.commons.jconfig.config.ConfigManager;
import org.commons.jconfig.config.ConfigRuntimeException;
import org.commons.jconfig.config.KeyNotFound;
import org.commons.jconfig.config.ConfigContext.Entry;
import org.commons.jconfig.datatype.ByteUnit;
import org.commons.jconfig.internal.jmx.ConfigLoaderJvm;
import org.commons.jconfig.internal.jmx.VirtualMachineException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class App2ConfigTest {
    @MockClass(realClass = ConfigLoaderJvm.class)
    public static class MockConfigLoaderJvm {
        @Mock
        public void attach() {}

        @Mock
        public void addNotificationListener(final NotificationListener listener) throws VirtualMachineException {}

        @Mock
        public void subscribeConfigs(final String appName) throws VirtualMachineException {
            Deencapsulation.invoke(ConfigManager.INSTANCE, "handleLoadAppConfigsNotification");
        }
    }

    @BeforeClass
    public void setUp() {
        Mockit.setUpMock(new MockConfigLoaderJvm());
        ConfigManager.INSTANCE.getConfig(App2Config.class, new ConfigContext(new Entry("SUBSET1", "706")));
    }

    @AfterClass
    public void tearDown() {
        Mockit.tearDownMocks();
    }

    @Test(expectedExceptions = ConfigRuntimeException.class)
    public void cachedContextWithMissingSetType() {
        ConfigContext context1 = new ConfigContext(new Entry("A", "706"));
        assertEquals(context1.getUniqueId(), "A706");
        ConfigContext context2 = ConfigContext.EMPTY;
        ConfigManager.INSTANCE.getConfig(App2Config.class, context1);
        ConfigManager.INSTANCE.getConfig(App2Config.class, context2);

        Assert.fail("Testcase should have failed on previous line");
    }

    @Test
    public void getCluster() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "706"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getLocalCluster(), "gq1");
    }

    @Test
    public void getFarm() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "505"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getLocalFarm(), "505");
    }

    @Test
    public void getRocketstatSamplePercent() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "800"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getRocketstatSamplePercent().intValue(), 25);
    }

    @Test
    public void getTimeout() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "007"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getTimeout().getValue(), 4);
        assertEquals(config.getTimeout().getTimeUnit(), TimeUnit.DAYS);
    }

    @Test
    public void getTimeoutProblem1() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "007"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getTimeoutProblem1().getValue(), 4);
        assertEquals(config.getTimeoutProblem1().getTimeUnit(), TimeUnit.SECONDS);
    }

    @Test
    public void getTimeoutProblem2() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "007"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getTimeoutProblem2().getValue(), 16);
        assertEquals(config.getTimeoutProblem2().getTimeUnit(), TimeUnit.HOURS);
    }

    @Test
    public void getCacheSize() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "009"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getCacheSize().getValue(), 2);
        assertEquals(config.getCacheSize().getByteUnit(), ByteUnit.Mebibyte);
    }

    @Test
    public void getBufferSize() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "008"));
        App2Config config = ConfigManager.INSTANCE.getConfig(App2Config.class, context);
        assertEquals(config.getBufferSize().getValue(), 3);
        assertEquals(config.getBufferSize().getByteUnit(), ByteUnit.Mebibyte);
    }

    @Config(description = "App2ConfigMissingProps common config object example 2")
    @ConfigResource(name = "app2MissingKeyWithoutDefault.json")
    public static final class App2ConfigMissingProps extends App2Config {

    }

    @Test(
            expectedExceptions = KeyNotFound.class,
            expectedExceptionsMessageRegExp = ".*key BufferSize is required and is currently missing.*")
    public void getBufferSizeMissing() {
        ConfigContext context = new ConfigContext(new Entry("SUBSET1", "505"));
        ConfigManager.INSTANCE.getConfig(App2ConfigMissingProps.class, context);
    }

    /**
     * Verify that EMPTY context throws runtime exception when it fails to find
     * certain key
     */
    @Test(expectedExceptions = ConfigRuntimeException.class, expectedExceptionsMessageRegExp=".*SUBSET1 not found.*")
    public void testCachedConfigInstance() {
        ConfigContext context2 = ConfigContext.EMPTY;
        @SuppressWarnings("unused")
        App2Config config2 = ConfigManager.INSTANCE.getConfig(App2Config.class, context2);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void returnsSingletonInstance() {
        ConfigContext context1 = new ConfigContext(new Entry("SUBSET1", "706"));
        App2Config c1 = ConfigManager.INSTANCE.getConfig(App2Config.class, context1);
        App2Config c2 = ConfigManager.INSTANCE.getConfig(App2Config.class, context1);

        assertSame(c1, c2);
    }

    @Test
    public void contextWithSameKeyValues() {
        ConfigContext context1 = new ConfigContext(new Entry("SUBSET1", "706"));
        assertEquals(context1.getUniqueId(), "SUBSET1706");
        ConfigContext context2 = new ConfigContext(new Entry("SUBSET1", "800"));
        App2Config c1 = ConfigManager.INSTANCE.getConfig(App2Config.class, context1);
        App2Config c2 = ConfigManager.INSTANCE.getConfig(App2Config.class, context2);

        Assert.assertNotSame(c1, c2);
    }

    @Test
    public void returnDifferentConfigInstancesForDifferentContexts() {
        ConfigContext context1 = new ConfigContext(new Entry("SUBSET1", "706"), new Entry("A", "705"));
        assertEquals(context1.getUniqueId(), "A705SUBSET1706");
        ConfigContext context2 = new ConfigContext(new Entry("SUBSET1", "705"), new Entry("A", "705"));
        App2Config c1 = ConfigManager.INSTANCE.getConfig(App2Config.class, context1);
        App2Config c2 = ConfigManager.INSTANCE.getConfig(App2Config.class, context2);
        App2Config c11 = ConfigManager.INSTANCE.getConfig(App2Config.class, context1);
        App2Config c22 = ConfigManager.INSTANCE.getConfig(App2Config.class, context2);

        assertSame(c1, c11);
        assertSame(c2, c22);
        Assert.assertNotSame(c1, c2);
    }
}
