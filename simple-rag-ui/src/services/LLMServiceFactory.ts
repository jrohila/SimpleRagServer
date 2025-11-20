import { RemoteLLMService, LLMMessage, LLMServiceConfig, StreamCallback } from './RemoteLLMService';
import { LocalLLMService } from './LocalLLMService';

export type LLMMode = 'remote' | 'local';

export interface ILLMService {
  sendMessage(
    config: LLMServiceConfig,
    messages: LLMMessage[],
    callbacks: StreamCallback
  ): Promise<void>;
  isAvailable(): Promise<boolean>;
}

export class LLMServiceFactory {
  private static remoteLLM = new RemoteLLMService();
  private static localLLM = new LocalLLMService();

  static getService(mode: LLMMode): ILLMService {
    return mode === 'local' ? this.localLLM : this.remoteLLM;
  }

  static getLocalLLM(): LocalLLMService {
    return this.localLLM;
  }

  static async checkLocalAvailability(): Promise<boolean> {
    return this.localLLM.isAvailable();
  }

  static async clearLocalModelCache(): Promise<void> {
    return this.localLLM.clearModelCache();
  }

  static async getLocalModelCacheSize(): Promise<number> {
    return this.localLLM.getModelCacheSize();
  }
}
