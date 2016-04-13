## Spring Session Example Application Using Pivotal GemFire for Session Management.

This example demonstrates a highly simple _Spring Boot_-based _Spring Session_ Application using _Pivotal GemFire_ 
to manage (HTTP) Sessions.
  
### Requirements

* Java Runtime Environment (JRE) 1.8
* Gradle 2.2 (or higher)

### Build

```bash
$ gradlew clean build install
```

### Run

First, you must setup a GemFire cluster to which the example application (`SpringSessionDataGemFireClient`) 
will connect.  This example conveniently provides the `src/main/resources/gemfire-cluster-setup.gfsh` 
GemFire Shell (_Gfsh_) script to start the GemFire cluster.

The GemFire cluster consists of 1 GemFire _Locator_ ("`LocatorX`") and one GemFire _Server_ ("`ServerA`").  The _Gfsh_
shell script will create the necessary "`ClusteredSpringSessions`" `PARTITION` _Region_ along with configuring
entry-idle-timeout-expiration, set to timeout after 1 second using an action of `INVALIDATE`.

To call the setup _Gfsh_ shell script, run the following command in _Gfsh_...

```bash
gfsh> run --file=/path/to/spring-session-data-gemfire-example/src/main/resources/gemfire-cluster-setup.gfsh
```
The _Gfsh_ shell script will also list and describe the members and Region to show the configuration.  Once the 
GemFire Cluster is up and running, you can run the `SpringSessionDataGemFireClient` application class.

The application configures a GemFire cache client, connecting directly to the server on "*localhost*", port "*12480*".
The application uses the _Spring Session Data GemFire_ `GemFireOperationsSessionRepository` to create and save a 
new Session and then asserts that it has been successfully stored in the `ClusteredSpringSessions` _Region_ unaltered.
