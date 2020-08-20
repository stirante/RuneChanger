package com.stirante.runechanger.util;


import com.google.gson.Gson;
import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.ClientEventListener;
import com.stirante.runechanger.model.app.Version;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import generated.LolGameflowGameflowPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static com.sun.jna.platform.win32.WinNT.PROCESS_QUERY_INFORMATION;
import static java.lang.Math.*;

public class PerformanceMonitor {
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);

    public static class EventType {
        public static final String RESOURCE_USAGE = "RESOURCE_USAGE";
        public static final String CHAMPION_SELECT_BEGIN = "CHAMPION_SELECT_BEGIN";
        public static final String GAME_BEGIN = "GAME_BEGIN";
        public static final String GAME_END = "GAME_END";
        public static final String GUI_SHOW = "GUI_SHOW";
        public static final String GUI_HIDE = "GUI_HIDE";
        public static final String OVERLAY_SHOW = "OVERLAY_SHOW";
        public static final String OVERLAY_HIDE = "OVERLAY_HIDE";
        public static final String CHAMPIONS_INIT_START = "CHAMPIONS_INIT_START";
        public static final String CHAMPIONS_INIT_END = "CHAMPIONS_INIT_END";
    }

    private static String perfmonHtml;

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final LinkedList<PerformanceEvent> events = new LinkedList<>();
    private static final ReentrantLock eventLock = new ReentrantLock();
    private static Future<?> future;
    private static final Timer timer = new Timer();

    private static long prevKernelSystemTime = -1L;
    private static long prevUserSystemTime = -1L;
    private static long prevKernelProcessTime = -1L;
    private static long prevUserProcessTime = -1L;

    private static double cpuUsage = 0;

    private static long lastCheck = -1L;
    private static long monitorStart = -1L;


    public static void main(String[] args) {
        final AtomicInteger counter = new AtomicInteger();
        start();

        for (int n = 0; n < 8; n++) {
            int finalN = n;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (20000 * Math.random()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pushEvent("THREAD_START", finalN);
                counter.addAndGet(1);
                for (int i = 0; i < 2_000_000_0; i++) {
                    double d = tan(atan(tan(atan(tan(atan(tan(atan(tan(atan(123456789.123456789))))))))));
                    cbrt(d);
                }
                pushEvent("THREAD_END", finalN);
                if (counter.decrementAndGet() == 0) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (counter.get() > 0) {
                        return;
                    }
                    stop();
                }
            }).start();
        }
    }

    public static boolean isRunning() {
        return running.get();
    }

    public static void start() {
        if (running.get()) {
            return;
        }
        if (perfmonHtml == null) {
            try (Scanner scanner = new Scanner(PerformanceMonitor.class.getResourceAsStream("/perfmon.html"), StandardCharsets.UTF_8
                    .name())) {
                perfmonHtml = scanner.useDelimiter("\\A").next();
            }
        }
        EventBus.register(PerformanceMonitor.class);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running.get()) {
                    pushEvent(EventType.RESOURCE_USAGE, new UsageBean());
                } else {
                    eventLock.lock();
                    Map<String, Object> outData = new HashMap<>();
                    outData.put("hardware", Hardware.getAllHardwareInfo());
                    outData.put("startTime", monitorStart);
                    outData.put("version", Version.INSTANCE.version);
                    outData.put("commit", Version.INSTANCE.commitId);
                    outData.put("branch", Version.INSTANCE.branch);
                    outData.put("events", new ArrayList<>(events));
                    String s = new Gson().toJson(outData);
                    new File("performance").mkdir();
                    File file = new File(
                            "performance/" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(monitorStart)) + ".html");
                    try {
                        Files.writeString(file.getAbsoluteFile().toPath(), perfmonHtml.replace("{{data}}", s));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    log.info("Saving performance monitor output in " + file.getAbsolutePath());
                    running.set(false);
                    prevKernelSystemTime = -1L;
                    prevUserSystemTime = -1L;
                    prevKernelProcessTime = -1L;
                    prevUserProcessTime = -1L;
                    cpuUsage = 0;
                    lastCheck = -1L;
                    monitorStart = -1L;
                    events.clear();
                    System.gc();
                    eventLock.unlock();
                    timer.cancel();
                    timer.purge();
                }
            }
        }, 0, 1000);
        running.set(true);
        monitorStart = System.currentTimeMillis();
    }

    public static void stop() {
        running.set(false);
        EventBus.unregister(PerformanceMonitor.class);
        try {
            if (future != null) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Subscribe(ClientEventListener.GamePhaseEvent.NAME)
    public void onGamePhase(ClientEventListener.GamePhaseEvent event) {
        if (event.getData() == LolGameflowGameflowPhase.GAMESTART) {
            pushEvent(EventType.GAME_BEGIN);
        }
        else if (event.getData() == LolGameflowGameflowPhase.ENDOFGAME) {
            pushEvent(EventType.GAME_END);
        }
    }

    @Subscribe(ClientEventListener.ChampionSelectionEvent.NAME)
    public void onChampionSelection(ClientEventListener.ChampionSelectionEvent event) {
        if (event.getEventType() == ClientEventListener.WebSocketEventType.CREATE) {
            pushEvent(EventType.CHAMPION_SELECT_BEGIN);
        }
    }

    public static void pushEvent(String type) {
        pushEvent(type, null);
    }

    public static void pushEvent(String type, Object additionalInfo) {
        if (!running.get()) {
            return;
        }
        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
            eventLock.lock();
            events.add(new PerformanceEvent(type, additionalInfo));
            eventLock.unlock();
        });
    }

    private static boolean isFirstCheck() {
        return lastCheck == -1L;
    }

    private static double getMemoryUsage() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024D;
    }

    private static double getCpuUsage() {
        if (!isFirstCheck() && (System.currentTimeMillis() - lastCheck) < 250) {
            return cpuUsage;
        }
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        WinNT.HANDLE h = Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_INFORMATION, false, pid);
        try {
            WinBase.FILETIME creationProcessTime = new WinBase.FILETIME();
            WinBase.FILETIME exitProcessTime = new WinBase.FILETIME();
            WinBase.FILETIME kernelProcessTime = new WinBase.FILETIME();
            WinBase.FILETIME userProcessTime = new WinBase.FILETIME();
            Kernel32.INSTANCE.GetProcessTimes(h, creationProcessTime, exitProcessTime, kernelProcessTime, userProcessTime);
            WinBase.FILETIME idleSystemTime = new WinBase.FILETIME();
            WinBase.FILETIME kernelSystemTime = new WinBase.FILETIME();
            WinBase.FILETIME userSystemTime = new WinBase.FILETIME();
            Kernel32.INSTANCE.GetSystemTimes(idleSystemTime, kernelSystemTime, userSystemTime);
            if (!isFirstCheck()) {
                long kernelSystemTimeDiff = kernelSystemTime.toDWordLong().longValue() - prevKernelSystemTime;
                long userSystemTimeDiff = userSystemTime.toDWordLong().longValue() - prevUserSystemTime;
                long kernelProcessTimeDiff = kernelProcessTime.toDWordLong().longValue() - prevKernelProcessTime;
                long userProcessTimeDiff = userProcessTime.toDWordLong().longValue() - prevUserProcessTime;

                long totalSys = userSystemTimeDiff + kernelSystemTimeDiff;
                long totalProc = userProcessTimeDiff + kernelProcessTimeDiff;
                if (totalSys > 0) {
                    cpuUsage = (100.0 * totalProc) / totalSys;
                }
            }
            prevKernelProcessTime = kernelProcessTime.toDWordLong().longValue();
            prevUserProcessTime = userProcessTime.toDWordLong().longValue();
            prevKernelSystemTime = kernelSystemTime.toDWordLong().longValue();
            prevUserSystemTime = userSystemTime.toDWordLong().longValue();
            lastCheck = System.currentTimeMillis();
        } finally {
            Kernel32Util.closeHandle(h);
        }
        return cpuUsage;
    }

    private static class PerformanceEvent {
        public final String type;
        public final Object additionalInfo;
        public final long time;

        public PerformanceEvent(String type, Object additionalInfo) {
            this.type = type;
            this.additionalInfo = additionalInfo;
            time = System.currentTimeMillis() - monitorStart;
        }
    }

    private static class UsageBean {
        public final double memoryUsage;
        public final double cpuUsage;

        private UsageBean() {
            cpuUsage = getCpuUsage();
            memoryUsage = getMemoryUsage();
        }
    }

}
