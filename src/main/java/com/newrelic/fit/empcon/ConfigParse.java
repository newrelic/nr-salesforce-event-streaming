package com.newrelic.fit.empcon;

import com.newrelic.fit.config.Instance;
import com.newrelic.infra.publish.security.Obfuscator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConfigParse {

    private static Logger logger = LoggerFactory.getLogger(ConfigParse.class);

    public static List<Instance> readConfig(String configFile) {
        List<Instance> configuredInstances = new LinkedList<Instance>();
        FileInputStream is;
        try {
            is = new FileInputStream(configFile);
            Yaml yaml = new Yaml();
            Map<String, Object> ymlParser = yaml.load(is);

            if (ymlParser.containsKey("insights_url")) {
                Instance.setInsightsUrl((String) ymlParser.get("insights_url"));
            } else {
                logger.error("Required property 'insights_url' missing in kafka-config.yml'");
                System.exit(-1);
            }

            if (ymlParser.containsKey("insights_insert_key")) {
                Instance.setInsightsInsertKey((String) ymlParser.get("insights_insert_key"));
            } else {
                logger.error("Required property 'insights_insert_key' missing in kafka-config.yml'");
                System.exit(-1);
            }

            if (ymlParser.containsKey("proxy_host")) {
                Instance.setProxyHost((String) ymlParser.get("proxy_host"));
            }
            if (ymlParser.containsKey("proxy_port")) {
                Instance.setProxyPort((Integer) ymlParser.get("proxy_port"));
            }
            if (ymlParser.containsKey("proxy_user")) {
                Instance.setProxyUser((String) ymlParser.get("proxy_user"));
            }
            if (ymlParser.containsKey("proxy_password")) {
                Instance.setProxyPassword((String) ymlParser.get("proxy_password"));
            }
            if (ymlParser.containsKey("proxy_password_obfuscated")) {
                String password = Obfuscator.deobfuscateNameUsingKey((String) ymlParser.get("proxy_password_obfuscated"));
                Instance.setProxyPassword(password);
            }

            if (ymlParser.containsKey("instances")) {
                List<Map> instancesParser = (List<Map>) ymlParser.get("instances");
                for (Map instanceParser : instancesParser) {
                    Instance inst = new Instance();
                    configuredInstances.add(inst);

                    if (instanceParser.containsKey("connectioninfo")) {
                        Map argumentsParser = (Map) instanceParser.get("connectioninfo");

                        if (argumentsParser.containsKey("url")) {
                            inst.setUrl((String) argumentsParser.get("url"));
                        }
                        if (argumentsParser.containsKey("username")) {
                            inst.setUsername((String) argumentsParser.get("username"));
                        }
                        if (argumentsParser.containsKey("password")) {
                            inst.setPassword((String) argumentsParser.get("password"));
                        }

                        if (argumentsParser.containsKey("password_obfuscated")) {
                            String password = Obfuscator.deobfuscateNameUsingKey((String) argumentsParser.get("password_obfuscated"));
                            inst.setPassword(password);
                        }

                        if (argumentsParser.containsKey("channel")) {
                            inst.setChannel((String) argumentsParser.get("channel"));
                        }
                        if (argumentsParser.containsKey("replayfrom")) {
                            Integer replayfrom = (Integer) argumentsParser.get("replayfrom");
                            inst.setRelayfrom(replayfrom.longValue());

                        }

                        if (instanceParser.containsKey("labels")) {
                            Map<String, Object> labelsParser = (Map<String, Object>) instanceParser.get("labels");
                            labelsParser.forEach((k, v) -> {
                                inst.addLabel(k, v);
                            });
                        }

                        if (instanceParser.containsKey("eventtype")) {
                            inst.setEventtype((String) instanceParser.get("eventtype"));

                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
        return configuredInstances;
    }

    public static List<Instance> readConfigFileOrEnv(String configFile) {
        final boolean NR_SF_ENV_CONF = Boolean.parseBoolean(System.getenv().getOrDefault("NR_SF_ENV_CONF", "FALSE"));
        if (!NR_SF_ENV_CONF) {
            logger.info("NR_SF_ENV_CONF is NOT set, use empcon-config.yml for configuration!");
            return readConfig(configFile);
        }

		/*
		Handle Salesforce connection using environment variables approach
		 */
        logger.info("NR_SF_ENV_CONF is set", NR_SF_ENV_CONF);
        final String NR_SF_TOPICS_DEFAULT = "/event/LightningUriEventStream,/event/LoginEventStream,/event/LogoutEventStream";

        List<Instance> configuredInstances = new LinkedList<Instance>();
        FileInputStream is;
        try {
            final Map<String, String> env = System.getenv();
            String NR_SF_INSIGHTS_URL = env.getOrDefault("NR_SF_INSIGHTS_URL", "FALSE");
            String NR_SF_INSIGHTS_INSERT_KEY = env.getOrDefault("NR_SF_INSIGHTS_INSERT_KEY", "FALSE");
            String NR_SF_PROXY_HOST = env.getOrDefault("NR_SF_PROXY_HOST", "FALSE");
            String NR_SF_PROXY_PORT = env.getOrDefault("NR_SF_PROXY_PORT", "FALSE");
            String NR_SF_PROXY_PASSWORD = env.getOrDefault("NR_SF_PROXY_PASSWORD", "FALSE");
            String NR_SF_PROXY_PASSWORD_OBFUSCATED = env.getOrDefault("NR_SF_PROXY_PASSWORD_OBFUSCATED", "");


            String NR_SF_URL = env.getOrDefault("NR_SF_URL", "FALSE");
            String NR_SF_USERNAME = env.getOrDefault("NR_SF_USERNAME", "FALSE");
            String NR_SF_PASSWORD = env.getOrDefault("NR_SF_PASSWORD", "FALSE");
            String NR_SF_PASSWORD_OBFUSCATED = env.getOrDefault("NR_SF_PASSWORD_OBFUSCATED", "FALSE");

            String NR_SF_TOPICS = env.getOrDefault("NR_SF_TOPICS", NR_SF_TOPICS_DEFAULT);
            String NR_SF_REPLAYFROM = env.getOrDefault("NR_SF_REPLAYFROM", "-1");
            String NR_SF_LABELS = env.getOrDefault("NR_SF_LABELS", "FALSE");
            String NR_SF_EVENT_PREFIX = env.getOrDefault("NR_SF_EVENT_PREFIX", "SF");

            Instance.setEventPrefix(NR_SF_EVENT_PREFIX);

            if (!NR_SF_INSIGHTS_URL.equals("FALSE")) {
                Instance.setInsightsUrl(NR_SF_INSIGHTS_URL);
            } else {
                logger.error("Required env variable 'NR_SF_INSIGHTS_URL'");
                System.exit(-1);
            }
            if (!NR_SF_INSIGHTS_INSERT_KEY.equals("FALSE")) {
                Instance.setInsightsInsertKey(NR_SF_INSIGHTS_INSERT_KEY);
            } else {
                logger.error("Required env variable 'NR_SF_INSIGHTS_INSERT_KEY'");
                System.exit(-1);
            }
            if (!NR_SF_PROXY_HOST.equals("FALSE")) {
                Instance.setProxyHost(NR_SF_PROXY_HOST);
            }
            if (!NR_SF_PROXY_PORT.equals("FALSE")) {
                Instance.setProxyPort(Integer.parseInt(NR_SF_PROXY_PORT));
            }
            if (!NR_SF_PROXY_PASSWORD.equals("FALSE")) {
                Instance.setProxyPassword(NR_SF_PROXY_PASSWORD);
            }

            if (!NR_SF_PROXY_PASSWORD_OBFUSCATED.isEmpty()) {
                String password = Obfuscator.deobfuscateNameUsingKey(NR_SF_PROXY_PASSWORD_OBFUSCATED);
                Instance.setProxyPassword(password);
            }

            String[] topics = NR_SF_TOPICS.split("\\s*,\\s*");
            for (String topic : topics) {
                if (!topic.isEmpty()) {
                    Instance inst = new Instance();
                    configuredInstances.add(inst);
                    inst.setChannel(topic);
                    inst.setEventtype(getEventType(topic));

                    if (!NR_SF_URL.equals("FALSE")) {
                        inst.setUrl(NR_SF_URL);
                    }
                    if (!NR_SF_USERNAME.equals("FALSE")) {
                        inst.setUsername(NR_SF_USERNAME);
                    }
                    if (!NR_SF_PASSWORD.equals("FALSE")) {
                        inst.setPassword(NR_SF_PASSWORD);
                    }
                    if (!NR_SF_PASSWORD_OBFUSCATED.equals("FALSE")) {
                        String password = Obfuscator.deobfuscateNameUsingKey(NR_SF_PASSWORD_OBFUSCATED);
                        inst.setPassword(password);
                    }

                    if (!NR_SF_REPLAYFROM.isEmpty()) {
                        Integer replayfrom = Integer.parseInt(NR_SF_REPLAYFROM);
                        inst.setRelayfrom(replayfrom.longValue());

                    }

                    if (!NR_SF_LABELS.equals("FALSE")) {
                        String[] labels = NR_SF_LABELS.split("\\s*,\\s*");
                        for (int i = 0; i < labels.length; i++) {
                            String[] d = labels[i].split("\\s*=\\s*");
                            if (d.length == 2) {
                                String k = d[0];
                                String v = d[1];
                                inst.addLabel(k, v);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
        return configuredInstances;
    }

    private static String getEventType(String topic) {
        int last = topic.lastIndexOf("/");
        return last >= 0 ? topic.substring(last + 1) : topic;
    }
}
