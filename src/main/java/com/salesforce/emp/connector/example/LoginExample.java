/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.emp.connector.ApiActions;
import com.salesforce.emp.connector.object.Event;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.util.ajax.JSON;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of using the EMP connector using login credentials
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class LoginExample {
    final static Logger log = LoggerFactory.getLogger(LoginExample.class);

    public static void main(String[] argv) throws Exception {
        if (argv.length < 5 || argv.length > 6) {
            System.err.println("Usage: LoginExample <Salesforce_username> <Salesforce_password> <Cloudcard_username> <Cloudcard_password> <topic_url> [replayFrom]");
            System.exit(1);
        }

        Map<String, String> arguments = new HashMap<>();
        arguments.put("salesforce_username", argv[0]);
        arguments.put("salesforce_password", argv[1]);
        arguments.put("cloudcard_username", argv[2]);
        arguments.put("cloudcard_password", argv[3]);
        arguments.put("topic", argv[4]);
        if (argv.length == 6) {
            arguments.put("replayFrom", argv[5]);
        }

        long replayFrom = EmpConnector.REPLAY_FROM_TIP;
        if (arguments.containsKey("replayFrom")) {
            replayFrom = Long.parseLong(arguments.get("replayFrom"));
        }

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
//                loginToCloudcard(arguments);
                String accessToken = ApiActions.login(arguments.get("cloudcard_username"), arguments.get("cloudcard_password"));
                log.info("Successfully logged in to CloudCard: Access token is " + accessToken);
                arguments.put("cloudcard_access_token", accessToken);
                return login(arguments.get("salesforce_username"), arguments.get("salesforce_password"));
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

//        NOTE: this consumer is where the connector logic would go.
        Consumer<Map<String, Object>> consumer = event -> {
            Event eventObject = convertJsonToEventObject(event);

            log.info(eventObject.getType() + " invoice statement at " + eventObject.getCreatedDate() + " (replayID: " + eventObject.getReplayId() + ")");
        };

        EmpConnector connector = new EmpConnector(params);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        TopicSubscription subscription = connector.subscribe(arguments.get("topic"), replayFrom, consumer).get(5, TimeUnit.SECONDS);

        log.info(String.format("Subscribed: %s", subscription));
    }

    private static Event convertJsonToEventObject(Map<String, Object> event) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = null;
        ZonedDateTime datetime = null;
        try {
            json = objectMapper.readTree(JSON.toString(event));
            datetime = ZonedDateTime.parse(json.at("/event/createdDate").asText());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Event(datetime, json.at("/event/replayId").asInt(), json.at("/event/type").asText());
    }
}
