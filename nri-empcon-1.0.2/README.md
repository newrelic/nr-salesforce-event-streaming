
# NRI-EMPCON Integration for SalesForce Streaming API

This integration subscribes to and replays SalesForce streaming events using EMP Connector.
The event will be transformed and ingested into New Relic Insight.

Refer to the following links for more details.

- [SalesForce Streaming API ](https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/intro_stream.htm)
- [EMP Connector](https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/code_sample_java_client_intro.htm)


## 1. Prerequisite

- JRE 1.8+
- New Relic Information
  1. account ID and Insight insert key
- SalesForce Information
  1.  login username, password and security token for the username
  2.  Topic Channel
- Network Information
  1. Proxy username and password (if required)

## 2. Installation

#### Unzip the tar file to an existing directory

```
# mkdir -p /app/nri-empcon
# cd /app/nri-empcon
# tar xvf nri-empcon-<VERSION>.tar

```

## 3. Configuration

Configure the following setting in config/empcon-config.yml

- New Relic Insight Information

  1. insights_url
  2. insights_insert_key

- SalesForce Connection Information

  > (Multi instances are supported, see the sample empcon-config.yml for reference)

  1. url  
     Your Salesforce instance URL
  2. username and password  
     username - your SalesForce '<username>'  
     password - your SalesForce '<passwrod><security token for the username>'  
  3. channel  
     The SalesForce topic to be subscribed to
  4. replayfrom
     - `-1` replay event from the tip (new event)  
     - `-2` replay event from the earliest (in last 24 hours)  
  5. any additional labels/attributes (optional)
  6. eventtype  
     The New Relic Insight event type of your choice for this instance

- Network Proxy (if required)
  1. proxy_host
  2. proxy_user
  3. proxy_password

### 3.1 Password Obfuscation

- Password obfuscatoin is supported for `proxy_password` and `password`

  - use `proxy_password_obfuscated` and `password_obfuscated` in place of `proxy_password` and `password` in the config file

- Use the following command line utility to generate obfucated value for the above password

  ```
  # newrelic agent config obfuscate --key '<your obfucation key>' --value '<the password>'
  ```

  The documentation and download instruction for the command utility are available at below link:  
  [Commandline utility](https://tinyurl.com/y6a3r9ve)

- Update the values for `proxy_password_obfuscated` and `password_obfuscated` in the config file  
- Update nri-empcon.sh with 'your obfucation key'

```shell
  export NEW_RELIC_CONFIG_OBSCURING_KEY="<your obfucation key>"
```

## 4. Running the Integration

### 4.1 Start the integration

```
#cd /app/nri-empcon
#./nri-empcon.sh start
#./nri-empcon.sh status (check the status, make sure it is running)

```

### 4.2 Stop the integration

```
#cd /app/nri-empcon
#./nri-empcon.sh stop

```

### 4.3 Check the status of the integration

```
#cd /app/nri-empcon
#./nri-empcon.sh status

```

### 4.4 Log file

- logs/plugin.log
- logs/plugin.err

## 5. Troubleshooting

- Look for `c.n.fit.empcon.MonitoringConsumer - ==>subscribed:` in the logs/plugin.log file. If found, it indicates the integration has successfully subscribed to the channel. If you have multiple instances defined in the configue file, you should expect multiple `c.n.fit.empcon.MonitoringConsumer - ==>subscribed:` message in the log file.
- If `c.n.fit.empcon.MonitoringConsumer - ==>subscribed:` cannot be found in the log file, check logs/plugin.err for detail why the integration is unable to subscrsibe to the channel defined.

---

> Sample empcon-config.yml

```yml
integration_name: com.newrelic.empcon
insights_url: https://insights-collector.newrelic.com/v1/accounts/<New Relic account ID>/events
insights_insert_key: <New Relic Insight insert key>

proxy_host: abc123.compute.amazonaws.com
proxy_port: 3128
proxy_user: squid
proxy_password: abc123

instances:
  - name: empcon-salesforce-hh1
    eventtype: empevent1
    connectioninfo:
      url: https://myabc123-dev-ed.my.salesforce.com
      username: abc123@something.domain
      password: <password><security token>
      channel: /topic/InvoiceStatementUpdates
      replayfrom: -1

    labels:
      env: production1
      role: empRole1

  - name: empcon-salesforce-hh2
    eventtype: empevent2
    connectioninfo:
      url: https://myxyz123-dev-ed.my.salesforce.com
      username: xyz@something.domain
      password: <password><security token>
      channel: <your topic channel>
      replayfrom: -1

    labels:
      env: production2
      role: stock2
```

> Sample empcon-config.yml with password obfucation

```yml
integration_name: com.newrelic.empcon
insights_url: https://insights-collector.newrelic.com/v1/accounts/<New Relic account ID>/events
insights_insert_key: <New Relic Insight insert key>

proxy_host: abc123.compute.amazonaws.com
proxy_port: 3128
proxy_user: squid
proxy_password_obfuscated: <obfuscated password>

instances:
  - name: empcon-salesforce-hh1
    eventtype: empevent1
    connectioninfo:
      url: https://myabc123-dev-ed.my.salesforce.com
      username: abc123@something.domain
      password_obfuscated: <obfuscated password>
      channel: /topic/InvoiceStatementUpdates
      replayfrom: -1

    labels:
      env: production1
      role: empRole1
```

> Sample SalesForce event

```json
{
    "event": {
        "createdDate": "2021-01-22T01:40:40.736Z",
        "replayId": 196,
        "type": "updated"
    },
    "sobject": {
        "Description__c": "new lead",
        "Id": "a005g000001WGfMAAW",
        "Status__c": "Open",
        "Name": "INV-0002"
    }
}
```

> Sample New Relic Insight Event

```json
{
  "event": {
    "channel": "/topic/InvoiceStatementUpdates",
    "env": "production1",
    "event_createdDate": "2021-01-22T01:40:40.736Z",
    "event_replayId": "196",
    "event_type": "updated",
    "relayfrom": -1,
    "role": "empRole1",
    "sobject_Description__c": "new lead",
    "sobject_Id": "a005g000001WGfMAAW",
    "sobject_Name": "INV-0002",
    "sobject_Status__c": "Open",
    "timestamp": 1611279642248,
    "url": "https://myabc123-dev-ed.my.salesforce.com",
    "username": "abc123@something.domain"
  }
}
```
