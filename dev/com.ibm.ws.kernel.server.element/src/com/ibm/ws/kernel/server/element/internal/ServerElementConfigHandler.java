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
package com.ibm.ws.kernel.server.element.internal;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.server.element.ServerElement;

/**
 * DS Component that receives configuration for the server element.
 * This component is activated when the server element configuration is available
 * from ConfigAdmin, with defaults already applied from metatype.
 */
@Component(
    configurationPolicy = ConfigurationPolicy.OPTIONAL,
    configurationPid = "com.ibm.ws.kernel.server.element",
    service = {},
    immediate = true
)
public class ServerElementConfigHandler {
    
    private static final TraceComponent tc = Tr.register(ServerElementConfigHandler.class);
    
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerElementConfigHandler activated with properties: " + properties);
        }
        updateConfiguration(properties);
    }
    
    @Modified
    protected void modified(Map<String, Object> properties) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerElementConfigHandler modified with properties: " + properties);
        }
        updateConfiguration(properties);
    }
    
    private void updateConfiguration(Map<String, Object> properties) {
        // Check if beta mode is enabled
        boolean isBeta = Boolean.valueOf(System.getProperty("com.ibm.ws.beta.edition"));
        
        if (!isBeta) {
            // Not in beta mode, use default
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Not in beta mode, using default quiesce timeout");
            }
            ServerElement.setDefaultQuiesceTimeout();
            return;
        }
        
        // Beta mode - process quiesceTimeout if present
        Object timeoutObj = properties.get("quiesceTimeout");
        
        if (timeoutObj != null) {
            // Liberty's config system provides duration values as Long (in the specified unit)
            // The metatype specifies ibm:type="duration(s)" so the value comes as seconds
            try {
                Long timeoutSeconds = null;
                
                if (timeoutObj instanceof Long) {
                    timeoutSeconds = (Long) timeoutObj;
                } else if (timeoutObj instanceof Integer) {
                    timeoutSeconds = ((Integer) timeoutObj).longValue();
                } else if (timeoutObj instanceof String) {
                    // If it comes as a String, parse it using KernelUtils
                    // This handles duration syntax like "1m30s", "45s", "90", etc.
                    timeoutSeconds = KernelUtils.evaluateDuration((String) timeoutObj, TimeUnit.SECONDS);
                }
                
                if (timeoutSeconds != null) {
                    boolean success = ServerElement.setQuiesceTimeout(timeoutSeconds.intValue());
                    if (!success) {
                        // Value was below minimum, warning already logged by setQuiesceTimeout
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Failed to set quiesce timeout to " + timeoutSeconds +
                                     " seconds (below minimum), using default");
                        }
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Could not parse quiesceTimeout value, using default");
                    }
                    ServerElement.setDefaultQuiesceTimeout();
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error processing quiesceTimeout: " + e.getMessage());
                }
                ServerElement.setDefaultQuiesceTimeout();
            }
        } else {
            // No quiesceTimeout configured, use default
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No quiesceTimeout configured, using default");
            }
            ServerElement.setDefaultQuiesceTimeout();
        }
    }
}

// Made with Bob
