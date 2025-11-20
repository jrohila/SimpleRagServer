import { LLMMessage, LLMServiceConfig, StreamCallback } from './RemoteLLMService';
import { Platform } from 'react-native';

// Extend Navigator interface to include gpu property for WebGPU
declare global {
  interface Navigator {
    gpu?: any;
  }
}

// Feature flag - enable now with Vite bundler
const ENABLE_LOCAL_LLM = true;

export class LocalLLMService {
  private generator: any = null;
  private isInitialized = false;
  private isInitializing = false;
  private modelName = 'onnx-community/granite-4.0-micro-ONNX-web';

  async initialize(): Promise<void> {
    if (!ENABLE_LOCAL_LLM) {
      throw new Error('Local LLM is currently disabled due to bundling configuration. This feature will be available in a future update.');
    }

    if (this.isInitialized || this.isInitializing) {
      return;
    }

    this.isInitializing = true;

    try {
      // Check WebGPU availability
      if (!navigator.gpu) {
        throw new Error('WebGPU is not supported in this browser');
      }

      console.log('Initializing local LLM with WebGPU...');
      
      // Dynamic import to avoid bundling issues
      const { pipeline } = await import('@huggingface/transformers');
      
      this.generator = await pipeline(
        'text-generation',
        this.modelName,
        { device: 'webgpu' }
      );

      this.isInitialized = true;
      console.log('Local LLM initialized successfully');
    } catch (error) {
      console.error('Failed to initialize local LLM:', error);
      this.isInitializing = false;
      throw error;
    } finally {
      this.isInitializing = false;
    }
  }

  async sendMessage(
    config: LLMServiceConfig,
    messages: LLMMessage[],
    callbacks: StreamCallback
  ): Promise<void> {
    if (!ENABLE_LOCAL_LLM) {
      callbacks.onError(new Error('Local LLM is currently disabled due to bundling configuration'));
      return;
    }

    try {
      if (!this.isInitialized) {
        await this.initialize();
      }

      // Dynamic import
      const { TextStreamer } = await import('@huggingface/transformers');

      // Create a custom streamer that calls our callback
      const streamer = new TextStreamer(this.generator.tokenizer, {
        skip_prompt: true,
        skip_special_tokens: true,
        callback_function: (text: string) => {
          callbacks.onContent(text);
        }
      });

      // Generate response
      await this.generator(messages, {
        max_new_tokens: 2048, // Increased from 512 to 2048 tokens (~1600 words)
        temperature: config.temperature || 0.7,
        do_sample: config.temperature ? config.temperature > 0 : false,
        streamer,
      });

      callbacks.onComplete();
    } catch (error) {
      console.error('Error in local LLM generation:', error);
      callbacks.onError(error as Error);
    }
  }

  async isAvailable(): Promise<boolean> {
    // Check feature flag first
    if (!ENABLE_LOCAL_LLM) {
      return false;
    }
    // Check if WebGPU is available
    return !!navigator.gpu;
  }

  getModelName(): string {
    return this.modelName;
  }

  isModelLoaded(): boolean {
    return this.isInitialized;
  }
}
