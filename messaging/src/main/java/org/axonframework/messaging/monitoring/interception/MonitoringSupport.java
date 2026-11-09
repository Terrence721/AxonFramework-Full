/*
 * Copyright (c) 2026. Terrence Daniels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.messaging.monitoring.interception;

import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.monitoring.MessageMonitor;

/**
 * Shared behavior of the monitoring interceptors in this package.
 * <p>
 * Reports the given {@code message} to the {@link MessageMonitor} and ties the outcome to the lifecycle of the given
 * {@link ProcessingContext}: a failure in the context is reported as a failure, a successful commit as a success.
 *
 * @author Terrence Daniels
 * @since 5.0.0
 */
final class MonitoringSupport {

    private MonitoringSupport() {
        // Utility class
    }

    /**
     * Starts monitoring the given {@code message} when the given {@code context} has been started. Does nothing for a
     * context that was not started.
     *
     * @param messageMonitor The monitor to report the message to.
     * @param message        The message to monitor.
     * @param context        The context whose outcome determines what is reported.
     * @param <M>            The type of message to monitor.
     */
    static <M extends Message> void monitorIfStarted(MessageMonitor<? super M> messageMonitor,
                                                     M message,
                                                     ProcessingContext context) {
        if (!context.isStarted()) {
            return;
        }
        MessageMonitor.MonitorCallback monitorCallback = messageMonitor.onMessageIngested(message);
        context.onError((ctx, phase, error) -> monitorCallback.reportFailure(error));
        context.runOnAfterCommit(ctx -> monitorCallback.reportSuccess());
    }
}
