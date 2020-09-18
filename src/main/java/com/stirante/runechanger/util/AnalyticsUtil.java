package com.stirante.runechanger.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.ChampionSelection;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.model.app.Version;
import ly.count.sdk.ConfigCore;
import ly.count.sdk.internal.CtxCore;
import ly.count.sdk.internal.DeviceCore;
import ly.count.sdk.internal.ModuleCrash;
import ly.count.sdk.internal.Params;
import ly.count.sdk.java.Config;
import ly.count.sdk.java.Countly;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AnalyticsUtil {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AnalyticsUtil.class);
    public static final String APP_KEY = "555ef1b803f476d2169f8bf18fed9ac41ce6bd91";
    public static final String DEV_APP_KEY = "962c6753cb1af60b13344f5100d9a30488f794ea";
    public static final String SERVER_URL = "https://stats.stirante.com";
    public static final String ANALYTICS_DIR = "analytics";

    private static final ReentrantLock initLock = new ReentrantLock();

    public static void main(String[] args) {
        SimplePreferences.load();
        init(true);
        onConsent(true);
        log.info("init done");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("causing crash");
        addCrashReport(new RuntimeException("test"), "An error occurred while testing", true);
        Countly.session().end();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("stop");
        Countly.stop(false);
    }

    public static void addCrashReport(Throwable t, String comment, boolean fatal) {
        if (!Countly.isInitialized()) {
            init(false);
        }
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            initLock.lock();
            String logs = "";
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            for (Logger logger : context.getLoggerList()) {
                for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext(); ) {
                    Appender<ILoggingEvent> appender = index.next();
                    if (appender instanceof FileAppender) {
                        try {
                            byte[] encoded =
                                    Files.readAllBytes(Paths.get(((FileAppender<ILoggingEvent>) appender).getFile()));
                            logs = new String(encoded);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            Map<String, String> segments = new HashMap<>();
            segments.put("commit", Version.INSTANCE.commitIdAbbrev);
            segments.put("experimental", String.valueOf(SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.EXPERIMENTAL_CHANNEL, false)));
            segments.put("CPU", CustomDevice.dev.getCpu());
            segments.put("GPU", CustomDevice.dev.getGpu());
            segments.put("RAM", String.valueOf(CustomDevice.dev.getRam()));
            Countly.session().addCrashReport(t, fatal, comment + ": " + t.getMessage(), segments, logs);
            initLock.unlock();
        });
    }

    @Subscribe(ClientEventListener.ChampionSelectionEvent.NAME)
    public static void onChampionSelect(ClientEventListener.ChampionSelectionEvent event) {
        if (!Countly.isInitialized()) {
            return;
        }
        if (event.getEventType() == ClientEventListener.WebSocketEventType.CREATE) {
            ChampionSelection championSelectionModule = RuneChanger.getInstance().getChampionSelectionModule();
            Countly.session()
                    .event("champion_select")
                    .addSegment("type", championSelectionModule.getGameMode().name())
                    .addSegment("draft", String.valueOf(championSelectionModule.isPositionSelector()))
                    .record();
        }
    }

    public static void onConsent(boolean consent) {
        initNoData();
        if (consent) {
            Countly.onConsent(Config.Feature.values());
        }
        else {
            Countly.onConsentRemoval(Config.Feature.values());
        }
    }

    private static void initNoData() {
        if (!DebugConsts.ENABLE_ANALYTICS_DEBUG && DebugConsts.isRunningFromIDE()) {
            return;
        }
        if (!Countly.isInitialized()) {
            Config config = (Config) new Config(SERVER_URL, DebugConsts.ENABLE_ANALYTICS_DEBUG ? DEV_APP_KEY : APP_KEY)
                    .enableFeatures(Config.Feature.Events, Config.Feature.Sessions, Config.Feature.CrashReporting, Config.Feature.UserProfiles)
                    .setDeviceIdStrategy(Config.DeviceIdStrategy.UUID)
                    .setRequiresConsent(true)
                    .overrideModule(Config.Feature.CrashReporting, ModuleCrash.class)
                    .setApplicationVersion(Version.INSTANCE.version + "@" + Version.INSTANCE.commitIdAbbrev);

            if (DebugConsts.ENABLE_ANALYTICS_DEBUG) {
                config
                        .setLoggingLevel(ConfigCore.LoggingLevel.DEBUG)
                        .enableTestMode();
            }

            File targetFolder = new File(PathUtils.getWorkingDirectory() + File.separator + ANALYTICS_DIR);
            targetFolder.mkdir();

            Countly.init(targetFolder, config);

            if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.ANALYTICS, false)) {
                Countly.onConsent(Config.Feature.values());
            }
            else {
                Countly.onConsentRemoval(Config.Feature.values());
            }

            EventBus.register(AnalyticsUtil.class);
        }
    }

    public static void init(boolean begin) {
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            initLock.lock();
            Hardware.HardwareInfo info = Hardware.getAllHardwareInfo();

            CustomDevice device = new CustomDevice();
            device
                    .setCpu(info.cpuName)
                    .setCpuSpeed(info.cpuSpeed)
                    .setGpu(info.gpuNames.length == 0 ? "none" : info.gpuNames[0])
                    .setRam(info.ram);

            for (Field field : SimplePreferences.SettingsKeys.class.getFields()) {
                String id;
                try {
                    id = (String) field.get(null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                device.put(id, SimplePreferences.getStringValue(id, ""));
            }

            DeviceCore.dev = device;

            initNoData();
            if (begin) {
                beginSession();
            }
            initLock.unlock();
        });
    }

    public static void beginSession() {
        if (Countly.isInitialized()) {
            Countly.session().begin();
        }
    }

    public static class CustomDevice extends DeviceCore {
        public static CustomDevice dev = new CustomDevice();

        private String cpu;
        private String gpu;
        private Long cpuSpeed;
        private Long ram;
        private Map<String, String> additional = new HashMap<>();

        private CustomDevice() {
            dev = this;
        }

        public String getCpu() {
            return cpu;
        }

        public CustomDevice setCpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public String getGpu() {
            return gpu;
        }

        public CustomDevice setGpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Long getCpuSpeed() {
            return cpuSpeed;
        }

        public CustomDevice setCpuSpeed(Long cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
            return this;
        }

        public Long getRam() {
            return ram;
        }

        public CustomDevice setRam(Long ram) {
            this.ram = ram;
            return this;
        }

        public CustomDevice put(String key, String value) {
            additional.put(key, value);
            return this;
        }

        public String get(String key) {
            return additional.get(key);
        }

        @Override
        public Params buildMetrics(final CtxCore sdkctx) {
            Params params = new Params();
            Params.Obj obj = params.obj("metrics")
                    .put("_os", getOS())
                    .put("_os_version", getOSVersion())
                    .put("_locale", getLocale())
                    .put("_app_version", sdkctx.getConfig().getApplicationVersion())
                    .put("cpu", getCpu())
                    .put("cpu_speed", getCpuSpeed())
                    .put("gpu", getGpu())
                    .put("ram", getRam());
            for (String key : additional.keySet()) {
                obj.put(key, additional.get(key));
            }
            obj.add();

            return params;
        }

    }
}
