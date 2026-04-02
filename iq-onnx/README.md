# iq-onnx — Local ONNX Model Inference

`iq-onnx` brings local on-device AI model execution to IQ. It uses the ONNX Runtime to run embedding models, cross-encoder rerankers, and zero-shot classifiers — without sending data to an external API.

This is particularly useful for semantic search, document reranking, and intent classification where you want fast, private, cost-free inference running alongside the rest of IQ.

## What it provides

- **Embedding models** — convert text segments into dense vector representations for semantic similarity and retrieval
- **Cross-encoder reranker** — reranks a set of candidate results relative to a query for improved precision compared to embedding-only retrieval
- **Zero-shot classification** — classify text into categories without task-specific training, using a locally loaded ONNX model
- **Text segment handling** — splits and prepares documents for embedding and inference
- **EmbeddingMatch** — pairs a text segment with its similarity score for ranked retrieval results

## Role in the system

`iq-onnx` is an optional inference backend. Use it when you want semantic capabilities without external API calls.

## Requirements

- Java 21
- ONNX Runtime (included as a Maven dependency)
- A compatible ONNX model file for your chosen task
- Part of the IQ mono-repo; build with `./mvnw -pl iq-onnx -am compile`
