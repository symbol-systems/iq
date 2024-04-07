# Workspace Properties

This file, named `workspace.properties`, is created to manage the configuration settings for a workspace. 

It is found within the workspace home folder (default `iq`).

It serves as the platform configuration record for the workspace, maintaining key settings related to repositories, imports, and other runtime attributes.

## Properties:

| Property            | Description                                                                                | Example Value                          |
|---------------------|--------------------------------------------------------------------------------------------|----------------------------------------|
| `current.ns`        | Represents the namespace associated with the workspace.                                    | `urn:fact:default:`                    |
| `current.repo`      | Denotes the currently active repository within the workspace.                              | `default`                              |
| `current.store`     | Indicates the type of store being used.                                                    | `default`                              |
| `iq.created`        | Records the timestamp when the workspace was initially created - in milliseconds.          | -                                      |
| `iq.modified`       | Reflects the timestamp when the workspace properties were last modified - in milliseconds. | -                                      |
| `importDefault.default`    | Specifies the `default` repository's import path for data lake onboarding.                 | `iq.api/import/dama/`                  |
| `index.default`     | Represents the `default` repository's SPARQL endpoint for indexing.                        | `urn:fact:default:indexer/skos.sparql` |
| `import.example`    | Specifies the `example` repository's import path for data lake onboarding.                 | `iq.api/import/dama/`                  |
| `index.example`     | Represents the `example` repository's SPARQL endpoint for indexing.                        | `urn:fact:default:indexer/skos.sparql` |

