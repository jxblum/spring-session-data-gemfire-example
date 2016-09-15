## Spring Session Example Application Using Pivotal GemFire for Session Management.

This example demonstrates a highly simple _Spring Boot_-based _Spring Session_ Application using _Pivotal GemFire_ 
to manage (HTTP) Sessions.
  
### Requirements

* Java Runtime Environment (JRE) 1.8
* Gradle 2.2 (or higher)

### Build

```bash
$ gradlew clean build install -x test
```

### Run

First, you must setup a GemFire cluster to which the example _Spring Boot_, GemFire cache client application,
using _Spring Session_ (`SpringSessionDataGemFireCacheClient`) will connect.  This example conveniently provides 
the `src/main/resources/gemfire-cluster-setup.gfsh` GemFire Shell (_Gfsh_) script to start the GemFire cluster.

The GemFire cluster consists of 1 GemFire _Locator_ ("`LocatorX`") and 1 GemFire _Server_ ("`ServerA`").
The _Gfsh_ shell script will create the necessary "`ClusteredSpringSessions`" `PARTITION` _Region_ as well as 
configure the _Region's_ `entry-idle-timeout-expiration`, which is set to timeout after 1 second 
using an action of `INVALIDATE`.

To call the setup _Gfsh_ shell script, run the following command in _Gfsh_...

```bash
gfsh> run --file=/path/to/spring-session-data-gemfire-example/src/main/resources/gemfire-cluster-setup.gfsh
```
The _Gfsh_ shell script will also list and describe the members and _Region_ to show the configuration.  Once the 
GemFire Cluster is up and running, you can run the `SpringSessionDataGemFireClient` application class.

The _Spring Boot_ application configures a GemFire cache client, connecting directly to the server 
on "*localhost*", port "*40404*".  The application uses the _Spring Session Data GemFire_ 
`GemFireOperationsSessionRepository` to create and save a new Session and then asserts that 
it has been successfully stored in the `ClusteredSpringSessions` _Region_ unaltered.

This example additionally provides a _Spring Boot_ Web application (`SpringBootWebApplicationWithSpringSessionDataGemFireEnabled`) 
that configures and bootstraps a GemFire cache client in the webapp using _Spring Session_ backed by GemFire 
to manage the webapp's `HttpSession`.  The webapp provides a couple _Spring_ Web MVC endpoints that you can access
from within your browser (or CURL).

Also, rather than externally (from your IDE) configure the GemFire cluster using and executing the provided
_Gfsh_ shell script, a `SpringBootGemFireServer` application class has been provided to launch the GemFire Server.
However, you may want to tweak the `maxInactiveIntervalInSeconds` attribute when interacting with the Webapp, manually.
