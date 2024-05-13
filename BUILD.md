# Maven Build Guide

IQ comprises a set of integrated modules handling data, communication and interaction among humans and AI.

### The build system steps:

1. **Clean and Install:**

`mvn clean install`
- cleans the project, then compiles the source code, runs tests, and packages the project to ensure a fresh and consistent build.

2. **Compile Only:**

`mvn compile`
- compile the source code. Helpful during the development phase for faster feedback on code changes.

3. **Run Tests:**

`mvn test`
- Compile then execute the tests without packaging the project. Useful for running unit tests and ensuring code integrity.

4. **Generate Javadoc:**

`mvn javadoc:javadoc`
- Generate Javadoc documentation for the project. This is helpful for developers looking to extend or improve the code.

5. **Dependency Updates:**

`mvn versions:display-dependency-updates`
- Identify any available updates for project dependencies. Useful for keeping dependencies up-to-date and addressing security or functionality improvements.

6. **Dependency Tree:**

`mvn dependency:tree`
- Display the project's dependency tree, showing the hierarchy of dependencies. Useful for understanding the project's dependencies and resolving any conflicts.

7. **Package as JAR:**

`mvn package`
- Package the compiled code into a JAR file without running tests. Useful for creating distributable artifacts.

8. **API Platform:**

`mvn compile quarkus:dev -pl iq-run-apis -am`
- Run the Quarkus in development mode for live coding and testing. 

