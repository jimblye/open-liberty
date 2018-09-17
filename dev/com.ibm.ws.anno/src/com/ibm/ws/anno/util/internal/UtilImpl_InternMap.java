/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.ws.anno.util.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.wsspi.anno.util.Util_InternMap;

public class UtilImpl_InternMap implements Util_InternMap {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.util");
    
    public static final String CLASS_NAME = "UtilImpl_InternMap";

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    public static final int DEFAULT_LOG_THRESHHOLD = 1024 * 4;

    protected final int logThreshHold;

    @Override
    public int getLogThreshHold() {
        return logThreshHold;
    }

    //

    // TODO: How does this factor across the factory/service implementation?
    /*
     * The validate method will return a Message Id as the string if a
     * validation error is found.
     *
     * see com.ibm.wsspi.anno.util.Util_InternMap.doValidate
     */

    public static String doValidate(String value, ValueType valueType) {
        String vMsg = null;

        switch ( valueType ) {
            case VT_CLASS_RESOURCE:
                if ( value.contains("\\") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if ( !value.endsWith(".class") ) {
                    vMsg = "ANNO_UTIL_EXPECTED_CLASS";
                }
                break;

            case VT_CLASS_REFERENCE:
                if ( value.contains("\\") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if ( value.endsWith(".class") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_CLASS_NAME:
                if ( value.contains("\\") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if ( value.contains("/") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if ( value.endsWith(".class") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_FIELD_NAME:
                if ( value.contains("\\") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if ( value.contains("/") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if ( value.endsWith(".class") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_METHOD_NAME:
                if ( value.contains("\\") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if ( value.contains("/") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if ( value.endsWith(".class") ) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_OTHER:
                break;

            default:
                vMsg = "ANNO_UTIL_UNRECOGNIZED_TYPE";
                break;
        }

        return vMsg;
    }

    @Override
    @Trivial
    public String validate(String useValue, ValueType useValueType) {
        return UtilImpl_InternMap.doValidate(useValue, useValueType);
    }

    //

    @Trivial
    public UtilImpl_InternMap(UtilImpl_Factory factory, ValueType valueType, String name) {
        this(factory, valueType, DEFAULT_LOG_THRESHHOLD, name);
    }

    public UtilImpl_InternMap(UtilImpl_Factory factory, ValueType valueType, int logThreshHold, String name) {
        super();

        String methodName = "<init>";
        
        this.factory = factory;

        this.logThreshHold = logThreshHold;

        this.name = name;

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
                        "(" + this.name + ")";

        this.valueType = valueType;
        this.checkValues = logger.isLoggable(Level.FINER);

        this.internMap = new HashMap<String, String>();
        this.lastReportedLength = 0;
        this.totalLength = 0;

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Value type [ {1} ] Log threshhold [ {2} ]",
                    new Object[] { this.hashText,
                                   this.valueType,
                                   Integer.valueOf(this.logThreshHold) });
        }
    }

    //

    protected final UtilImpl_Factory factory;

    @Override
    @Trivial
    public UtilImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final ValueType valueType;

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    protected boolean checkValues;

    public boolean getCheckValues() {
        return checkValues;
    }

    //

    protected final String name;

    @Override
    public String getName() {
        return name;
    }

    // Name intern map ...

    protected final Map<String, String> internMap;
    protected int lastReportedLength;
    protected int totalLength;

    protected Map<String, String> getInternMap() {
        return internMap;
    }

    protected synchronized int getLastReportedLength() {
        return lastReportedLength;
    }

    @Override
    public synchronized Collection<String> getValues() {
        return getInternMap().values();
    }

    @Override
    public synchronized int getSize() {
        return getInternMap().size();
    }

    @Override
    public synchronized int getTotalLength() {
        return totalLength;
    }

    @Override
    @Trivial
    public String intern(String useName) {
        return intern(useName, Util_InternMap.DO_FORCE);
    }

    @Override
    public synchronized String intern(String useName, boolean doForce) {
        String methodName = "intern";
        
        if ( useName == null ) {
            return useName;
        }

        if ( checkValues ) {
            // The validate method will return a Message Id as the string if a
            // validation error is found. The Message Id can then be passed to Tr.warning
            // along with the substitution parameters.
            String vMsg = validate(useName, valueType);
            if ( vMsg != null ) {
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        vMsg,
                        new Object[] { getHashText(), useName, valueType });
            }
        }

        String priorIntern = internMap.get(useName);
        if ( (priorIntern != null) || !doForce ) {
            return priorIntern;
        }

        priorIntern = useName;
        internMap.put(useName, useName);

        totalLength += useName.length();
        if ( (totalLength - lastReportedLength) > logThreshHold ) {
            lastReportedLength = totalLength;
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Total [ {1} ]",
                        new Object[] { hashText, Integer.valueOf(totalLength) });
            }
        }

        return priorIntern;
    }

    // Used to allow fast return lookups:
    //
    // For a lookup to an identity map that is backed by an intern map,
    // if the intern map lookup fails, the map lookup must return null.

    @Override
    public synchronized boolean contains(String useName) {
        return internMap.containsKey(useName);
    }

    //

    @Override
    @Trivial
    public synchronized void log(Logger useLogger) {
        String methodName = "log";
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Intern Map: BEGIN [ {0} ]:", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Log threshhold[ {0} ]", Integer.valueOf(getLogThreshHold()));

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Size [ {0} ]", Integer.valueOf(getSize()));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Total Length [ {0} ]", Integer.valueOf(getTotalLength()));

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Intern Map: END [ {0} ]:", getHashText());
    }
}
