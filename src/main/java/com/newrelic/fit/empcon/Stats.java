package com.newrelic.fit.empcon;

import com.newrelic.fit.config.Instance;
import com.newrelic.insights.publish.Event;
import com.newrelic.insights.publish.InsightsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Stats {
    private static Logger logger = LoggerFactory.getLogger(MonitoringConsumer.class);
    private final  String SF_STATS_EVENT="SFStatsSample";
    private long startTime = System.currentTimeMillis();
    private long messageProcessed = 0;
    private Instance instance = null;
    private InsightsClient insightsClient = null;
    public Stats(Instance instance, InsightsClient client) {
        this.instance=instance;
        this.insightsClient=client;
    }

    public void incrMessageCount() {
        messageProcessed++;
    }

    public long getMessageCount() {
        return messageProcessed;
    }
    public Event[] getStats(){

        Event nrevent = Event.create(SF_STATS_EVENT);
        nrevent.put("messageProcessed", messageProcessed);
        nrevent.put("startTime", startTime);
        nrevent.put("topic",instance.getChannel());
        nrevent.put("eventTypeName",instance.getEventtype());
        Event[] events={nrevent};
        return events;
    }
    public boolean postStats(){
        boolean result = insightsClient.post(getStats());
        if (!result) {
            logger.error("Unable to post events to Insights");
        }else{
            logger.debug("Stats Posted successfully! "+instance.getChannel()+":"+instance.getEventtype());
        }
        return result;
    }
}
