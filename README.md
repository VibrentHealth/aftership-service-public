# About aftership-service
Aftership service will be used to track orders created for VRP System. For e.g. Aftership service will be used to track salivary kit sent by Genotek system.

# Responsibilities of Aftership service:
* Track orders created for VRP systems
* Expose webhooks to receive notifications from aftership.com for the updated tracking statuses
* Send the updated tracking statuses

# Dependencies
* Java 11
* Lombok

## Setup and Running
- [Installing Lombok for intelliJ](#-installing-lombok-for-intellij)
- [Setup Configuration on IntelliJ](#-setup-configuration-on-intellij)
- [Environment variables](#-environment-variables)

### Setup
This section describes how to add Lombok into the IDEA and describes all the environment variables and their default values.
 
#### Installing Lombok for intelliJ

If you are using intelliJ, need to activate annotations processor:
    Settings -> Compiler -> Annotation Processors

Now install lombok plugin:

    Preferences -> Plugins
    Click Browse repositories...
    Search for "Lombok Plugin"
    Install
    Restart IntelliJ
    
#### Setup Configuration on IntelliJ
    
    Main class:
    
    com.vibrent.aftership.AfterShipServiceApplication
    
    Use classpath of modules: navigate to aftership-service
        
#### Environment variables:
| Variable                          | Description                                                                                   | Default Value                                                                         |
| ------------------------------    |-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `DB_HOST`                    | Database host name                                                                            | `localhost`                                                                           |
| `DB_USERNAME` | Database username                                                                             | `root`                                                                                |
| `DB_PASSWORD` | Database password                                                                             | `password`                                                                            |
| `KAFKA_HOST` | URL of the Kafka server                                                                       | `_localhost:9092`                                                                     |
| `KAFKA_ENABLED` | Boolean variable to check if kafka server is enabled                                          | `true`                                                                                |
| `KAFKA_DEFAULT_CONCURRENCY` | Kafka topic concurrency                                                                       | `2`                                                                                   |
| `AFTERSHIP_BASE_URL` | Aftership base url                                                                            | `https://api.aftership.com/v4`                                                                                    |
| `AFTERSHIP_API_KEY` | Aftership API Key used to create trackings                                                    | `94363240-4b0a-4800-b97d-26c1d6348e81`                                                                                    | 
| `AFTERSHIP_WEB_HOOK_SECRET` | Aftership webhook secret key used to validate the realtime updates received for the trackings | `70d4576e2fab8ed24eabd1cb79fa14bf`                                                                                    | 
| `AFTERSHIP_RETRY_STATUS_CODES` | Status codes used to retry the tracking request using rock steady library                     | `408,429,503,504`                                                                     | 
| `MAX_RETRY_TRACKING_DELIVERY_COUNT` | Max Number of times service tries to create tracking with aftership.com                       | `3`                                                                                   | 
| `FETCH_TRACKING_BEFORE_DAYS` | Number of days service wait to fetch latest tracking status explicitly                        | `3`                                                                                   | 
| `AFTERSHIP_GET_TRACKING_EXCLUDE_STATUS` | Service will not fetch the latest status if status is one of the specified status             | `Delivered,Exception,Expired`                                                         | 
| `AFTERSHIP_EXCEPTION_SUB_STATUS` | Sub statuses to exclude while sending the track delivery response                             | `Exception_011,Exception_002,Exception_003,Exception_007,Exception_012,Exception_013` | 
| `RETRY_TRACKING_DELIVERY_CRON` | Cron expression to retry failed trackings                                                     | `0 0 0/1 ? * * *`                                                                     | 
| `GET_TRACKING_CRON` | Cron expression to fetch latest tracking status                                               | `0 0 13 * * ? *`                                                                      | 
| `SLIDING_WINDOW_SIZE` | The type of the sliding window used by the circuit-breaker.                                   | `20`                                                                                  | 
| `MINIMUM_NUMBER_OF_CALLS` | The minimum number of calls before circuit-breaker is on                                      | `21`                                                                                  | 
| `SLIDING_WINDOW_TYPE` | The type of the sliding window used by the circuit-breaker.                                   | `COUNT_BASED`                                                                         | 
| `WAIT_DURATION_IN_OPEN_STATE` | The wait duration in seconds before opening the app again.                                    | `300s`                                                                                | 
| `WAIT_DURATION` | The wait duration between each of the retries.                                                | `1s`                                                                                  | 
| `MAX_RETRY_ATTEMPTS` | The maximum number of retries before consider a request as a failure.                         | `3`                                                                                   | 

 
# Code Coverage
The application quality checks are enforced via code coverage rules in the build file [pom.xml](pom.xml)
To customize the code coverage exclusions refer to the [COVERAGE.md](COVERAGE.md) file

# Application Monitoring
Application generates logs and they are pushed to a common ELK stack. Please read the [monitoring guide](MONITORING.md) for more details.
    
---
