package com.stirante.runechanger.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Hardware {
    private static final Logger log = LoggerFactory.getLogger(Hardware.class);

    private static <T> List<T> executeCommand(String cmd, Class<T> clz, boolean csv) {
        InputStream in = null;
        Process process = null;
        List<T> result = new ArrayList<>();
        try {
            process = Runtime.getRuntime().exec("wmic /locale:MS_409 " + cmd + (csv ? " /format:csv" : ""));
            in = process.getInputStream();
            Scanner sc = new Scanner(in);
            boolean hadHeader = false;
            ArrayList<Integer> indexes = new ArrayList<>();
            while (sc.hasNextLine()) {
                String s = sc.nextLine();
                if (s.isBlank()) {
                    continue;
                }
                if (csv) {
                    if (!hadHeader) {
                        hadHeader = true;
                        continue;
                    }
                    result.add(fromCsvLine(s, clz));
                }
                else {
                    StringBuilder sb = new StringBuilder();
                    if (!hadHeader) {
                        boolean adding = true;
                        indexes.add(0);
                        for (int i = 0; i < s.length(); i++) {
                            if (adding && Character.isWhitespace(s.charAt(i))) {
                                adding = false;
                            }
                            else if (!adding && !Character.isWhitespace(s.charAt(i))) {
                                adding = true;
                                indexes.add(i);
                            }
                        }
                        hadHeader = true;
                        continue;
                    }
                    for (int i = 0; i < indexes.size(); i++) {
                        if (i + 1 < indexes.size()) {
                            sb.append(s.substring(indexes.get(i), indexes.get(i + 1)).trim().replaceAll(",", ";"))
                                    .append(",");
                        }
                    }
                    result.add(fromCsvLine(sb.toString(), clz));
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred while getting cpu info", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while getting cpu info", false);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    private static <T> T fromCsvLine(String line, Class<T> clz) {
        try {
            T result = clz.getConstructor().newInstance();
            String[] split = line.split(",");
            Field[] declaredFields = clz.getDeclaredFields();
            setCsvFields(declaredFields, split, result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void setCsvFields(Field[] classFields, String[] csvFields, Object model) throws Exception {
        for (int i = 0; i < Math.min(csvFields.length, classFields.length); i++) {
            Field field = classFields[i];
            field.setAccessible(true);
            String s = csvFields[i];
            if (field.getType() == String.class) {
                field.set(model, s);
            }
            else if (field.getType() == int.class) {
                if (!s.isEmpty()) {
                    try {
                        field.set(model, Integer.parseInt(s));
                    } catch (NumberFormatException e) {
                        field.set(model, -1L);
                    }
                }
            }
            else if (field.getType() == long.class) {
                if (!s.isEmpty()) {
                    try {
                        field.set(model, Long.parseLong(s));
                    } catch (NumberFormatException e) {
                        field.set(model, -1L);
                    }
                }
            }
            else if (field.getType() == boolean.class) {
                if (!s.isEmpty()) {
                    try {
                        field.set(model, Boolean.parseBoolean(s));
                    } catch (NumberFormatException e) {
                        field.set(model, false);
                    }
                }
            }
        }
    }

    public static class HardwareInfo {
        public String cpuName;
        public long cpuSpeed;
        public long ram;
        public String[] gpuNames;
    }

    public static HardwareInfo getAllHardwareInfo() {
        HardwareInfo info = new HardwareInfo();
        CpuInfo cpuInfo = getCpuInfo();
        if (cpuInfo != null) {
            info.cpuName = cpuInfo.Name.replaceAll("\\([rR]\\)", "®").replaceAll("\\([tT][mM]\\)", "™");
            info.cpuSpeed = cpuInfo.CurrentClockSpeed;
        }
        List<MemoryInfo> memInfo = getMemoryInfo();
        info.ram = memInfo.stream().mapToLong(memoryInfo -> memoryInfo.Capacity / (1024 * 1024)).sum();
        List<GpuInfo> gpuInfo = getGpuInfo();
        info.gpuNames = new String[gpuInfo.size()];
        for (int i = 0; i < gpuInfo.size(); i++) {
            GpuInfo gpu = gpuInfo.get(i);
            info.gpuNames[i] = gpu.Name;
        }
        return info;
    }

    public static class CpuInfo {
        public String Node;
        public int AddressWidth;
        public int Architecture;
        public int Availability;
        public String Caption;
        public long ConfigManagerErrorCode;
        public boolean ConfigManagerUserConfig;
        public int CpuStatus;
        public String CreationClassName;
        public long CurrentClockSpeed;
        public int CurrentVoltage;
        public int DataWidth;
        public String Description;
        public String DeviceID;
        public boolean ErrorCleared;
        public String ErrorDescription;
        public long ExtClock;
        public int Family;
        public String InstallDate;
        public long L2CacheSize;
        public long L2CacheSpeed;
        public long LastErrorCode;
        public int Level;
        public int LoadPercentage;
        public String Manufacturer;
        public long MaxClockSpeed;
        public String Name;
        public String OtherFamilyDescription;
        public String PNPDeviceID;
        public String PowerManagementCapabilities;
        public boolean PowerManagementSupported;
        public String ProcessorId;
        public int ProcessorType;
        public int Revision;
        public String Role;
        public String SocketDesignation;
        public String Status;
        public int StatusInfo;
        public String Stepping;
        public String SystemCreationClassName;
        public String SystemName;
        public String UniqueId;
        public int UpgradeMethod;
        public String Version;
        public long VoltageCaps;
    }

    public static CpuInfo getCpuInfo() {
        return executeCommand("cpu list", CpuInfo.class, true).stream().findFirst().orElse(null);
    }

    public static class MemoryInfo {
        public String Node;
        public String BankLabel;
        public long Capacity;
        public short DataWidth;
        public String Description;
        public String DeviceLocator;
        public short FormFactor;
        public boolean HotSwappable;
        public String InstallDate;
        public short InterleaveDataDepth;
        public int InterleavePosition;
        public String Manufacturer;
        public short MemoryType;
        public String Model;
        public String Name;
        public String OtherIdentifyingInfo;
        public String PartNumber;
        public int PositionInRow;
        public boolean PoweredOn;
        public boolean Removable;
        public boolean Replaceable;
        public String SerialNumber;
        public String SKU;
        public int Speed;
        public String Status;
        public String Tag;
        public short TotalWidth;
        public short TypeDetail;
        public String Version;
    }

    public static List<MemoryInfo> getMemoryInfo() {
        return executeCommand("memorychip list", MemoryInfo.class, true);
    }

    public static class GpuInfo {
        public String AcceleratorCapabilities;
        public String AdapterCompatibility;
        public String AdapterDACType;
        public long AdapterRAM;
        public short Availability;
        public String CapabilityDescriptions;
        public String Caption;
        public int ColorTableEntries;
        public int ConfigManagerErrorCode;
        public boolean ConfigManagerUserConfig;
        public String CreationClassName;
        public int CurrentBitsPerPixel;
        public int CurrentHorizontalResolution;
        public long CurrentNumberOfColors;
        public int CurrentNumberOfColumns;
        public int CurrentNumberOfRows;
        public int CurrentRefreshRate;
        public short CurrentScanMode;
        public int CurrentVerticalResolution;
        public String Description;
        public String DeviceID;
        public int DeviceSpecificPens;
        public int DitherType;
        public String DriverDate;
        public String DriverVersion;
        public boolean ErrorCleared;
        public String ErrorDescription;
        public int ICMIntent;
        public int ICMMethod;
        public String InfFilename;
        public String InfSection;
        public String InstallDate;
        public String InstalledDisplayDrivers;
        public int LastErrorCode;
        public int MaxMemorySupported;
        public int MaxNumberControlled;
        public int MaxRefreshRate;
        public int MinRefreshRate;
        public boolean Monochrome;
        public String Name;
        public short NumberOfColorPlanes;
        public int NumberOfVideoPages;
        public String PNPDeviceID;
        public String PowerManagementCapabilities;
        public boolean PowerManagementSupported;
        public short ProtocolSupported;
        public int ReservedSystemPaletteEntries;
        public int SpecificationVersion;
        public String Status;
        public short StatusInfo;
        public String SystemCreationClassName;
        public String SystemName;
        public short SystemPaletteEntries;
        public String TimeOfLastReset;
        public short VideoArchitecture;
        public short VideoMemoryType;
        public short VideoMode;
        public String VideoModeDescription;
        public String VideoProcessor;
    }

    public static List<Hardware.GpuInfo> getGpuInfo() {
        return executeCommand("path win32_VideoController", Hardware.GpuInfo.class, false);
    }

}
