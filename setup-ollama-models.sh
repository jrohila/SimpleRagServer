#!/bin/bash

# Script to pull Ollama models
# Wait for Ollama service to be ready
echo "Waiting for Ollama service to be ready..."
sleep 10

# Set Ollama host
export OLLAMA_HOST=ollama:11434

# Pull the models you specified
echo "Pulling embeddinggemma:300m..."
ollama pull embeddinggemma:300m || echo "Warning: embeddinggemma:300m not found. Trying alternative embedding model..."

# If the above fails, try alternative embedding models
if ! ollama list | grep -q "embeddinggemma:300m"; then
    echo "Trying all-minilm as alternative embedding model..."
    ollama pull all-minilm:22m
fi

echo "Pulling granite3.3:2b..."
ollama pull granite3.3:2b || echo "Warning: granite3.3:2b not found. Trying granite3.1:2b..."

# If granite3.3:2b fails, try granite3.1:2b
if ! ollama list | grep -q "granite3.3:2b"; then
    echo "Trying granite3.1:2b as alternative..."
    ollama pull granite3.1:2b
fi

echo "Model setup complete!"
ollama list