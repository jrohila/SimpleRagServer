import { LLMMessage } from './RemoteLLMService';
import config from '../config/ui';

export interface ProcessMessagesRequest {
  publicName: string;
  messages: Array<{
    role: 'system' | 'user' | 'assistant';
    content: string;
  }>;
  maxContextLength: number;
  completionLength: number;
  headroomLength: number;
}

export type ProcessMessagesResponse = Array<{
  role: string;
  content: string;
}>;

export class WebGpuMessageService {
  /**
   * Process messages through backend to add RAG context before sending to WebGPU LLM
   */
  async processMessagesForWebGPU(
    publicName: string,
    messages: LLMMessage[],
    maxContextLength: number = config.WEBGPU.maxContextLength,
    completionLength: number = config.WEBGPU.completionLength,
    headroomLength: number = config.WEBGPU.headroomLength
  ): Promise<ProcessMessagesResponse> {
    try {
      const response = await fetch(`${config.WEBGPU.apiBase}/process-messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          publicName,
          messages: messages.map(m => ({
            role: m.role,
            content: m.content
          })),
          maxContextLength,
          completionLength,
          headroomLength
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
