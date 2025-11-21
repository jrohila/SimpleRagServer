import { LLMMessage } from './RemoteLLMService';

export interface ProcessMessagesRequest {
  publicName: string;
  messages: Array<{
    role: 'system' | 'user' | 'assistant';
    content: string;
  }>;
}

export type ProcessMessagesResponse = Array<{
  role: string;
  content: string;
}>;

const API_BASE_URL = 'http://localhost:8080/api/webgpu';

export class WebGpuMessageService {
  /**
   * Process messages through backend to add RAG context before sending to WebGPU LLM
   */
  async processMessagesForWebGPU(
    publicName: string,
    messages: LLMMessage[]
  ): Promise<ProcessMessagesResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/process-messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          publicName,
          messages: messages.map(m => ({
            role: m.role,
            content: m.content
          }))
        } as ProcessMessagesRequest),
      });

      if (!response.ok) {
        throw new Error(`Failed to process messages: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error processing messages for WebGPU:', error);
      throw error;
    }
  }
}
