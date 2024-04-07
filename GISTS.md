
```
export PROJECT_ID=iq-run-apis
mvn io.quarkus.platform:quarkus-maven-plugin:2.16.3.Final:create \
    -DprojectGroupId=systems.symbol \
    -DprojectArtifactId=$PROJECT_ID \
    -Dextensions='resteasy-reactive'
cd $PROJECT_ID

```