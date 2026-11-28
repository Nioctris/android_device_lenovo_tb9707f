/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import org.lineageos.lenovoparts.Sysfs;

/** One reading of the charger, charge pumps and battery gauges. */
public final class PowerStats {

    private static final String PSY = "/sys/class/power_supply/";
    private static final Sysfs.Node CURRENT = new Sysfs.Node(PSY + "battery/current_now");
    private static final Sysfs.Node VOLTAGE = new Sysfs.Node(PSY + "battery/voltage_now");

    /* Battery (FG Gen4 through the PMIC charger) */
    String status;
    int capacity;
    long batteryUa;
    long batteryUv;
    int batteryTemp;

    /* Charger input */
    boolean usbPresent;
    String usbType;
    int pdActive;
    long vbusUv;
    long mainInputUa;

    /* Charge pumps */
    boolean pumpEnabled;
    long pumpMasterUa;
    long pumpSlaveUa;
    int pumpTemp;

    /* BQ27541 gauge */
    String health;
    int cycles;
    long chargeFullUah;
    long chargeDesignUah;

    /* Modes */
    boolean bypass;
    boolean limitBypass;

    public static PowerStats read() {
        PowerStats s = new PowerStats();
        s.status = str("battery/status");
        s.capacity = (int) num("battery/capacity");
        s.batteryUa = num("battery/current_now");
        s.batteryUv = num("battery/voltage_now");
        s.batteryTemp = (int) num("battery/temp");
        s.bypass = num("battery/bypass_charging") == 1;
        s.limitBypass = num("battery/battery_charging_enabled") == 0;

        s.usbPresent = num("usb/present") == 1;
        s.usbType = str("usb/real_type");
        s.pdActive = (int) num("usb/pd_active");
        s.vbusUv = num("usb/voltage_now");
        s.mainInputUa = num("usb/input_current_now");

        s.pumpEnabled = num("charge_pump_master/cp_enable") == 1;
        s.pumpMasterUa = num("charge_pump_master/cp_isns");
        s.pumpSlaveUa = num("charge_pump_master/cp_isns_slave");
        s.pumpTemp = (int) num("charge_pump_master/cp_die_temp");

        s.health = str("bq27541-0/health");
        s.cycles = (int) num("bq27541-0/cycle_count");
        s.chargeFullUah = num("bq27541-0/charge_full");
        s.chargeDesignUah = num("bq27541-0/charge_full_design");
        return s;
    }

    /** Battery power alone, for the background chart. */
    public static double readBatteryWatts() {
        long ua = CURRENT.readLong();
        long uv = VOLTAGE.readLong();
        return valid(ua) && valid(uv) ? ua * (double) uv / 1e12 : Double.NaN;
    }

    /** Battery power in watts, positive while charging. */
    public double batteryWatts() {
        return batteryUa * (double) batteryUv / 1e12;
    }

    /** Charge pump input current, both chips. */
    long pumpInputUa() {
        if (!pumpEnabled) {
            return 0;
        }
        return Math.max(pumpMasterUa, 0) + (valid(pumpSlaveUa) ? Math.max(pumpSlaveUa, 0) : 0);
    }

    /** Charger output power in watts: VBUS times the main and pump inputs. */
    double inputWatts() {
        return vbusUv * (double) (Math.max(mainInputUa, 0) + pumpInputUa()) / 1e12;
    }

    private static String str(String node) {
        return Sysfs.readString(PSY + node);
    }

    private static long num(String node) {
        return Sysfs.readLong(PSY + node);
    }

    static boolean valid(long v) {
        return v != Long.MIN_VALUE;
    }
}
