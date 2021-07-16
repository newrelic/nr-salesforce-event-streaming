package com.newrelic.fit.empcon;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.newrelic.infra.publish.security.Obfuscator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.newrelic.fit.config.Instance;

public class ConfigParse {
	
	private static Logger logger = LoggerFactory.getLogger(ConfigParse.class);

	public static List<Instance> readConfig(String configFile) {
	    List<Instance> configuredInstances = new LinkedList<Instance>();
		FileInputStream is;
		try {
			is = new FileInputStream(configFile);
			Yaml yaml= new Yaml();
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
				for (Map instanceParser: instancesParser) {
					Instance inst = new Instance();
					configuredInstances.add(inst);
					
					if (instanceParser.containsKey("connectioninfo")) {
						Map	argumentsParser = (Map) instanceParser.get("connectioninfo");
					
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
							String password=Obfuscator.deobfuscateNameUsingKey((String) argumentsParser.get("password_obfuscated"));
							inst.setPassword(password);
						}

						if (argumentsParser.containsKey("channel")) {
							inst.setChannel((String) argumentsParser.get("channel"));
						}
						if (argumentsParser.containsKey("replayfrom")) {
							Integer replayfrom=(Integer)argumentsParser.get("replayfrom");
							inst.setRelayfrom(replayfrom.longValue());
						
						}

						if (instanceParser.containsKey("labels")) {
							Map<String, Object>	labelsParser = (Map<String, Object>) instanceParser.get("labels");
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
}
