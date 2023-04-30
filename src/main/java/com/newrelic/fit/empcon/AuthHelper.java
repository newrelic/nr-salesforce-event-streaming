package com.newrelic.fit.empcon;

import com.newrelic.fit.config.Instance;
import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.DelegatingBayeuxParameters;

import com.salesforce.emp.connector.ProxyBayeuxParameter;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.util.function.Supplier;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.salesforce.emp.connector.LoginHelper.login;

public class AuthHelper {
    private static Logger logger = LoggerFactory.getLogger(AuthHelper.class);

    public static Supplier<BayeuxParameters> getBayeuxParametersSupplier(Instance instance) {
        return () -> {
            try {
                URL hostUrl=new URL(instance.getUrl());
                    ProxyBayeuxParameter proxyParas = new ProxyBayeuxParameter(new BayeuxParameters() {
                        public String bearerToken() {
                            throw new IllegalStateException("Have not authenticated");
                        }
                        @Override
                        public URL host(){
                            return hostUrl;
                        }
                    });

                    if (Instance.getProxyHost() != null) {
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
                    }
                    if (instance.getClientKey() == null) {
                        return login(hostUrl, instance.getUsername(), instance.getPassword(), proxyParas);
                    } else {
                        return oauthlogin(instance.getClientKey(), instance.getClientSecret(), hostUrl, proxyParas);
                    }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
    private static BayeuxParameters oauthlogin(String clientId, String clientSecret, URL hostUrl, BayeuxParameters parameters) throws Exception {
        final String tokenEndpoint = hostUrl + "/services/oauth2/token";
        HttpClient client = new HttpClient(parameters.sslContextFactory());
        try {
            client.getProxyConfiguration().getProxies().addAll(parameters.proxies());
            logger.info("proxyes : {}", parameters.proxies());
            AuthenticationStore authenticationStore = client.getAuthenticationStore();
            for (Authentication auth : parameters.authentications()) {
                logger.info("proxy auth: {}", parameters.proxies());
                authenticationStore.addAuthentication(auth);
            }
            client.start();
            logger.debug("tokenEndPoint: {}", tokenEndpoint);
            String response = client.newRequest(tokenEndpoint)
                    .method(HttpMethod.POST)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .param("grant_type", "client_credentials")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .send()
                    .getContentAsString();

            logger.debug("http response: {}", response);
            JSONObject jsonResponse = new JSONObject(response);
            String accessToken = jsonResponse.getString("access_token");

            return new DelegatingBayeuxParameters(parameters) {
                @Override
                public String bearerToken() {
                    return accessToken;
                }
                @Override
                public URL host(){
                    return hostUrl;
                }
            };

        } finally {
            client.stop();
            client.destroy();
        }
    }

}

