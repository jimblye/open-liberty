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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Configuration holder for server element attributes.
 * This class provides static methods to get and set configuration values
 * that are defined as attributes on the <server> element in server.xml.
 */
public class ServerElement {
    
    private static final TraceComponent tc = Tr.register(ServerElement.class);
    

    private static final int DEFAULT_QUIESCE_TIMEOUT_SECONDS = 30;

    private static final int MINIMUM_QUIESCE_TIMEOUT_SECONDS = 30;
    
    /**
     * Current quiesce timeout value in seconds.
     */
    private static int quiesceTimeout = DEFAULT_QUIESCE_TIMEOUT_SECONDS;
    
    /**
     * Get the current quiesce timeout value in seconds.
     *
     * @return the quiesce timeout in seconds
     */
    public static int getQuiesceTimeout() {
        int timeout = quiesceTimeout;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getQuiesceTimeout returning: " + timeout + " seconds");
        }
        return timeout;
    }
    
    /**
     * Set the quiesce timeout value in seconds.
     * The value is validated. If the value is invalid (below minimum),
     * the default value is set instead.
     *
     * @param timeoutSeconds the quiesce timeout in seconds
     * @return true if the requested value was valid and set, false if it was invalid (Sets default instead)
     */
    public static boolean setQuiesceTimeout(int timeoutSeconds) {
        if (timeoutSeconds >= MINIMUM_QUIESCE_TIMEOUT_SECONDS) {
            quiesceTimeout = timeoutSeconds;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "setQuiesceTimeout: quiesceTimeout set to " + timeoutSeconds + " seconds");
            }
            return true;
        }
        
        // Value not valid. Setting default
        quiesceTimeout = DEFAULT_QUIESCE_TIMEOUT_SECONDS;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setQuiesceTimeout: Invalid timeout value [{0}] seconds. Setting to default [{1}] seconds",
                         new Object[]{timeoutSeconds, DEFAULT_QUIESCE_TIMEOUT_SECONDS} );
                     
        }
        return false;
    }
    
    /**
     * Reset the quiesce timeout to the default value.
     * This is a convenience method that sets the timeout to the default value.
     */
    public static void setDefaultQuiesceTimeout() {
        setQuiesceTimeout(DEFAULT_QUIESCE_TIMEOUT_SECONDS);
    }
}
