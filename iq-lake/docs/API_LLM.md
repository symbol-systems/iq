# OpenAI API Documentation

## Overview
The OpenAI API provides an endpoint for answering LLM (Large Language Model) queries using OpenAI Language Models. 

Users can interact with this API to obtain responses from language models based on specified parameters.

## Endpoint

```
GET /llm/openai/{namedMap}/{repo}/{promptPath: .*}?query=
```

### Endpoint Example

```
GET /llm/openai/gpt-3.5-turbo/default/prompt/pirate?query=hello
```

### Configurable API Parameters

| Parameter   | Description                                | Usage  |
|-------------|--------------------------------------------|--------|
| namedMap       | The language namedMap to use.                 | path   |
| repo        | The repository to fetch data from.         | path   |
| promptPath  | The promptPath for the prompt query        | path   |
| query       | The human question.                        | query   |
| maxTokens   | Maximum number of tokens for the response. | query  |
| temperature | Controls the randomness of the namedMap.      | query  |
| auth        | Authorization token in Bearer format.      | header |

### Response
The API responds with a JSON payload containing the language namedMap results.

### Example  Request

```
GET /llm/openai/gpt-3.5-turbo/default/prompt/pirate?query=hello+world
```

## Authentication
Ensure that you include the proper authorization token in the Authorization header using the OAuth Bearer format.

## Usage Guidelines

- Provide valid values for the required parameters (namedMap, repo, prompt).
- Review the optional parameters (query, maxTokens) to customize the API response.
- Use a valid Bearer token for authentication.
- Handle the JSON response from the API in your application logic.
- For more details on available language models, refer to the OpenAI documentation.

## Errors
If an error occurs, the API will respond with an appropriate HTTP status code and an error message in the JSON format.

