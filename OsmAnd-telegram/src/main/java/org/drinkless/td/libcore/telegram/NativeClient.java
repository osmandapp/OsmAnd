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

/**
 * This class is used internally by Client to send requests to the TDLib.
 */
final class NativeClient {
    static {
        try {
            System.loadLibrary("tdjni");
            Log.w("DLTD", "TDJNI loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.w("DLTD", "Can't find tdjni", e);
        }
    }

    public static native long createClient();

    public static native void destroyClient(long clientId);

    public static native void clientSend(long clientId, long eventId, TdApi.Object event);

    public static native int clientReceive(long clientId, long[] eventIds, TdApi.Object[] events, double timeout);

    public static native TdApi.Object clientExecute(TdApi.Object event);

    public static native void setLogVerbosityLevel(int newLogVerbosity);

    public static native boolean setLogFilePath(String filePath);

    public static native void setLogMaxFileSize(long maxFileSize);

    //Just for testing
    public static native TdApi.Object pingPong(TdApi.Object object);

    public static native void ping(TdApi.Object object);
}
