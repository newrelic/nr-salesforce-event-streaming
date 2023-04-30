package com.newrelic.fit.empcon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.fit.config.Instance;
import com.newrelic.fit.util.JsonFlattenFunction;
import com.newrelic.insights.publish.Event;
import com.newrelic.insights.publish.InsightsClient;
import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;
import com.salesforce.emp.connector.example.BearerTokenProvider;
import com.salesforce.emp.connector.example.LoggingListener;
import org.cometd.bayeux.Channel;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.newrelic.fit.empcon.AuthHelper.getBayeuxParametersSupplier;

public class MonitoringConsumer {
    private static Logger logger = LoggerFactory.getLogger(MonitoringConsumer.class);
    private EmpConnector connector = null;
    private InsightsClient insightsClient = null;
    private Instance instance = null;

    private Stats stats=null;

    public TopicSubscription connect(Instance instance, InsightsClient client) {
        logger.info("connecting: " + instance.getUrl());
        this.insightsClient = client;
        this.instance = instance;
        this.stats= new Stats(instance,client);
        try {
            connector = createConsumer(instance);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("failed to connect: " + e.getMessage());
            logger.error("exiting...");
            e.printStackTrace();
            System.exit(1);
        }
        try {
            TopicSubscription subscription = connector.subscribe(instance.getChannel(), instance.getRelayfrom(), this::onMessage).get(5, TimeUnit.SECONDS);
            logger.info("==>subscribed: " + instance.getUrl() + subscription);
            return subscription;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("failed to subscribe: " + instance.getChannel());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private EmpConnector createConsumer(Instance instance) throws Exception {


        BearerTokenProvider tokenProvider = new BearerTokenProvider(getBayeuxParametersSupplier(instance));

        BayeuxParameters params = tokenProvider.login();
        logger.debug("cometd endpoint: {}", params.endpoint());
        EmpConnector connector = new EmpConnector(params);
        LoggingListener loggingListener = new LoggingListener(false, true);

        connector.addListener(Channel.META_HANDSHAKE, loggingListener)
                .addListener(Channel.META_CONNECT, loggingListener)
                .addListener(Channel.META_DISCONNECT, loggingListener)
                .addListener(Channel.META_SUBSCRIBE, loggingListener)
                .addListener(Channel.META_UNSUBSCRIBE, loggingListener);

        connector.setBearerTokenProvider(tokenProvider);
        connector.start().get(5, TimeUnit.SECONDS);
        return connector;
    }


    public HashMap<String, String> getJsonAsMap(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
            };
            HashMap<String, String> result = mapper.readValue(json, typeRef);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Couldnt parse json:" + json, e);
        }
    }

    public void onMessage(Map<String, Object> event) {

        JsonFlattenFunction transformer = new JsonFlattenFunction();
        logger.debug("received event:{}", JSON.toString(event));
        String insightsJson = transformer.apply(JSON.toString(event));

        Map<String, String> insightEvent = getJsonAsMap(insightsJson);
        List<Event> nrevents = new ArrayList<Event>();

        Event nrevent = Event.create(instance.getEventtype());
        nrevent.put("url", instance.getUrl());
        nrevent.put("channel", instance.getChannel());
        nrevent.put("username", instance.getUsername());
        nrevent.put("relayfrom", instance.getRelayfrom());

        for (Map.Entry<String, String> entry : insightEvent.entrySet()) {
            nrevent.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Object> entry : instance.getLabels().entrySet()) {
            nrevent.put(entry.getKey(), entry.getValue());
        }

        nrevents.add(nrevent);

        boolean result = insightsClient.post(nrevents.toArray(new Event[nrevents.size()]));

        if (result != true) {
            logger.error("Unable to post events to Insights");
        }
        stats.incrMessageCount();

    }


    public void stop() {
        if (connector != null) {
            connector.stop();
        }
    }

    public Stats getStats() {
        return stats;
    }
}