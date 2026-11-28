/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Small sysfs and procfs reads into a per-thread buffer. The charts read
 * a few dozen nodes twice a second, so the numeric path allocates nothing
 * besides the stream itself.
 */
public final class Sysfs {

    public static final long NONE = Long.MIN_VALUE;

    private static final ThreadLocal<byte[]> BUFFER =
            ThreadLocal.withInitial(() -> new byte[4096]);

    private Sysfs() {
    }

    /** Reads up to the buffer size; returns the byte count or -1. */
    public static int read(String path, byte[] buf) {
        try (FileInputStream in = new FileInputStream(path)) {
            int total = 0;
            int n;
            while (total < buf.length && (n = in.read(buf, total, buf.length - total)) > 0) {
                total += n;
            }
            return total;
        } catch (IOException e) {
            return -1;
        }
    }

    /** The leading integer of a node ("42", "-7", "35 %"), or [NONE]. */
    public static long readLong(String path) {
        byte[] buf = BUFFER.get();
        int n = read(path, buf);
        return n <= 0 ? NONE : parseLong(buf, 0, n);
    }

    /** First line of a node, trimmed, or null. */
    public static String readString(String path) {
        byte[] buf = BUFFER.get();
        int n = read(path, buf);
        if (n < 0) {
            return null;
        }
        int end = 0;
        while (end < n && buf[end] != '\n') {
            end++;
        }
        return new String(buf, 0, end).trim();
    }

    /** Parses an integer starting at [from], skipping leading blanks; [NONE] if none. */
    public static long parseLong(byte[] buf, int from, int to) {
        int i = from;
        while (i < to && (buf[i] == ' ' || buf[i] == '\t')) {
            i++;
        }
        boolean negative = i < to && buf[i] == '-';
        if (negative) {
            i++;
        }
        int start = i;
        long v = 0;
        while (i < to && buf[i] >= '0' && buf[i] <= '9') {
            v = v * 10 + (buf[i] - '0');
            i++;
        }
        if (i == start) {
            return NONE;
        }
        return negative ? -v : v;
    }

    /**
     * A node read over and over: it stays open and is re-read from offset
     * 0, which saves the path lookup and permission checks of each open.
     */
    public static final class Node {
        private final String mPath;
        private FileDescriptor mFd;

        public Node(String path) {
            mPath = path;
        }

        /** Reads into [buf]; returns the byte count or -1. */
        public synchronized int read(byte[] buf) {
            try {
                if (mFd == null) {
                    mFd = Os.open(mPath, OsConstants.O_RDONLY | OsConstants.O_CLOEXEC, 0);
                }
                int total = 0;
                int n;
                while (total < buf.length
                        && (n = Os.pread(mFd, buf, total, buf.length - total, total)) > 0) {
                    total += n;
                }
                return total;
            } catch (ErrnoException | IOException e) {
                close();
                return -1;
            }
        }

        public long readLong() {
            byte[] buf = BUFFER.get();
            int n = read(buf);
            return n <= 0 ? NONE : parseLong(buf, 0, n);
        }

        private void close() {
            if (mFd != null) {
                try {
                    Os.close(mFd);
                } catch (ErrnoException e) {
                    // nothing to do
                }
                mFd = null;
            }
        }
    }

    /** The per-thread buffer, for callers that parse a whole file. */
    public static byte[] buffer() {
        return BUFFER.get();
    }
}
