// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package io.ballerinax.salesforce;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.ballerinax.salesforce.Constants.CHANNEL_NAME;
import static io.ballerinax.salesforce.Constants.CONSUMER_SERVICES;
import static io.ballerinax.salesforce.Constants.IS_SAND_BOX;
import static io.ballerinax.salesforce.Constants.REPLAY_FROM;
import static org.cometd.bayeux.Channel.META_CONNECT;
import static org.cometd.bayeux.Channel.META_DISCONNECT;
import static org.cometd.bayeux.Channel.META_HANDSHAKE;
import static org.cometd.bayeux.Channel.META_SUBSCRIBE;
import static org.cometd.bayeux.Channel.META_UNSUBSCRIBE;


/**
 * Util class containing the java external functions for Salesforce Ballerina trigger.
 */
public class ListenerUtil {
    private static final ArrayList<BObject> services = new ArrayList<>();
    private static Runtime runtime;
    private static EmpConnector connector;
    private static TopicSubscription subscription;

    public static void initListener(BObject listener, int replayFrom, boolean isSandBox) {
        listener.addNativeData(CONSUMER_SERVICES, services);
        listener.addNativeData(REPLAY_FROM, replayFrom);
        listener.addNativeData(IS_SAND_BOX, isSandBox);
    }

    public static Object attachService(BObject listener, BObject service, Object channelName) {
        listener.addNativeData(CHANNEL_NAME, ((BString) channelName).getValue());
        @SuppressWarnings("unchecked")
        ArrayList<BObject> services =
                (ArrayList<BObject>) listener.getNativeData(CONSUMER_SERVICES);
        if (service == null) {
            return null;
        }
        services.add(service);
        return null;
    }

    public static Object startListener(Environment environment, BString username, BString password, BObject listener) {
        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(username.getValue(), password.getValue(), listener);
            } catch (Exception e) {
                throw sfdcError(e.getMessage());
            }
        });
        BayeuxParameters params;
        try {
            params = tokenProvider.login();
        } catch (Exception e) {
            throw sfdcError(e.getMessage());
        }
        connector = new EmpConnector(params);
        LoggingListener loggingListener = new LoggingListener(true, true);
        connector.addListener(META_CONNECT, loggingListener)
                .addListener(META_DISCONNECT, loggingListener)
                .addListener(META_HANDSHAKE, loggingListener)
                .addListener(META_SUBSCRIBE, loggingListener)
                .addListener(META_UNSUBSCRIBE, loggingListener);
        try {
            connector.start().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw sfdcError(e.getMessage());
        }
        runtime = environment.getRuntime();
        @SuppressWarnings("unchecked")
        ArrayList<BObject> services =
                (ArrayList<BObject>) listener.getNativeData(CONSUMER_SERVICES);
        for (BObject service : services) {
            String channelName = listener.getNativeData(CHANNEL_NAME).toString();
            long replayFrom  = (Integer) listener.getNativeData(REPLAY_FROM);
            Consumer<Map<String, Object>> consumer = event -> injectEvent(service, runtime, event);
            try {
                subscription = connector.subscribe(channelName, replayFrom, consumer).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw sfdcError(e.getMessage());
            }
        }
        return null;
    }

    public static Object detachService(BObject listener, BObject service) {
        String channel = listener.getNativeData(CHANNEL_NAME).toString();
        connector.unsubscribe(channel);
        @SuppressWarnings("unchecked")
        ArrayList<BObject> services =
                (ArrayList<BObject>) listener.getNativeData(CONSUMER_SERVICES);
        services.remove(service);
        return null;
    }

    public static Object stopListener() {
        // Cancel a subscription
        subscription.cancel();
        // Stop the connector
        connector.stop();
        return null;
    }

    private static void injectEvent(BObject serviceObject, Runtime runtime, Map<String, Object> eventData) {
        DispatcherService dispatcherService = new DispatcherService(serviceObject, runtime);
        dispatcherService.handleDispatch(eventData);
    }

    private static BError sfdcError(String errorMessage) {
        return ErrorCreator.createError(StringUtils.fromString(errorMessage));
    }
}
