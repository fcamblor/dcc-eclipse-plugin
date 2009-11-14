/*
 * Copyright (c) 1998, Regents of the University of California
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, Berkeley nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. * 
 */
package com.googlecode.eclipse.plugin.directorycpcontainer;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * This is a simple logger that logs messages and stack traces to the Eclipse error log.
 * 
 * @author Frederic Camblor
 */
public class Logger {
    public final static String PLUGIN_ID = "com.googlecode.eclipse.plugin.directorycpcontainer";
    // logging severities
    public static final int OK = IStatus.OK;
    public static final int ERROR = IStatus.ERROR;
    public static final int CANCEL = IStatus.CANCEL;
    public static final int INFO = IStatus.INFO;
    public static final int WARNING = IStatus.WARNING;
    // reference to the Eclipse error log
    private static ILog log;
    
    /**
     * Get a reference to the Eclipse error log
     */
    static {
        log = Platform.getLog(Platform.getBundle(PLUGIN_ID));    
    }
    
    /**
     * Prints stack trace to Eclipse error log 
     */
    public static void log(int severity, Throwable e) {
        Status s = new Status(severity, PLUGIN_ID, IStatus.OK, e.getMessage(), e);
        log.log(s);
    }
    
    /**
     * Prints a message to the Eclipse error log
     */
    public static void log(int severity, String msg) {
        Status s = new Status(severity, PLUGIN_ID, IStatus.OK, msg, null);
        log.log(s);
    }

}
