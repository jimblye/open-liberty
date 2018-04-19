/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.source;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.collector.manager.buffer.BufferManagerEMQHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.logging.RoutedMessage;
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.collector.CollectorJsonHelpers;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.ws.logging.internal.WsLogRecord;
import com.ibm.ws.logging.utils.LogFormatUtils;
import com.ibm.ws.logging.utils.SequenceNumber;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

public class LogSource implements Source, WsLogHandler {

    private static final TraceComponent tc = Tr.register(LogSource.class);

    private final String sourceName = "com.ibm.ws.logging.source.message";
    private final String location = "memory";
    private BufferManager bufferMgr = null;
    static Pattern messagePattern;
    private final SequenceNumber sequenceNumber = new SequenceNumber();
    public static final String LINE_SEPARATOR;

    static {
        messagePattern = Pattern.compile("^([A-Z][\\dA-Z]{3,4})(\\d{4})([A-Z])(:)");

        LINE_SEPARATOR = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

    //private final AtomicLong seq = new AtomicLong();

    protected void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        this.bufferMgr = null;
    }

    public BufferManager getBufferManager() {
        return bufferMgr;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public void publish(RoutedMessage routedMessage) {
        //Publish the message if it is not coming from a handler thread

        LogRecord logRecord = routedMessage.getLogRecord();
        if (logRecord != null && bufferMgr != null) {

            LogTraceData parsedMessage = parse(routedMessage);
            if (!BufferManagerEMQHelper.getEMQRemovedFlag() && extractMessage(routedMessage, logRecord).startsWith("CWWKF0011I")) {
                BufferManagerEMQHelper.removeEMQTrigger();
            }
            bufferMgr.add(parsedMessage);
        }

    }

    private String extractMessage(RoutedMessage routedMessage, LogRecord logRecord) {
        String messageVal = routedMessage.getFormattedVerboseMsg();
        if (messageVal == null) {
            messageVal = logRecord.getMessage();
        }
        return messageVal;
    }

    public LogTraceData parse(RoutedMessage routedMessage) {

        GenericData genData = new GenericData();
        LogRecord logRecord = routedMessage.getLogRecord();
        String messageVal = extractMessage(routedMessage, logRecord);

        long dateVal = logRecord.getMillis();
        genData.addPair(LogFieldConstants.IBM_DATETIME, dateVal);

        String messageIdVal = null;

        if (messageVal != null) {
            messageIdVal = parseMessageId(messageVal);
        }

        genData.addPair(LogFieldConstants.IBM_MESSAGEID, messageIdVal);

        int threadIdVal = (int) Thread.currentThread().getId();//logRecord.getThreadID();
        genData.addPair(LogFieldConstants.IBM_THREADID, threadIdVal);
        genData.addPair(LogFieldConstants.MODULE, logRecord.getLoggerName());
        genData.addPair(LogFieldConstants.SEVERITY, LogFormatUtils.mapLevelToType(logRecord));
        genData.addPair(LogFieldConstants.LOGLEVEL, LogFormatUtils.mapLevelToRawType(logRecord));
        genData.addPair(LogFieldConstants.IBM_METHODNAME, logRecord.getSourceMethodName());
        genData.addPair(LogFieldConstants.IBM_CLASSNAME, logRecord.getSourceClassName());

        genData.addPair(LogFieldConstants.LEVELVALUE, logRecord.getLevel().intValue());
        String threadName = Thread.currentThread().getName();
        genData.addPair(LogFieldConstants.THREADNAME, threadName);

        WsLogRecord wsLogRecord = getWsLogRecord(logRecord);

        if (wsLogRecord != null) {
            genData.addPair(LogFieldConstants.CORRELATION_ID, wsLogRecord.getCorrelationId());
            genData.addPair(LogFieldConstants.ORG, wsLogRecord.getOrganization());
            genData.addPair(LogFieldConstants.PRODUCT, wsLogRecord.getProduct());
            genData.addPair(LogFieldConstants.COMPONENT, wsLogRecord.getComponent());
        }

        if (logRecord instanceof WsLogRecord) {
            if (((WsLogRecord) logRecord).getExtensions() != null) {
                KeyValuePairList extensions = new KeyValuePairList(LogFieldConstants.EXTENSIONS_KVPL);
                Map<String, String> extMap = ((WsLogRecord) logRecord).getExtensions();
                for (Map.Entry<String, String> entry : extMap.entrySet()) {
                    CollectorJsonHelpers.handleExtensions(extensions, entry.getKey(), entry.getValue());
                }
                genData.addPairs(extensions);
            }
        }

        genData.addPair(LogFieldConstants.IBM_SEQUENCE, sequenceNumber.next(dateVal));
        //String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            if (stackTrace != null) {
                genData.addPair(LogFieldConstants.THROWABLE, stackTrace);
            }
            String s = thrown.getLocalizedMessage();
            if (s == null) {
                s = thrown.toString();
            }
            genData.addPair(LogFieldConstants.THROWABLE_LOCALIZED, s);
        }

        //JTSBTS need to look at this message vs formatted message
        genData.addPair(LogFieldConstants.MESSAGE, messageVal);
        genData.setLogRecordLevel(logRecord.getLevel());
        genData.setLoggerName(logRecord.getLoggerName());

        if (routedMessage.getFormattedMsg() != null) {
            genData.addPair(LogFieldConstants.FORMATTEDMSG, routedMessage.getFormattedMsg());
        }

        genData.setSourceType(sourceName);
        LogTraceData logData = new LogTraceData(genData);
        logData.setLevelValue(logRecord.getLevel().intValue());
        logData.setLogLevel(LogFormatUtils.mapLevelToRawType(logRecord));

        return logData;

    }

    /* Overloaded method for test, should be removed down the line */
    public GenericData parse(RoutedMessage routedMessage, LogRecord logRecord) {

        GenericData genData = new GenericData();
        String messageVal = extractMessage(routedMessage, logRecord);

        long dateVal = logRecord.getMillis();
        genData.addPair(LogFieldConstants.IBM_DATETIME, dateVal);

        String messageIdVal = null;

        if (messageVal != null) {
            messageIdVal = parseMessageId(messageVal);
        }

        genData.addPair(LogFieldConstants.IBM_MESSAGEID, messageIdVal);

        int threadIdVal = (int) Thread.currentThread().getId();//logRecord.getThreadID();
        genData.addPair(LogFieldConstants.IBM_THREADID, threadIdVal);
        genData.addPair(LogFieldConstants.MODULE, logRecord.getLoggerName());
        genData.addPair(LogFieldConstants.SEVERITY, LogFormatUtils.mapLevelToType(logRecord));
        genData.addPair(LogFieldConstants.LOGLEVEL, LogFormatUtils.mapLevelToRawType(logRecord));
        genData.addPair(LogFieldConstants.IBM_METHODNAME, logRecord.getSourceMethodName());
        genData.addPair(LogFieldConstants.IBM_CLASSNAME, logRecord.getSourceClassName());

        if (logRecord instanceof WsLogRecord) {
            if (((WsLogRecord) logRecord).getExtensions() != null) {
                KeyValuePairList extensions = new KeyValuePairList(LogFieldConstants.EXTENSIONS_KVPL);
                Map<String, String> extMap = ((WsLogRecord) logRecord).getExtensions();
                for (Map.Entry<String, String> entry : extMap.entrySet()) {
                    CollectorJsonHelpers.handleExtensions(extensions, entry.getKey(), entry.getValue());
                }
                genData.addPairs(extensions);
            }
        }

        genData.addPair(LogFieldConstants.IBM_SEQUENCE, sequenceNumber.next(dateVal));
        //String sequence = date + "_" + String.format("%013X", seq.incrementAndGet());

        Throwable thrown = logRecord.getThrown();
        StringBuilder msgBldr = new StringBuilder();
        msgBldr.append(messageVal);
        if (thrown != null) {
            String stackTrace = DataFormatHelper.throwableToString(thrown);
            if (stackTrace != null) {
                msgBldr.append(LINE_SEPARATOR).append(stackTrace);
            }
        }
        genData.addPair(LogFieldConstants.MESSAGE, msgBldr.toString());
        genData.setSourceType(sourceName);

        return genData;
    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        String messageId = null;
        Matcher matcher = messagePattern.matcher(msg);
        if (matcher.find())
            messageId = msg.substring(matcher.start(), matcher.end() - 1);
        return messageId;
    }

    @FFDCIgnore(value = { ClassCastException.class })
    private WsLogRecord getWsLogRecord(LogRecord logRecord) {
        try {
            return (WsLogRecord) logRecord;
        } catch (ClassCastException ex) {
            return null;
        }
    }
}
