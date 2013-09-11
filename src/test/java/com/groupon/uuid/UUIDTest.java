/*
Copyright (c) 2013, Groupon, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

Neither the name of GROUPON nor the names of its contributors may be
used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.groupon.uuid;

import org.junit.Test;

import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class UUIDTest {
    @Test
    public void testStructure() {
        UUID id = new UUID();
        String str = id.toString();

        assertEquals(str.charAt(8) , '-');
        assertEquals(str.charAt(13), '-');
        assertEquals(str.charAt(14), 'b');
        assertEquals(str.charAt(18), '-');
        assertEquals(str.charAt(23), '-');
        assertEquals(str.length(), 36);
    }

    @Test
    public void testParsing() {
        UUID id1 = new UUID();
        UUID id2 = new UUID(id1.toString());
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());

        UUID id3 = new UUID(id1.getBytes());
        assertEquals(id1, id3);
    }

    @Test
    public void testSequentialToggle() {
        final int n = 1000;
        String[] ids = new String[n];

        for (int i = 0; i < n; i++) {
            ids[i] = new UUID().toString();
        }

        for (int i = 1; i < n; i++) {
            assertTrue(ids[i-1].charAt(0) != ids[i].charAt(0));
        }

        UUID.useSequentialIds();
        String initialCounter = new UUID().toString().split("-")[0];
        UUID.useVariableIds();
        UUID.useSequentialIds();
        String secondCounter = new UUID().toString().split("-")[0];
        assertEquals(initialCounter, secondCounter);

        for (int i = 0; i < n; i++) {
            ids[i] = new UUID().toString();
        }

        for (int i = 1; i < n; i++) {
            char a = ids[i-1].charAt(7);
            char b = ids[i].charAt(7);

            assertTrue((a + 1 == b) ||
                    (a == '9' && b == 'a') || (a == 'f' && b == '0'));
            assertEquals(ids[i-1].charAt(0), ids[i].charAt(0));
            assertEquals(ids[i-1].charAt(1), ids[i].charAt(1));
            assertEquals(ids[i-1].charAt(2), ids[i].charAt(2));
        }

        UUID.useVariableIds();

        for (int i = 0; i < n; i++)
            ids[i] = new UUID().toString();

        for (int i = 1; i < n; i++) {
            assertTrue(ids[i-1].charAt(0) != ids[i].charAt(0));
        }
    }

    @Test
    public void testGetBytesImmutability() {
        UUID id = new UUID();
        byte[] bytes = id.getBytes();
        byte[] original = Arrays.copyOf(bytes, bytes.length);
        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;

        assertTrue(Arrays.equals(id.getBytes(), original));
    }

    @Test
    public void testConstructorImmutability() {
        UUID id = new UUID();
        byte[] bytes = id.getBytes();
        byte[] original = Arrays.copyOf(bytes, bytes.length);

        UUID id2 = new UUID(bytes);
        bytes[0] = 0;
        bytes[1] = 0;

        assertTrue(Arrays.equals(id2.getBytes(), original));
    }

    @Test
    public void testVersionField() {
        UUID generated = new UUID();
        assertEquals('b', generated.getVersion());

        UUID parsed1 = new UUID("20be0ffc-314a-bd53-7a50-013a65ca76d2");
        assertEquals('b', parsed1.getVersion());

        UUID parsed2 = new UUID("20be0ffc-314a-7d53-7a50-013a65ca76d2");
        assertEquals('7', parsed2.getVersion());
        assertEquals(-1, parsed2.getProcessId());
        assertNull(parsed2.getTimestamp());
        assertNull(parsed2.getMacFragment());
    }

    @Test
    public void testPIDField() throws Exception {
        UUID id = new UUID();

        assertEquals(UUID.PID, id.getProcessId());
    }

    @Test
    public void testDateField() {
        UUID id = new UUID();
        assertTrue(id.getTimestamp().getTime() > new Date().getTime() - 100);
        assertTrue(id.getTimestamp().getTime() < new Date().getTime() + 100);
    }

    private static byte[] macAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            byte[] mac = null;
            while (interfaces.hasMoreElements() && mac != null && mac.length != 6) {
                NetworkInterface netInterface = interfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() ) { continue; }
                mac = netInterface.getHardwareAddress();
            }

            // if the machine is not connected to a network it has no active MAC address
            if (mac == null)
                mac = new byte[] {0, 0, 0, 0, 0, 0};

            return mac;
        } catch (Exception e) {
            throw new RuntimeException("Could not get MAC address");
        }
    }

    @Test
    public void testMacAddressField() throws Exception{
        UUID id = new UUID();

        byte[] mac = macAddress();

        // if the machine is not connected to a network it has no active MAC address
        if (mac == null)
            mac = new byte[] {0, 0, 0, 0, 0, 0};

        byte[] field = id.getMacFragment();
        assertEquals(0, field[0]);
        assertEquals(0, field[1]);
        assertEquals(mac[2] & 0xF, field[2]);
        assertEquals(mac[3], field[3]);
        assertEquals(mac[4], field[4]);
        assertEquals(mac[5], field[5]);
    }

    @Test
    public void testForDuplicates() {
        int n = 1000000;
        Set<UUID> uuids = new HashSet<UUID>();
        UUID[] uuidArray = new UUID[n];

        for (int i = 0; i < n; i++)
            uuidArray[i] = new UUID();

        for (int i = 0; i < n; i++)
            uuids.add(uuidArray[i]);

        assertEquals(n, uuids.size());
    }

    private class Generator extends Thread {
        private UUID[] uuids;
        int id;
        int n;
        int numThreads;

        public Generator(int n, UUID[] uuids, int id, int numThreads) {
            this.n = n;
            this.uuids = uuids;
            this.id = id;
            this.numThreads = numThreads;
        }

        @Override
        public void run() {
            for (int i = 0; i < n; i++) {
                uuids[numThreads * i + id] = new UUID();
            }
        }
    }

    @Test
    public void concurrentGeneration() throws Exception {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        int n = 1000000;
        UUID[] uuids = new UUID[n];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Generator(n / numThreads, uuids, i, numThreads);
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++)
            threads[i].join();

        Set<UUID> uuidSet = new HashSet<UUID>();

        int effectiveN = n / numThreads * numThreads;
        for (int i = 0; i < effectiveN; i++)
            uuidSet.add(uuids[i]);

        assertEquals(effectiveN, uuidSet.size());
    }
}
