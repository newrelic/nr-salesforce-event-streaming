[![New Relic Community header](https://opensource.newrelic.com/static/Community_Project-0c3079a4e4dbe2cbd05edc4f8e169d7b.png)](https://opensource.newrelic.com/oss-category/#new-relic-community)

![GitHub forks](https://img.shields.io/github/forks/newrelic/nr-salesforce-event-streaming?style=social)
![GitHub stars](https://img.shields.io/github/stars/newrelic/nr-salesforce-event-streaming?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/newrelic/nr-salesforce-event-streaming?style=social)

![GitHub all releases](https://img.shields.io/github/downloads/newrelic/nr-salesforce-event-streaming/total)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/newrelic/nr-salesforce-event-streaming)
![GitHub last commit](https://img.shields.io/github/last-commit/newrelic/nr-salesforce-event-streaming)
![GitHub Release Date](https://img.shields.io/github/release-date/newrelic/nr-salesforce-event-streaming)

![GitHub issues](https://img.shields.io/github/issues/newrelic/nr-salesforce-event-streaming)
![GitHub issues closed](https://img.shields.io/github/issues-closed/newrelic/nr-salesforce-event-streaming)
![GitHub pull requests](https://img.shields.io/github/issues-pr/newrelic/nr-salesforce-event-streaming)
![GitHub pull requests closed](https://img.shields.io/github/issues-pr-closed/newrelic/nr-salesforce-event-streaming)


# NRI-EMPCON Integration for SalesForce Streaming API
This integration subscribes to and replays SalesForce streaming events using EMP Connector.
The event will be transformed and ingested into New Relic Insight.

Refer to the following links for more details.

- [SalesForce Streaming API ](https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/intro_stream.htm)
- [EMP Connector](https://developer.salesforce.com/docs/atlas.en-us.api_streaming.meta/api_streaming/code_sample_java_client_intro.htm)

#### Supported Salesforce Authentication Method
- Basic username and password
- [OAuth 2.0 Client Credentials Flow](https://help.salesforce.com/s/articleView?id=sf.connected_app_client_credentials_setup.htm&type=5)


# Docker Deployment Option

### 1. Config env variables  in `envfile.txt` file

```
### Use env variables for configuration
NR_SF_ENV_CONF=true

### New Relic connection info
NR_SF_INSIGHTS_URL=https://insights-collector.newrelic.com/v1/accounts/<YOUR New Relic RPM ID>/events
NR_SF_INSIGHTS_INSERT_KEY=<YOUR New Relic Insert Key>

### Salesforce connection info
NR_SF_URL=https://<YOUR Salesforce instance>.my.salesforce.com
NR_SF_TOPICS=/event/LoginEventStream,/event/LogoutEventStream,/event/LightningUriEventStream,/event/UriEventStream

###Authentication#### 
###option 1: basic username and password
NR_SF_USERNAME=<YOUR Salesforce login id>
### Use NR_SF_PASSWORD_OBFUSCATED(preferred) or NR_SF_PASSWORD (for testing)
NR_SF_PASSWORD_OBFUSCATED=<YOUR obfuscated password>
#NR_SF_PASSWORD=<Clear password> 

###option2: Salesforce OAuth2 Client Credentials Flow
NR_SF_CLIENT_ID=<YOUR APP CLIENT ID>
NR_SF_CLIENT_SECRET=<YOUR APP CLIENT SECRET>
###Instructions on how to setup Oauth2 Client Credentials Flow
###https://help.salesforce.com/s/articleView?id=sf.connected_app_client_credentials_setup.htm&type=5


### Password obfuscation key
NEW_RELIC_CONFIG_OBSCURING_KEY=<YOUR Password Obfuscation Key>

### how to generate NR_SF_PASSWORD_OBFUSCATED #####
# newrelic agent config obfuscate --key '<your obfuscation key>' --value '<YOUR Salesforce password>'
###################################################

### Proxy configuration(optional)
# NR_SF_PROXY_HOST: <YOUR network proxy host>
# NR_SF_PROXY_PORT: <YOUR network proxy port>
# NR_SF_PROXY_USER: <YOUR network proxy user>
####Use NR_SF_PROXY_PASSWORD_OBFUSCATED(preferred) or NR_SF_PROXY_PASSWORD (for testing)
# NR_SF_PROXY_PASSWORD_OBFUSCATED: <YOUR network obfuscated password>
# NR_SF_PROXY_PASSWORD: <YOUR network obfuscated password>

### labels (optional)
NR_SF_LABELS=source=NR_Salesforce_Event_Streaming,env=production

```


### 2. Start your docker image
`docker run --env-file envfile.txt -d haihongren/nr-salesforce-event-streaming:1.1.1`

#### 2.1 check logs 
`docker logs <container id>` 

Look for `... INFO ==>subscribed: ....` message to confirm the integration is able to connect and subscribe to the topics successfully. 
```
 23:42:18 INFO ==>subscribed: https://<YOUR SF Instance>.my.salesforce.comSubscription [/event/LoginEventStream:-1]
 23:42:19 INFO ==>subscribed: https://<YOUR SF Instance>.my.salesforce.comSubscription [/event/LogoutEventStream:-1]
 23:42:21 INFO ==>subscribed: https://<YOUR SF Instance>.my.salesforce.comSubscription [/event/LightningUriEventStream:-1]
 23:42:22 INFO ==>subscribed: https://<YOUR SF Instance>.my.salesforce.comSubscription [/event/UriEventStream:-1]
```

### 3. check the new event types in New Relic

The integation creates new event type using `SF`+`<Topic Name>` for the event type name. 
With the default topic setting(NR_SF_TOPICS), the following new event types will be created.

```
SFLoginEventStream
SFLogoutEventStream
SFLightningUriEventStream
SFUriEventStream
```

# Host Based Deployment Option

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

#### Download and unzip the [package nri-empcon-*.tar.gz](https://github.com/newrelic-experimental/nr-salesforce-event-streaming/releases)  to an existing directory

```
# mkdir -p /app/nri-empcon
# cd /app/nri-empcon
# tar xvfz nri-empcon-<VERSION>.tar.gz

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

## 6. Development/Building
- Clone the repo
- Build 
```
./gradlew clean build
```

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
    "replayfrom": -1,
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

## Contributing

We encourage your contributions to improve [Project Name]! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. If you have any questions, or to execute our corporate CLA, required if your contribution is on behalf of a company, please drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

## License

nr-salesforce-event-streaming is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

