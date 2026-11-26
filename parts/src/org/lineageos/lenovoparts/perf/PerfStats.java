/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf;

import android.content.Context;
import android.os.CpuUsageInfo;
import android.os.HardwarePropertiesManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/** CPU clusters, GPU, temperatures and memory, read once per refresh. */
final class PerfStats {

    /** SM8250 clusters: silver 0-3, gold 4-6, prime 7. */
    static final int[][] CLUSTERS = {{0, 1, 2, 3}, {4, 5, 6}, {7}};
    private static final int[] POLICIES = {0, 4, 7};

    static final class Cluster {
        long curKhz, minKhz, maxKhz;
        int loadPercent = -1;
    }

    final Cluster[] clusters = new Cluster[CLUSTERS.length];
    long gpuHz, gpuMaxHz;
    int gpuBusy = -1;
    float cpuTemp = Float.NaN, gpuTemp = Float.NaN, skinTemp = Float.NaN;
    long memTotalKb, memAvailKb, swapTotalKb, swapFreeKb;

    /** Previous per-core usage, to turn the cumulative counters into a load. */
    static final class Sampler {
        private final HardwarePropertiesManager mHpm;
        private long[] mActive;
        private long[] mTotal;

        Sampler(Context context) {
            mHpm = context.getSystemService(HardwarePropertiesManager.class);
        }

        PerfStats read() {
            PerfStats s = new PerfStats();
            for (int i = 0; i < CLUSTERS.length; i++) {
                Cluster c = new Cluster();
                String policy = "/sys/devices/system/cpu/cpufreq/policy" + POLICIES[i] + "/";
                c.curKhz = num(policy + "scaling_cur_freq");
                c.minKhz = num(policy + "scaling_min_freq");
                c.maxKhz = num(policy + "scaling_max_freq");
                s.clusters[i] = c;
            }
            readLoad(s);

            String kgsl = "/sys/class/kgsl/kgsl-3d0/";
            s.gpuHz = num(kgsl + "gpuclk");
            s.gpuMaxHz = num(kgsl + "max_gpuclk");
            String busy = str(kgsl + "gpu_busy_percentage");
            if (busy != null) {
                try {
                    s.gpuBusy = Integer.parseInt(busy.replace("%", "").trim());
                } catch (NumberFormatException e) {
                    // keep unknown
                }
            }

            readThermal(s);
            readMemory(s);
            return s;
        }

        private void readLoad(PerfStats s) {
            CpuUsageInfo[] usages;
            try {
                usages = mHpm != null ? mHpm.getCpuUsages() : null;
            } catch (SecurityException e) {
                usages = null;
            }
            if (usages == null || usages.length < 8) {
                return;
            }
            long[] active = new long[usages.length];
            long[] total = new long[usages.length];
            for (int i = 0; i < usages.length; i++) {
                if (usages[i] != null) {
                    active[i] = usages[i].getActive();
                    total[i] = usages[i].getTotal();
                }
            }
            if (mActive != null) {
                for (int c = 0; c < CLUSTERS.length; c++) {
                    long da = 0, dt = 0;
                    for (int cpu : CLUSTERS[c]) {
                        da += active[cpu] - mActive[cpu];
                        dt += total[cpu] - mTotal[cpu];
                    }
                    if (dt > 0) {
                        s.clusters[c].loadPercent = (int) Math.min(100, Math.round(100.0 * da / dt));
                    }
                }
            }
            mActive = active;
            mTotal = total;
        }
    }

    private static void readThermal(PerfStats s) {
        File[] zones = new File("/sys/class/thermal").listFiles(
                (dir, name) -> name.startsWith("thermal_zone"));
        if (zones == null) {
            return;
        }
        for (File zone : zones) {
            String type = str(zone + "/type");
            if (type == null) {
                continue;
            }
            long milli = num(zone + "/temp");
            if (milli == Long.MIN_VALUE || milli <= -40000 || milli > 150000) {
                continue;
            }
            float c = milli / 1000f;
            if (type.startsWith("cpu-") || type.startsWith("cpuss-")) {
                s.cpuTemp = Float.isNaN(s.cpuTemp) ? c : Math.max(s.cpuTemp, c);
            } else if (type.startsWith("gpuss-")) {
                s.gpuTemp = Float.isNaN(s.gpuTemp) ? c : Math.max(s.gpuTemp, c);
            } else if (type.startsWith("skin") || type.startsWith("quiet-therm")
                    || type.startsWith("xo-therm")) {
                s.skinTemp = Float.isNaN(s.skinTemp) ? c : Math.max(s.skinTemp, c);
            }
        }
    }

    private static void readMemory(PerfStats s) {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split("\\s+");
                if (f.length < 2) {
                    continue;
                }
                long kb = Long.parseLong(f[1]);
                switch (f[0]) {
                    case "MemTotal:": s.memTotalKb = kb; break;
                    case "MemAvailable:": s.memAvailKb = kb; break;
                    case "SwapTotal:": s.swapTotalKb = kb; break;
                    case "SwapFree:": s.swapFreeKb = kb; break;
                    default: break;
                }
            }
        } catch (IOException | NumberFormatException e) {
            // leave zeros
        }
    }

    private static String str(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line = r.readLine();
            return line != null ? line.trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static long num(String path) {
        String v = str(path);
        if (v == null) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }
}
