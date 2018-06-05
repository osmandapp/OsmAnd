/*
 * This file is part of TD.
 *
 * TD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * TD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TD.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2014-2015 Arseny Smirnov
 *           2014-2015 Aliaksei Levin
 */

package org.drinkless.td.libcore.telegram;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main class for interaction with the TDLib.
 */
public class Client implements Runnable {
    /**
     * Interface for handler for results of queries to TDLib and incoming updates from TDLib.
     */
    public interface ResultHandler {
        /**
         * Callback called on result of query to TDLib or incoming update from TDLib.
         *
         * @param object Result of query or update of type TdApi.Update about new events.
         */
        void onResult(TdApi.Object object);
    }

    /**
     * Interface for handler of exceptions thrown while invoking ResultHandler.
     * By default, all such exceptions are ignored.
     * All exceptions thrown from ExceptionHandler are ignored.
     */
    public interface ExceptionHandler {
        /**
         * Callback called on exceptions thrown while invoking ResultHandler.
         *
         * @param e Exception thrown by ResultHandler.
         */
        void onException(Throwable e);
    }

    /**
     * Sends a request to the TDLib.
     *
     * @param query            Object representing a query to the TDLib.
     * @param resultHandler    Result handler with onResult method which will be called with result
     *                         of the query or with TdApi.Error as parameter. If it is null, nothing
     *                         will be called.
     * @param exceptionHandler Exception handler with onException method which will be called on
     *                         exception thrown from resultHandler. If it is null, then
     *                         defaultExceptionHandler will be called.
     * @throws NullPointerException if query is null.
     */
    public void send(TdApi.Function query, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        if (query == null) {
            throw new NullPointerException("query is null");
        }

        readLock.lock();
        try {
            if (isClientDestroyed) {
                if (resultHandler != null) {
                    handleResult(new TdApi.Error(500, "Client is closed"), resultHandler, exceptionHandler);
                }
                return;
            }

            long queryId = currentQueryId.incrementAndGet();
            handlers.put(queryId, new Handler(resultHandler, exceptionHandler));
            NativeClient.clientSend(nativeClientId, queryId, query);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Sends a request to the TDLib with an empty ExceptionHandler.
     *
     * @param query         Object representing a query to the TDLib.
     * @param resultHandler Result handler with onResult method which will be called with result
     *                      of the query or with TdApi.Error as parameter. If it is null, then
     *                      defaultExceptionHandler will be called.
     * @throws NullPointerException if query is null.
     */
    public void send(TdApi.Function query, ResultHandler resultHandler) {
        send(query, resultHandler, null);
    }

    /**
     * Synchronously executes a TDLib request. Only a few marked accordingly requests can be executed synchronously.
     *
     * @param query            Object representing a query to the TDLib.
     * @throws NullPointerException if query is null.
     * @return request result.
     */
    public static TdApi.Object execute(TdApi.Function query) {
        if (query == null) {
           throw new NullPointerException("query is null");
        }
        return NativeClient.clientExecute(query);
    }

    /**
     * Replaces handler for incoming updates from the TDLib.
     *
     * @param updatesHandler   Handler with onResult method which will be called for every incoming
     *                         update from the TDLib.
     * @param exceptionHandler Exception handler with onException method which will be called on
     *                         exception thrown from updatesHandler, if it is null, defaultExceptionHandler will be invoked.
     */
    public void setUpdatesHandler(ResultHandler updatesHandler, ExceptionHandler exceptionHandler) {
        handlers.put(0L, new Handler(updatesHandler, exceptionHandler));
    }

    /**
     * Replaces handler for incoming updates from the TDLib. Sets empty ExceptionHandler.
     *
     * @param updatesHandler Handler with onResult method which will be called for every incoming
     *                       update from the TDLib.
     */
    public void setUpdatesHandler(ResultHandler updatesHandler) {
        setUpdatesHandler(updatesHandler, null);
    }

    /**
     * Replaces default exception handler to be invoked on exceptions thrown from updatesHandler and all other ResultHandler.
     *
     * @param defaultExceptionHandler Default exception handler. If null Exceptions are ignored.
     */
    public void setDefaultExceptionHandler(Client.ExceptionHandler defaultExceptionHandler) {
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    /**
     * Function for benchmarking number of queries per second which can handle the TDLib, ignore it.
     *
     * @param query   Object representing a query to the TDLib.
     * @param handler Result handler with onResult method which will be called with result
     *                of the query or with TdApi.Error as parameter.
     * @param count   Number of times to repeat the query.
     * @throws NullPointerException if query is null.
     */
    public void bench(TdApi.Function query, ResultHandler handler, int count) {
        if (query == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < count; i++) {
            send(query, handler);
        }
    }

    /**
     * Overridden method from Runnable, do not call it directly.
     */
    @Override
    public void run() {
        while (!stopFlag) {
            receiveQueries(300.0 /*seconds*/);
        }
        Log.d("DLTD", "Stop TDLib thread");
    }

    /**
     * Creates new Client.
     *
     * @param updatesHandler          Handler for incoming updates.
     * @param updatesExceptionHandler Handler for exceptions thrown from updatesHandler. If it is null, exceptions will be iggnored.
     * @param defaultExceptionHandler Default handler for exceptions thrown from all ResultHandler. If it is null, exceptions will be iggnored.
     * @return created Client
     */
    public static Client create(ResultHandler updatesHandler, ExceptionHandler updatesExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        Client client = new Client(updatesHandler, updatesExceptionHandler, defaultExceptionHandler);
        new Thread(client, "TDLib thread").start();
        return client;
    }

    /**
     * Changes TDLib log verbosity.
     *
     * @param newLogVerbosity New value of log verbosity. Must be non-negative.
     *                        Value 0 corresponds to android.util.Log.ASSERT,
     *                        value 1 corresponds to android.util.Log.ERROR,
     *                        value 2 corresponds to android.util.Log.WARNING,
     *                        value 3 corresponds to android.util.Log.INFO,
     *                        value 4 corresponds to android.util.Log.DEBUG,
     *                        value 5 corresponds to android.util.Log.VERBOSE,
     *                        value greater than 5 can be used to enable even more logging.
     *                        Default value of the log verbosity is 5.
     * @throws IllegalArgumentException if newLogVerbosity is negative.
     */
    public static void setLogVerbosityLevel(int newLogVerbosity) {
        if (newLogVerbosity < 0) {
            throw new IllegalArgumentException();
        }
        NativeClient.setLogVerbosityLevel(newLogVerbosity);
    }

    /**
     * Sets file path for writing TDLib internal log.
     * By default TDLib writes logs to the Android Log.
     * Use this method to write the log to a file instead.
     *
     * @param filePath Path to a file for writing TDLib internal log. Use an empty path to
     *                 switch back to logging to the Android Log.
     * @return whether opening the log file succeeded
     */
    public static boolean setLogFilePath(String filePath) {
        return NativeClient.setLogFilePath(filePath);
    }

    /**
     * Changes maximum size of TDLib log file.
     *
     * @param maxFileSize Maximum size of the file to where the internal TDLib log is written
     *                    before the file will be auto-rotated. Must be positive. Defaults to 10 MB.
     * @throws IllegalArgumentException if max_file_size is non-positive.
     */
    public static void setLogMaxFileSize(long maxFileSize) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException();
        }
        NativeClient.setLogMaxFileSize(maxFileSize);
    }

    /**
     * Closes Client.
     */
    public void close() {
        writeLock.lock();
        try {
            if (isClientDestroyed) {
                return;
            }
            if (!stopFlag) {
                send(new TdApi.Close(), null);
            }
            isClientDestroyed = true;
            while (!stopFlag) {
                Thread.yield();
            }
            while (handlers.size() != 1) {
                receiveQueries(300.0);
            }
            NativeClient.destroyClient(nativeClientId);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * This function is called from the JNI when a fatal error happens to provide a better error message.
     * It shouldn't return. Do not call it directly.
     *
     * @param errorMessage Error message.
     */
    static void onFatalError(String errorMessage) {
        class ThrowError implements Runnable {
            private ThrowError(String errorMessage) {
                this.errorMessage = errorMessage;
            }

            @Override
            public void run() {
                throw new RuntimeException("TDLib fatal error: " + errorMessage);
            }

            private final String errorMessage;
        }
        new Thread(new ThrowError(errorMessage), "TDLib fatal error thread").start();
        while (true) {
            try {
                Thread.sleep(1000);                 // 1000 milliseconds is one second.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private volatile boolean stopFlag = false;
    private volatile boolean isClientDestroyed = false;
    private final long nativeClientId;

    private final ConcurrentHashMap<Long, Handler> handlers = new ConcurrentHashMap<Long, Handler>();
    private final AtomicLong currentQueryId = new AtomicLong();

    private volatile ExceptionHandler defaultExceptionHandler = null;

    private static final int MAX_EVENTS = 1000;
    private final long[] eventIds = new long[MAX_EVENTS];
    private final TdApi.Object[] events = new TdApi.Object[MAX_EVENTS];

    private static class Handler {
        final ResultHandler resultHandler;
        final ExceptionHandler exceptionHandler;

        Handler(ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
            this.resultHandler = resultHandler;
            this.exceptionHandler = exceptionHandler;
        }
    }

    private Client(ResultHandler updatesHandler, ExceptionHandler updateExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        nativeClientId = NativeClient.createClient();
        handlers.put(0L, new Handler(updatesHandler, updateExceptionHandler));
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void processResult(long id, TdApi.Object object) {
        Handler handler;
        if (object instanceof TdApi.UpdateAuthorizationState) {
            if (((TdApi.UpdateAuthorizationState) object).authorizationState instanceof TdApi.AuthorizationStateClosed) {
                stopFlag = true;
            }
        }
        if (id == 0) {
            // update handler stays forever
            handler = handlers.get(id);
        } else {
            handler = handlers.remove(id);
        }
        if (handler == null) {
            Log.e("DLTD", "Can't find handler for the result " + id + " -- ignore result");
            return;
        }

        handleResult(object, handler.resultHandler, handler.exceptionHandler);
    }

    private void handleResult(TdApi.Object object, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        if (resultHandler == null) {
            return;
        }

        try {
            resultHandler.onResult(object);
        } catch (Throwable cause) {
            if (exceptionHandler == null) {
                exceptionHandler = defaultExceptionHandler;
            }
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.onException(cause);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void receiveQueries(double timeout) {
        int resultN = NativeClient.clientReceive(nativeClientId, eventIds, events, timeout);
        for (int i = 0; i < resultN; i++) {
            processResult(eventIds[i], events[i]);
            events[i] = null;
        }
    }
}
