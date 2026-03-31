# /connectors — Integration configurations

This folder contains connector configurations for external systems.

## What goes here

Define which systems IQ should connect to and what data to sync:

```turtle
# Slack connector
slack:connector
  iq:connectorType "slack" ;
  iq:channels ("#engineering" "#incidents") ;
  iq:syncFrequency "PT4H" ;      # Every 4 hours
  iq:modeRead true ;             # Read messages
  iq:modeWrite true .            # Write messages
```

## Files included

- `slack-config.ttl` — Connect to Slack workspace
- `github-config.ttl` — Connect to GitHub repos
- `aws-config.ttl` — Connect to AWS account

## How to use

1. **Get credentials** (API key, token, password)
2. **Set environment variable:**
   ```bash
   export SLACK_BOT_TOKEN=xoxb-your-token
   ```
3. **Load connector config:**
   ```bash
   ./bin/setup-connectors connectors/
   ```
4. **Sync data:**
   ```bash
   iq> connector sync slack-connector
   ```

## Available connectors

| System | Token/Key | Setup time |
|---|---|---|
| Slack | `SLACK_BOT_TOKEN` | 10 min |
| GitHub | `GITHUB_TOKEN` | 10 min |
| AWS | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` | 15 min |
| Jira | `JIRA_TOKEN` | 15 min |
| Salesforce | `SALESFORCE_CLIENT_ID` + `CLIENT_SECRET` | 20 min |
| PostgreSQL | `DATABASE_URL` | 5 min |
| Snowflake | `SNOWFLAKE_ACCOUNT` + `WAREHOUSE` + token | 15 min |

See [../docs/CONNECTORS.md](../docs/CONNECTORS.md) for complete list and setup.

## Creating your own connector config

Template:

```turtle
@prefix my: <http://example.com/connectors/> .
@prefix iq: <http://iq.systems/> .

my:my-connector
  a iq:Connector ;
  iq:name "my-connector" ;
  iq:connectorType "slack" ;    # or "github", "aws", etc.
  iq:enabled true ;
  
  # What to sync
  iq:dataCapture (
    "messages"
    "metadata"
  ) ;
  
  # How often
  iq:syncFrequency "PT6H" ;      # Every 6 hours
  
  # Read and/or write
  iq:modeRead true ;
  iq:modeWrite true ;
  
  # Where the credentials are
  iq:credentialSource "env:MY_API_KEY" .
```

## Sync frequency

Use ISO 8601 duration format:

- `PT1H` — Every 1 hour
- `PT6H` — Every 6 hours
- `PT24H` or `P1D` — Every 24 hours
- `PT5M` — Every 5 minutes (careful with rate limits!)

## Tips

- **Test credentials first:** `iq> connector test slack-connector`
- **Monitor sync:** `iq> connector logs slack-connector`
- **Be careful with write permissions:** Only enable if you need to send data back
- **Rate limits:** Some APIs limit requests. Adjust sync frequency.

---

**Next:** Build workflows that use connector data. See [../agents/README.md](../agents/README.md)
