package com.newrelic.fit.config;

import java.util.HashMap;
import java.util.Map;

public class Instance {

	private String url = null;
	private String username = null;
	private String password = null;
	private String channel = null;
	private Long relayfrom = -1L;
	private String eventtype="EMPEvent";

    
    public String getEventtype() {
		return eventtype;
	}

	public void setEventtype(String eventtype) {
		this.eventtype = eventtype;
	}

	private static String insightsUrl = null;
    private static String insightsInsertKey = null;
    private static String proxyHost = null;
    private static int proxyPort = 0;
    private static String proxyUser = null;
    private static String proxyPassword = null;
    
    private Map<String, Object> labels = new HashMap<String, Object>();



	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public long getRelayfrom() {
		return relayfrom;
	}

	public void setRelayfrom(Long relayfrom) {
		this.relayfrom = relayfrom;
	}

	public static String getInsightsUrl() {
		return insightsUrl;
	}

	public static void setInsightsUrl(String insightsUrl) {
		Instance.insightsUrl = insightsUrl;
	}

	public static String getInsightsInsertKey() {
		return insightsInsertKey;
	}

	public static void setInsightsInsertKey(String insightsInsertKey) {
		Instance.insightsInsertKey = insightsInsertKey;
	}

	public static String getProxyHost() {
		return proxyHost;
	}

	public static void setProxyHost(String proxyHost) {
		Instance.proxyHost = proxyHost;
	}

	public static int getProxyPort() {
		return proxyPort;
	}

	public static void setProxyPort(int proxyPort) {
		Instance.proxyPort = proxyPort;
	}

	public static String getProxyUser() {
		return proxyUser;
	}

	public static void setProxyUser(String proxyUser) {
		Instance.proxyUser = proxyUser;
	}

	public static String getProxyPassword() {
		return proxyPassword;
	}

	public static void setProxyPassword(String proxyPassword) {
		Instance.proxyPassword = proxyPassword;
	}

	
	@Override
	public String toString() {
		return "Instance [url=" + url + ", username=" + username + ", channel=" + channel
				+ ", relayfrom=" + relayfrom + ", proxy="+ Instance.proxyHost+"]";
	}
	

	public void addLabel(String k, Object v) {
		this.labels.put(k, v);
	}

	public Map<String, Object> getLabels() {
		return labels;
	}
}
