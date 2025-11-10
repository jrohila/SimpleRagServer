import apiClient from './apiClient';

/**
 * OpenAI-compatible chat completions endpoint
 * @param {Object} params
 * @param {string} params.publicName - The public name from the chat entity
 * @param {Object} params.request - The OpenAI chat request object
 * @param {string} params.request.model - Model identifier
 * @param {Array<{role: string, content: string}>} params.request.messages - Array of message objects
 * @param {number} [params.request.temperature] - Sampling temperature (0-2)
 * @param {boolean} [params.request.stream=false] - Whether to stream the response
 * @param {number} [params.request.max_tokens] - Maximum tokens to generate
 * @param {boolean} [params.useRag=false] - Whether to use RAG (Retrieval Augmented Generation)
 * @returns {Promise} - Axios response promise
 */
export function createChatCompletion({ publicName, request, useRag = false } = {}) {
  const params = {};
  if (useRag !== undefined) {
    params.useRag = useRag;
  }

  return apiClient.post(`/${publicName}/v1/chat/completions`, request, { params });
}

/**
 * Helper function to format messages for OpenAI-compatible request
 * @param {Array<{role: string, content: string}>} messages - Array of messages
 * @returns {Array<{role: string, content: string}>} - Formatted messages
 */
export function formatMessages(messages) {
  return messages.map(msg => ({
    role: msg.role || 'user',
    content: msg.content
  }));
}

/**
 * Create a simple chat completion with a single user message
 * @param {Object} params
 * @param {string} params.publicName - The public name from the chat entity
 * @param {string} params.message - The user message
 * @param {string} [params.model='gpt-3.5-turbo'] - Model identifier
 * @param {number} [params.temperature=0.7] - Sampling temperature
 * @param {boolean} [params.stream=false] - Whether to stream the response
 * @param {number} [params.max_tokens] - Maximum tokens to generate
 * @param {boolean} [params.useRag=false] - Whether to use RAG
 * @returns {Promise} - Axios response promise
 */
export function sendMessage({ 
  publicName, 
  message, 
  model = 'gpt-3.5-turbo',
  temperature = 0.7,
  stream = false,
  max_tokens,
  useRag = false 
} = {}) {
  const request = {
    model,
    messages: [{ role: 'user', content: message }],
    temperature,
    stream
  };

  if (max_tokens) {
    request.max_tokens = max_tokens;
  }

  return createChatCompletion({ publicName, request, useRag });
}

/**
 * Create a chat completion with conversation history
 * @param {Object} params
 * @param {string} params.publicName - The public name from the chat entity
 * @param {Array<{role: string, content: string}>} params.messages - Array of conversation messages
 * @param {string} [params.model='gpt-3.5-turbo'] - Model identifier
 * @param {number} [params.temperature=0.7] - Sampling temperature
 * @param {boolean} [params.stream=false] - Whether to stream the response
 * @param {number} [params.max_tokens] - Maximum tokens to generate
 * @param {boolean} [params.useRag=false] - Whether to use RAG
 * @returns {Promise} - Axios response promise
 */
export function sendConversation({ 
  publicName, 
  messages, 
  model = 'gpt-3.5-turbo',
  temperature = 0.7,
  stream = false,
  max_tokens,
  useRag = false 
} = {}) {
  const request = {
    model,
    messages: formatMessages(messages),
    temperature,
    stream
  };

  if (max_tokens) {
    request.max_tokens = max_tokens;
  }

  return createChatCompletion({ publicName, request, useRag });
}
