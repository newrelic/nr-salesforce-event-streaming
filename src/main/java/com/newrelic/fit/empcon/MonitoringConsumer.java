package com.newrelic.fit.empcon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.fit.config.Instance;
import com.newrelic.fit.util.JsonFlattenFunction;
import com.newrelic.insights.publish.Event;
import com.newrelic.insights.publish.InsightsClient;
import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.ProxyBayeuxParameter;
import com.salesforce.emp.connector.TopicSubscription;
import com.salesforce.emp.connector.example.BearerTokenProvider;
import com.salesforce.emp.connector.example.LoggingListener;
import org.cometd.bayeux.Channel;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.salesforce.emp.connector.LoginHelper.login;

public class MonitoringConsumer {
    private static Logger logger = LoggerFactory.getLogger(MonitoringConsumer.class);
    private EmpConnector connector = null;
    private InsightsClient insightsClient = null;
    private Instance instance = null;

    public TopicSubscription connect(Instance instance, InsightsClient client) {
        this.insightsClient = client;
        this.instance = instance;

        try {
            connector = createConsumer(instance);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            TopicSubscription subscription = connector.subscribe(instance.getChannel(), instance.getRelayfrom(), this::onMessage).get(5, TimeUnit.SECONDS);
            logger.info("==>subscribed: " + instance.getUrl() + subscription);
            return subscription;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private EmpConnector createConsumer(Instance instance) throws Exception {

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {

                if (Instance.getProxyHost() != null) {

                    ProxyBayeuxParameter proxyParas = new ProxyBayeuxParameter();

                    String proxyHost = Instance.getProxyHost();
                    int proxyPort = Instance.getProxyPort();

                    HttpProxy proxy = new HttpProxy(new Origin.Address(proxyHost, proxyPort), false);
                    proxyParas.addProxy(proxy);

                    if (Instance.getProxyUser() != null) {
                        String proxy_auth_username = Instance.getProxyUser();
                        String proxy_auth_password = Instance.getProxyPassword();
                        BasicAuthentication auth = new BasicAuthentication(new URI("http://" + proxyHost + ":" + proxyPort), Authentication.ANY_REALM, proxy_auth_username, proxy_auth_password);
                        proxyParas.addAuthentication(auth);
                    }

                    return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword(), proxyParas);

                } else {

                    return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword());
                }


            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

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

    }


    public void stop() {
        if (connector != null) {
            connector.stop();
        }
    }


}