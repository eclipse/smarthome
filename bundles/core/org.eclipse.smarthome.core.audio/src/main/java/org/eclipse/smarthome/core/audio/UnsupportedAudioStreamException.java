/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.audio;

/**
 * Thrown when a requested {@link AudioStream} is not supported by an {@link AudioSource} or {@link AudioSink}
 * implementation
 * 
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * 
 */
public class UnsupportedAudioStreamException extends AudioException {

    private static final long serialVersionUID = 1L;

    /**
     * Unsupported {@link AudioStream}
     */
    private Class<? extends AudioStream> unsupportedAudioStreamClass;

    /**
     * Constructs a new exception with the specified detail message, unsupported format, and cause.
     *
     * @param message The message
     * @param unsupportedAudioStreamClass The unsupported audio stream class
     * @param cause The cause
     */
    public UnsupportedAudioStreamException(String message, Class<? extends AudioStream> unsupportedAudioStreamClass,
            Throwable cause) {
        super(message, cause);
        this.unsupportedAudioStreamClass = unsupportedAudioStreamClass;
    }

    /**
     * Constructs a new exception with the specified detail message and unsupported format.
     *
     * @param message The message
     * @param unsupportedAudioStreamClass The unsupported audio stream class
     */
    public UnsupportedAudioStreamException(String message, Class<? extends AudioStream> unsupportedAudioStreamClass) {
        this(message, unsupportedAudioStreamClass, null);
    }

    /**
     * Gets the unsupported audio stream class.
     *
     * @return The unsupported audio stream class
     */
    public Class<? extends AudioStream> getUnsupportedAudioStreamClass() {
        return unsupportedAudioStreamClass;
    }
}
