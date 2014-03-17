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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NetworkInterface.class})
public class UUIDTest {

    /**
     * Ensure basic UUID structure is correct.
     */
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
        assertTrue(UUID.isValidUUID(str));
    }

    /**
     * Test that UUIDs can be correctly generated from a valid UUID String.
     */
    @Test
    public void testParsing() {
        UUID id1 = new UUID();
        UUID id2 = new UUID(id1.toString());
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());

        UUID id3 = new UUID(id1.getBytes());
        assertEquals(id1, id3);

        UUID id4 = new UUID("aAa0BBb1-CCc2-DDd3-EEe4-FFf567891234");
        assertEquals("aaa0bbb1-ccc2-ddd3-eee4-fff567891234", id4.toString());
    }

    /**
     * Test that a UUID object can be created using a java.util.UUID.
     */
    @Test
    public void testJavaUuidConstructor() {
        java.util.UUID id1 = java.util.UUID.randomUUID();
        UUID id2 = new UUID(id1);
        assertEquals(id1.toString(), id2.toString());
    }

    /**
     * Test that a UUID object can be created using hi and lo long segments.
     */
    @Test
    public void testHiLoConstructor() {
        java.util.UUID id1 = java.util.UUID.randomUUID();
        UUID id2 = new UUID(id1.getMostSignificantBits(), id1.getLeastSignificantBits());
        assertEquals(id1.toString(), id2.toString());
    }

    /**
     * Test basic functionality from the UUID string validator.
     */
    @Test
    public void testValidator() {
        assertEquals(false, UUID.isValidUUID("aoeu"));
        assertEquals(false, UUID.isValidUUID("aaaabbbbccccddddffffaaaabbb"));
        assertEquals(false, UUID.isValidUUID("00000000-0000-0000-0000-00000000000h"));
        assertEquals(true,  UUID.isValidUUID("00000000-0000-0000-0000-000000000000"));
        assertEquals(false, UUID.isValidUUID("00000000-0000-0000-0000_000000000000"));
        assertEquals(true,  UUID.isValidUUID("abcdef00-AAAA-BBBB-0000-abcDD1234123"));
    }

    /**
     * Test that we can generate a correct java.util.UUID given a UUID.
     */
    @Test
    public void testToJavaUUID() {
        for (int i = 0; i < 1000; i++) {
            UUID id1 = new UUID();
            java.util.UUID id2 = id1.toJavaUUID();
            assertEquals(id1.toString(), id2.toString());
            assertEquals(id1.getMostSignificantBits(), id2.getMostSignificantBits());
            assertEquals(id2.getLeastSignificantBits(), id2.getLeastSignificantBits());
        }
    }

    /**
     * Test UUID generation when in sequential mode.
     */
    @Test
    public void testSequentialToggle() {
        final int n = 1000;
        String[] ids = new String[n];
        UUID.useVariableIds();

        // first verify that in variable mode we get a bit of variability in subsequent UUIDs
        for (int i = 0; i < n; i++)
            ids[i] = new UUID().toString();
        for (int i = 1; i < n; i++)
            assertTrue(ids[i-1].charAt(0) != ids[i].charAt(0));

        // toggle into sequential mode and get counter value
        UUID.useSequentialIds();
        String counter1 = new UUID().toString().split("-")[0];

        // toggle again and get counter
        UUID.useVariableIds();
        UUID.useSequentialIds();
        String counter2 = new UUID().toString().split("-")[0];

        // toggle again and get counter
        UUID.useVariableIds();
        UUID.useSequentialIds();
        String counter3 = new UUID().toString().split("-")[0];

        // toggle again and get counter
        UUID.useVariableIds();
        UUID.useSequentialIds();
        String counter4 = new UUID().toString().split("-")[0];

        // test that at least 2 of the 3 sequential pairwise comparisons of counter values are the same
        // since these are time-based in 10 minute increments, its possible in rare cases that the wall clock could
        // change during this process, which is why its possible only 2 are the same, though this should be rare
        int equal = 0;
        equal += (counter1.equals(counter2) ? 1 : 0);
        equal += (counter2.equals(counter3) ? 1 : 0);
        equal += (counter3.equals(counter4) ? 1 : 0);
        assertTrue(equal >= 2);
    }

    @Test
    public void testSequentialGeneration() {
        UUID.useSequentialIds();
        final int n = 1000;
        String[] ids = new String[n];

        // generate n ids
        for (int i = 0; i < n; i++)
            ids[i] = new UUID().toString();

        // test that counter values are sequential in hex
        for (int i = 1; i < n; i++) {
            String prev = ids[i-1].substring(0, 8);
            String curr = ids[i].substring(0, 8);
            long prevValue = Long.parseLong(prev, 16) + 1;
            long currValue = Long.parseLong(curr, 16);
            assertEquals(prevValue, currValue);
        }

        // toggle back to normal mode
        UUID.useVariableIds();

        // generate n ids
        for (int i = 0; i < n; i++)
            ids[i] = new UUID().toString();

        // check that the most significant bit changes every time now
        for (int i = 1; i < n; i++)
            assertTrue(ids[i-1].charAt(0) != ids[i].charAt(0));
    }

    /**
     * Check that when we get the byte array with getBytes that changing it doesn't change the UUID.
     */
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

    /**
     * Check that when we construct a uuid with a byte array and that byte array later changes, that the UUID
     * does not change.
     */
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

    /**
     * Check that when we parse a UUID it returns the correct version and that if this version is not 'b' that the
     * vB specific methods reflect this.
     */
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

    /**
     * Test that the PID field is set
     * @throws Exception
     */
    @Test
    public void testPIDField() throws Exception {
        UUID id = new UUID();
        assertEquals(UUID.PID, id.getProcessId());
    }

    /**
     * Test that the date field is within a certain window of dates generated right after. Conceivably this could fail
     * if something crazy happens in the testing JVM right after the first line, but 100ms is a pretty big window.
     */
    @Test
    public void testDateField() {
        UUID id = new UUID();
        assertTrue(id.getTimestamp().getTime() > new Date().getTime() - 100);
        assertTrue(id.getTimestamp().getTime() < new Date().getTime() + 100);
    }

    /**
     * Exposes the MAC address that us used in the UUID via reflection.
     * @return the MAC address.
     */
    private static byte[] macAddress() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method macAddress = com.groupon.uuid.UUID.class.getDeclaredMethod("macAddress");
        macAddress.setAccessible(true);
        return (byte[])macAddress.invoke(new UUID());
    }

    /**
     * Test that the MAC address field is set and corresponds to the MAC found in a generated UUID.
     * @throws Exception
     */
    @Test
    public void testMacAddressField() throws Exception {
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

    /**
     * Generate a bunch of UUIDs and check to see if there are any dups. We use a Set to test this, since it checks
     * that each member is unique.
     */
    @Test
    public void testForDuplicates() {
        int n = 1000000;
        UUID[] uuidArray = new UUID[n];
        for (int i = 0; i < n; i++)
            uuidArray[i] = new UUID();

        Set<UUID> uuids = new HashSet<UUID>();
        Collections.addAll(uuids, uuidArray);
        assertEquals(n, uuids.size());
    }

    /**
     * This is a class to run a threaded UUID generator. It has a constructor, which tells in how many to generate and
     * where to place these, and a run() method which generates ids until its done.
     */
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

    /**
     * This is a shotgun test to see if generating ids in a threaded environment yields any duplicates.
     */
    @Test
    public void concurrentGeneration() throws Exception {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        int n = 1000000;
        UUID[] uuids = new UUID[n];

        // create a bunch of threads that generate uuids
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Generator(n / numThreads, uuids, i, numThreads);
            threads[i].start();
        }

        // wait for all the threads to finish
        for (int i = 0; i < numThreads; i++)
            threads[i].join();

        // put all ids in a set to check if there are dups
        Set<UUID> uuidSet = new HashSet<UUID>();
        int effectiveN = n / numThreads * numThreads;
        Collections.addAll(uuidSet, uuids);
        assertEquals(effectiveN, uuidSet.size());
    }
}
