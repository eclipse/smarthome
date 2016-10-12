/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.audio;

import java.net.URL;

import org.eclipse.smarthome.core.audio.internal.AudioServlet;

/**
 * This is an interface that is implemented by {@link AudioServlet} and which allows exposing audio streams through
 * HTTP.
 * Streams are only served a single time and then discarded.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public interface AudioHTTPServer {

    /**
     * Creates a url for a given {@link AudioStream} where it can be requested a single time.
     * Note that the HTTP header only contains "Content-length", if the passed stream is an instance of
     * {@link FixedLengthAudioStream}.
     * If the client that requests the url expects this header field to be present, make sure to pass such an instance.
     *
     * @param stream the stream to serve on HTTP
     * @return the absolute URL to access the stream (using the primary network interface)
     */
    URL serve(AudioStream stream);

    /**
     * Creates a url for a given {@link AudioStream} where it can be requested multiple times within the given time
     * frame.
     * This method only accepts {@link FixedLengthAudioStream}s, since it needs to be able to create multiple concurrent
     * streams from it, which isn't possible with a regular {@link AudioStream}.
     *
     * @param stream the stream to serve on HTTP
     * @param seconds number of seconds for which the stream is available through HTTP
     * @return the absolute URL to access the stream (using the primary network interface)
     */
    URL serve(FixedLengthAudioStream stream, int seconds);

}
