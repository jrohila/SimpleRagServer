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

echo "Pulling ibm/granite4:tiny-h..."
ollama pull ibm/granite4:tiny-h || echo "Warning: ibm/granite4:tiny-h not found. Trying ibm/granite4:tiny-hf..."

# If ibm/granite4:tiny-h fails, try ibm/granite4:tiny-hf
if ! ollama list | grep -q "ibm/granite4:tiny-h"; then
    echo "Trying ibm/granite4:tiny-hf as alternative..."
    ollama pull ibm/granite4:tiny-hf
fi

echo "Pulling ibm/granite4:micro-h..."
ollama pull ibm/granite4:micro-h || echo "Warning: ibm/granite4:micro-h not found. Trying ibm/granite4:micro-hf..."

# If ibm/granite4:micro-h fails, try ibm/granite4:micro-hf
if ! ollama list | grep -q "ibm/granite4:micro-h"; then
    echo "Trying ibm/granite4:micro-hf as alternative..."
    ollama pull ibm/granite4:micro-hf
fi

echo "Model setup complete!"
ollama list