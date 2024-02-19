/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link StdSerializer} for {@link ITmfTrace} to avoid building intermediate
 * representations.
 *
 * @author Loic Prieur-Drevon
 */
public class TraceSerializer extends StdSerializer<@NonNull ITmfTrace> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 9170252203750031947L;

    /**
     * Public constructor
     */
    public TraceSerializer() {
        super(ITmfTrace.class);
    }

    @Override
    public void serialize(ITmfTrace value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("path", value.getPath()); //$NON-NLS-1$
        gen.writeStringField("UUID", Objects.requireNonNull(value.getUUID()).toString()); //$NON-NLS-1$
        gen.writeNumberField("nbEvents", value.getNbEvents()); //$NON-NLS-1$
        gen.writeNumberField("start", value.getStartTime().toNanos()); //$NON-NLS-1$
        gen.writeNumberField("end", value.getEndTime().toNanos()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
