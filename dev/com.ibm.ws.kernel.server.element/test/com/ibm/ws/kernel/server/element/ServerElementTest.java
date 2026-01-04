/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.server.element;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for ServerElement
 */
public class ServerElementTest {

    @After
    public void tearDown() {
        // Reset to default after each test
        ServerElement.setDefaultQuiesceTimeout();
    }

    /**
     * Test that the default quiesce timeout is 30 seconds
     */
    @Test
    public void testDefaultQuiesceTimeout() {
        int timeout = ServerElement.getQuiesceTimeout();
        assertEquals("Default quiesce timeout should be 30 seconds", 30, timeout);
    }

    /**
     * Test setting minimum valid value (30 seconds)
     */
    @Test
    public void testSetMinimumValidQuiesceTimeout() {
        ServerElement.setQuiesceTimeout(30);
        int timeout = ServerElement.getQuiesceTimeout();
        assertEquals("Quiesce timeout should be 30 seconds", 30, timeout);
    }

    /**
     * Test setting a valid timeout value above minimum (45 seconds)
     */
    @Test
    public void testSetValidQuiesceTimeout() {
        ServerElement.setQuiesceTimeout(45);
        int timeout = ServerElement.getQuiesceTimeout();
        assertEquals("Quiesce timeout should be 45 seconds", 45, timeout);
    }

    /**
     * Test setting a large valid value (3600 seconds = 1 hour)
     */
    @Test
    public void testSetLargeValidQuiesceTimeout() {
        ServerElement.setQuiesceTimeout(3600); // 1 hour
        int timeout = ServerElement.getQuiesceTimeout();
        assertEquals("Quiesce timeout should be 3600 seconds", 3600, timeout);
    }

    /**
     * Test that values below minimum (29 seconds) are set to default (30 seconds)
     */
    @Test
    public void testSetBelowMinimumQuiesceTimeout() {
        ServerElement.setQuiesceTimeout(45); // Set to known value first
        ServerElement.setQuiesceTimeout(29); // Try to set below minimum - should fall back to default
        int timeout = ServerElement.getQuiesceTimeout();
        assertEquals("Below minimum timeout should fall back to default 30 seconds", 30, timeout);
    }

    /**
     * Test that negative and zero values are set to default (30 seconds)
     */
    @Test
    public void testSetNegativeOrZeroQuiesceTimeout() {
        // Test negative
        ServerElement.setQuiesceTimeout(45);
        ServerElement.setQuiesceTimeout(-5);
        assertEquals("Negative timeout should fall back to default 30 seconds", 30, ServerElement.getQuiesceTimeout());
        
        // Test zero
        ServerElement.setQuiesceTimeout(45);
        ServerElement.setQuiesceTimeout(0);
        assertEquals("Zero timeout should fall back to default 30 seconds", 30, ServerElement.getQuiesceTimeout());
    }

    /**
     * Test multiple sequential sets with valid values
     */
    @Test
    public void testMultipleSequentialSets() {
        ServerElement.setQuiesceTimeout(30);
        assertEquals("First set should be 30 seconds", 30, ServerElement.getQuiesceTimeout());
        
        ServerElement.setQuiesceTimeout(60);
        assertEquals("Second set should be 60 seconds", 60, ServerElement.getQuiesceTimeout());
        
        ServerElement.setQuiesceTimeout(90);
        assertEquals("Third set should be 90 seconds", 90, ServerElement.getQuiesceTimeout());
    }
}
