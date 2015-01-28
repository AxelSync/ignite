/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.rest.handlers.datastructures;

import org.apache.ignite.*;
import org.apache.ignite.internal.*;
import org.apache.ignite.internal.processors.rest.*;
import org.apache.ignite.internal.processors.rest.handlers.*;
import org.apache.ignite.internal.processors.rest.request.*;
import org.apache.ignite.internal.util.future.*;
import org.apache.ignite.internal.util.typedef.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.lang.*;

import java.util.*;
import java.util.concurrent.*;

import static org.apache.ignite.internal.processors.rest.GridRestCommand.*;

/**
 *
 */
public class DataStructuresCommandHandler extends GridRestCommandHandlerAdapter {
    /** Supported commands. */
    private static final Collection<GridRestCommand> SUPPORTED_COMMANDS = U.sealList(
        CACHE_INCREMENT,
        CACHE_DECREMENT
    );
    /**
     * @param ctx Context.
     */
    public DataStructuresCommandHandler(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridRestCommand> supportedCommands() {
        return SUPPORTED_COMMANDS;
    }

    /** {@inheritDoc} */
    @Override public IgniteFuture<GridRestResponse> handleAsync(GridRestRequest req) {
        assert SUPPORTED_COMMANDS.contains(req.command()) : req.command();

        return incrementOrDecrement((DataStructuresRequest)req).chain(new CX1<IgniteFuture<?>, GridRestResponse>() {
            @Override public GridRestResponse applyx(IgniteFuture<?> fut) throws IgniteCheckedException {
                GridRestResponse res = new GridRestResponse();

                res.setResponse(fut.get());

                return res;
            }
        });
    }
    /**
     * Handles increment and decrement commands.
     *
     * @param req Request.
     * @return Future of operation result.
     */
    private IgniteFuture<?> incrementOrDecrement(final DataStructuresRequest req) {
        assert req != null;
        assert req.command() == CACHE_INCREMENT || req.command() == CACHE_DECREMENT : req.command();

        if (req.key() == null) {
            IgniteCheckedException err =
                new IgniteCheckedException(GridRestCommandHandlerAdapter.missingParameter("key"));

            return new GridFinishedFuture(ctx, err);
        }
        else if (req.delta() == null) {
            IgniteCheckedException err =
                new IgniteCheckedException(GridRestCommandHandlerAdapter.missingParameter("delta"));

            return new GridFinishedFuture(ctx, err);
        }

        return ctx.closure().callLocalSafe(new Callable<Object>() {
            @Override public Object call() throws Exception {
                Long init = req.initial();
                Long delta = req.delta();

                boolean decr = req.command() == CACHE_DECREMENT;

                String key = (String)req.key();

                IgniteAtomicLong l = ctx.grid().atomicLong(key, init != null ? init : 0, true);

                return l.addAndGet(decr ? -delta : delta);
            }
        }, false);
    }
}
