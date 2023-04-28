package com.newrelic.fit.empcon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.fit.config.Instance;
import com.newrelic.fit.util.JsonFlattenFunction;
import com.newrelic.insights.publish.Event;
import com.newrelic.insights.publish.InsightsClient;
import com.salesforce.emp.connector.*;
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
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.MalformedURLException;



import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;






import static com.salesforce.emp.connector.LoginHelper.login;

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

                        //       this.salesforceLogin(instance.getClientKey(), instance.getClientSecret(), instance.getUrl());// Replace login, with my Login component
                        //  } else {
                    if (instance.getClientKey() == null) {
                        return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword(), proxyParas);
                    } else {
                        return this.salesforceLogin(instance.getClientKey(), instance.getClientSecret(), instance.getUrl(),proxyParas);
                     //   return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword(), proxyParas);
                    }//   }

                        //  }
                        // String scode = instance.getCode();
                        //


                } else {
                    if (instance.getClientKey() == null) {
                        return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword());
                    } else {

                        return this.salesforceLogin(instance.getClientKey(), instance.getClientSecret(), instance.getUrl());
                      //  return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword());
                    }

                   }
              //  }
               // else {
                 //   if (instance.getClientKey() != null) {


                       // return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword(), proxyParas);
                      //  this.salesforceLogin(instance.getClientKey(), instance.getClientSecret(), instance.getUrl());


                 //   }
                  //  else {
                        //  return salesforceLogin("yourClientId", "yourClientSecret", "yourRedirectUri", "yourCode");

                       // return login(new URL(instance.getUrl()), instance.getUsername(), instance.getPassword());
                 //   }
              //  }
            } catch (Exception e) {
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

    public  BayeuxParameters salesforceLogin(String clientId, String clientSecret, String clientUrl) throws Exception {
        return salesforceLogin(clientId, clientSecret, clientUrl, new BayeuxParameters() {
            public String bearerToken() {
                throw new IllegalStateException("Have not authenticated");
            }

            public URL endpoint() {
                throw new IllegalStateException("Have not established replay endpoint");
            }
        });
    }
    public BayeuxParameters salesforceLogin(String clientId, String clientSecret, String clientUrl, BayeuxParameters parameters  ){
        String tokenEndpoint = clientUrl + "/services/oauth2/token";


// Prepare the request parameters
        //BayeuxParameters parameters = new BayeuxParameters();
        //BayeuxParameters parameters = null;
        //String hostURL = clientUrl.substring(8);
        String encodedCredentials = encodeCredentials(clientId, clientSecret);
        String requestBody = "grant_type=client_credentials";
       // HttpClient httpClient = new HttpClient(parameters.sslContextFactory());

        HttpClient httpClient = HttpClients.createDefault();
     //   httpClient.getProxyConfiguration().getProxies().addAll(parameters.proxies());
        HttpPost httpPost = new HttpPost(tokenEndpoint);
        httpPost.setHeader("Authorization", "Basic " + encodedCredentials);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        String responseString = null;
        try {
            HttpEntity responseEntity = httpClient.execute(httpPost).getEntity();
            responseString = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // handle the exception
        }


        JSONObject jsonResponse = new JSONObject(responseString);
        String sessionId = jsonResponse.getString("access_token");
        String instanceURL = jsonResponse.getString("instance_url");
        String hostURL = instanceURL.substring(8);




       // URL soapEndpoint = new URL(parser.serverUrl);
       // String cometdEndpoint = Float.parseFloat(parameters.version()) < 37.0F ? "/cometd/replay/" : "/cometd/";
        URL rplayEndpoint = null;
        try {
            rplayEndpoint = new URL("https",hostURL , -1, "/cometd/43.0");
        } catch (MalformedURLException e) {
            // handle the exception
            e.printStackTrace();
        }

        final URL replayEndpoint = rplayEndpoint;
        BayeuxParameters params = new DelegatingBayeuxParameters(parameters) {
            @Override
            public String bearerToken() {
                return sessionId;
            }

            @Override
            public String version() {
                return "43.0";
            }

            @Override
            public URL endpoint() {
                return replayEndpoint;
            }
        };


        System.out.println("Access Token: " + sessionId);
        System.out.println("Instance URL: " + instanceURL);
        System.out.println("Host URL: " + hostURL);
        System.out.println("Client URL: " + clientUrl);

        return params;


    }


    private static String encodeCredentials(String clientId, String clientSecret) {
        String plainCredentials = clientId + ":" + clientSecret;
        byte[] plainCredentialsBytes = plainCredentials.getBytes(StandardCharsets.UTF_8);
        String encodedCredentials = Base64.getEncoder().encodeToString(plainCredentialsBytes);
        return encodedCredentials;
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