/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf;

import android.content.Context;

import java.io.File;

import org.lineageos.lenovoparts.Sysfs;

/** CPU clusters, GPU, temperatures and memory, read once per refresh. */
public final class PerfStats {

    /** SM8250 clusters: silver 0-3, gold 4-6, prime 7. */
    public static final int[][] CLUSTERS = {{0, 1, 2, 3}, {4, 5, 6}, {7}};
    private static final int[] POLICIES = {0, 4, 7};

    public static final class Cluster {
        long curKhz, minKhz, maxKhz;
        public int loadPercent = -1;
    }

    public final Cluster[] clusters = new Cluster[CLUSTERS.length];
    long gpuHz, gpuMaxHz;
    public int gpuBusy = -1;
    float cpuTemp = Float.NaN, gpuTemp = Float.NaN, skinTemp = Float.NaN;
    public long memTotalKb, memAvailKb, swapTotalKb, swapFreeKb;

    /** Previous per-core idle time, to turn the cumulative counters into a load. */
    public static final class Sampler {
        private static final int CPUS = 8;
        private static final String KGSL = "/sys/class/kgsl/kgsl-3d0/";

        private static final Sysfs.Node GPU_BUSY = new Sysfs.Node(KGSL + "gpu_busy_percentage");
        private static final Sysfs.Node MEMINFO = new Sysfs.Node("/proc/meminfo");

        private final Sysfs.Node[] mOnline = new Sysfs.Node[CPUS];
        private final Sysfs.Node[] mIsolate = new Sysfs.Node[CPUS];
        private final Sysfs.Node[][] mIdle = new Sysfs.Node[CPUS][];
        private final long[] mIdleNow = new long[CPUS];
        private final boolean[] mOnlineNow = new boolean[CPUS];
        private long[] mIdlePrev;
        private long mTimePrev;
        private String[] mCpuZones;
        private String[] mGpuZones;
        private String[] mSkinZones;

        public Sampler(Context context) {
            for (int cpu = 0; cpu < CPUS; cpu++) {
                String dir = "/sys/devices/system/cpu/cpu" + cpu;
                mOnline[cpu] = new Sysfs.Node(dir + "/online");
                mIsolate[cpu] = new Sysfs.Node(dir + "/isolate");
                int states = 0;
                while (new File(dir + "/cpuidle/state" + states + "/time").exists()) {
                    states++;
                }
                mIdle[cpu] = new Sysfs.Node[states];
                for (int st = 0; st < states; st++) {
                    mIdle[cpu][st] = new Sysfs.Node(dir + "/cpuidle/state" + st + "/time");
                }
            }
        }

        /** Everything shown on the page. */
        public PerfStats read() {
            return read(true);
        }

        /**
         * With [full] false, only what the charts need: CPU and GPU load
         * and memory, without frequencies and temperatures.
         */
        public PerfStats read(boolean full) {
            PerfStats s = new PerfStats();
            for (int i = 0; i < CLUSTERS.length; i++) {
                Cluster c = new Cluster();
                if (full) {
                    String policy = "/sys/devices/system/cpu/cpufreq/policy" + POLICIES[i] + "/";
                    c.curKhz = num(policy + "scaling_cur_freq");
                    c.minKhz = num(policy + "scaling_min_freq");
                    c.maxKhz = num(policy + "scaling_max_freq");
                }
                s.clusters[i] = c;
            }
            readLoad(s);

            long busy = GPU_BUSY.readLong();
            s.gpuBusy = busy == Sysfs.NONE ? -1 : (int) busy;
            if (full) {
                s.gpuHz = num(KGSL + "gpuclk");
                s.gpuMaxHz = num(KGSL + "max_gpuclk");
                readThermal(s);
            }
            readMemory(s, MEMINFO);
            return s;
        }

        /*
         * Per-core load from the cpuidle residency: the time a core was not in
         * any idle state over the sampling interval. The Thermal HAL has no
         * CPU usage on AIDL, and apps may not read /proc/stat.
         */
        private void readLoad(PerfStats s) {
            long now = android.os.SystemClock.elapsedRealtimeNanos() / 1000;
            for (int cpu = 0; cpu < CPUS; cpu++) {
                // cpu0 has no online node. core_ctl isolates (parks) idle
                // cores; their idle counters stop as well.
                mOnlineNow[cpu] = mOnline[cpu].readLong() != 0
                        && mIsolate[cpu].readLong() != 1;
                long sum = 0;
                for (Sysfs.Node node : mIdle[cpu]) {
                    long t = node.readLong();
                    if (t != Sysfs.NONE) {
                        sum += t;
                    }
                }
                mIdleNow[cpu] = sum;
            }
            if (mIdlePrev != null) {
                long wall = now - mTimePrev;
                for (int c = 0; c < CLUSTERS.length; c++) {
                    long busy = 0;
                    for (int cpu : CLUSTERS[c]) {
                        if (!mOnlineNow[cpu]) {
                            continue; // an offline or isolated core is idle
                        }
                        long di = mIdleNow[cpu] - mIdlePrev[cpu];
                        busy += Math.max(0, wall - Math.min(wall, di));
                    }
                    if (wall > 0) {
                        s.clusters[c].loadPercent = (int) Math.min(100,
                                Math.round(100.0 * busy / (wall * CLUSTERS[c].length)));
                    }
                }
            } else {
                mIdlePrev = new long[CPUS];
            }
            System.arraycopy(mIdleNow, 0, mIdlePrev, 0, CPUS);
            mTimePrev = now;
        }

        /** The thermal zones of interest, looked up once by their type. */
        private void findZones() {
            java.util.List<String> cpu = new java.util.ArrayList<>();
            java.util.List<String> gpu = new java.util.ArrayList<>();
            java.util.List<String> skin = new java.util.ArrayList<>();
            File[] zones = new File("/sys/class/thermal").listFiles(
                    (dir, name) -> name.startsWith("thermal_zone"));
            if (zones != null) {
                for (File zone : zones) {
                    String type = Sysfs.readString(zone + "/type");
                    if (type == null) {
                        continue;
                    }
                    String temp = zone + "/temp";
                    if (type.startsWith("cpu-") || type.startsWith("cpuss-")) {
                        cpu.add(temp);
                    } else if (type.startsWith("gpuss-")) {
                        gpu.add(temp);
                    } else if (type.startsWith("skin") || type.startsWith("quiet-therm")
                            || type.startsWith("xo-therm")) {
                        skin.add(temp);
                    }
                }
            }
            mCpuZones = cpu.toArray(new String[0]);
            mGpuZones = gpu.toArray(new String[0]);
            mSkinZones = skin.toArray(new String[0]);
        }

        private void readThermal(PerfStats s) {
            if (mCpuZones == null) {
                findZones();
            }
            s.cpuTemp = maxTemp(mCpuZones);
            s.gpuTemp = maxTemp(mGpuZones);
            s.skinTemp = maxTemp(mSkinZones);
        }
    }

    private static float maxTemp(String[] zones) {
        float max = Float.NaN;
        for (String zone : zones) {
            long milli = Sysfs.readLong(zone);
            if (milli == Sysfs.NONE || milli <= -40000 || milli > 150000) {
                continue;
            }
            float c = milli / 1000f;
            max = Float.isNaN(max) ? c : Math.max(max, c);
        }
        return max;
    }

    /** MemTotal, MemAvailable, SwapTotal and SwapFree from /proc/meminfo. */
    private static void readMemory(PerfStats s, Sysfs.Node meminfo) {
        byte[] buf = Sysfs.buffer();
        int n = meminfo.read(buf);
        int line = 0;
        while (line < n) {
            int colon = line;
            while (colon < n && buf[colon] != ':' && buf[colon] != '\n') {
                colon++;
            }
            if (colon < n && buf[colon] == ':') {
                if (key(buf, line, colon, "MemTotal")) {
                    s.memTotalKb = Sysfs.parseLong(buf, colon + 1, n);
                } else if (key(buf, line, colon, "MemAvailable")) {
                    s.memAvailKb = Sysfs.parseLong(buf, colon + 1, n);
                } else if (key(buf, line, colon, "SwapTotal")) {
                    s.swapTotalKb = Sysfs.parseLong(buf, colon + 1, n);
                } else if (key(buf, line, colon, "SwapFree")) {
                    s.swapFreeKb = Sysfs.parseLong(buf, colon + 1, n);
                    return; // the last one needed
                }
            }
            while (line < n && buf[line] != '\n') {
                line++;
            }
            line++;
        }
    }

    private static boolean key(byte[] buf, int from, int to, String key) {
        if (to - from != key.length()) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            if (buf[from + i] != key.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long num(String path) {
        return Sysfs.readLong(path);
    }
}
