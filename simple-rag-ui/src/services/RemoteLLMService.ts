import { sendConversation } from '../api/openAI';

export interface LLMMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

export interface LLMServiceConfig {
  publicName: string;
  temperature?: number;
  useRag?: boolean;
}

export interface StreamCallback {
  onContent: (content: string) => void;
  onComplete: () => void;
  onError: (error: Error) => void;
}

export class RemoteLLMService {
  async sendMessage(
    config: LLMServiceConfig,
    messages: LLMMessage[],
    callbacks: StreamCallback
  ): Promise<void> {
    try {
      // Limit to 10 most recent messages to avoid context overflow
      // Backend adds system messages (RAG context, memory, etc.), so we only send conversation history
      const MAX_MESSAGES = 10;
      const conversationMessages = messages.filter(m => m.role !== 'system');
      
      let limitedMessages = conversationMessages;
      if (conversationMessages.length > MAX_MESSAGES) {
        // Keep only the most recent messages
        limitedMessages = conversationMessages.slice(-MAX_MESSAGES);
        console.log(`Limited conversation history from ${conversationMessages.length} to ${MAX_MESSAGES} messages (system messages will be added by backend)`);
      } else {
        console.log(`Sending ${conversationMessages.length} conversation messages (system messages will be added by backend)`);
      }

      const response = await sendConversation({
        publicName: config.publicName,
        messages: limitedMessages,
        stream: true,
        temperature: config.temperature || 0.7,
        useRag: config.useRag ?? true
      });

      if (!response.body) {
        throw new Error('Response body is not available');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let reading = true;

      while (reading) {
        try {
          const { done, value } = await reader.read();
          
          if (done) {
            reading = false;
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            const trimmedLine = line.trim();
            
            if (trimmedLine.startsWith('data:')) {
              const data = trimmedLine.slice(5).trim();
              
              if (data === '[DONE]') {
                reading = false;
                break;
              }
              
              try {
                const parsed = JSON.parse(data);
                const content = parsed.choices?.[0]?.delta?.content;
                
                if (content) {
                  callbacks.onContent(content);
                }
              } catch (parseError) {
                console.error('Error parsing streaming chunk:', parseError);
              }
            }
          }
        } catch (readError) {
          console.error('Error reading stream:', readError);
          reading = false;
          break;
        }
      }

      callbacks.onComplete();
    } catch (error) {
      callbacks.onError(error as Error);
    }
  }

  async isAvailable(): Promise<boolean> {
    return true; // Remote service is always available
  }
}
