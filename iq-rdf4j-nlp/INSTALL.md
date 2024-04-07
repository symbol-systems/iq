## NLP - INSTALL

The NLP library is a machine learning toolkit for the processing of natural language text. 

It supports the most common NLP tasks, such as tokenization, sentence segmentation, part-of-speech tagging, named entity extraction, chunking, parsing, and coreference resolution. 

```
mkdir -p src/main/resources/nlp/
cd src/main/resources/nlp/
curl -O https://dlcdn.apache.org/opennlp/models/langdetect/1.8.3/langdetect-183.bin
curl -O https://dlcdn.apache.org/opennlp/models/ud-models-1.0/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin
curl -O https://dlcdn.apache.org/opennlp/models/ud-models-1.0/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin
curl -O https://dlcdn.apache.org/opennlp/models/ud-models-1.0/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin

ln -sf langdetect-183.bin langdetect.bin
ln -sf opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin opennlp-en-ud-ewt-sentence.bin
ln -sf opennlp-en-ud-ewt-pos-1.0-1.9.3.bin opennlp-en-ud-ewt-pos.bin
ln -sf opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin opennlp-en-ud-ewt-tokens.bin 

cd ../../../..
```

