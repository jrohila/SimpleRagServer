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
  private downloadProgressCallback?: (progress: { loaded: number; total: number; percentage: number }) => void;

  setDownloadProgressCallback(callback: (progress: { loaded: number; total: number; percentage: number }) => void) {
    this.downloadProgressCallback = callback;
  }

  private async checkModelCache(): Promise<boolean> {
    try {
      // Check if model is in browser cache
      const cacheKeys = await caches.keys();
      const modelCacheExists = cacheKeys.some(key => key.includes('transformers') || key.includes('onnx'));
      
      if (modelCacheExists) {
        console.log('Model found in browser cache');
        return true;
      }
      
      console.log('Model not found in cache, will download');
      return false;
    } catch (error) {
      console.error('Error checking cache:', error);
      return false;
    }
  }

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

      // Check if model is already cached
      const isCached = await this.checkModelCache();
      if (isCached) {
        console.log('Loading local LLM from cache...');
      } else {
        console.log('Downloading local LLM (this may take a few minutes on first load)...');
      }
      
      // Dynamic import to avoid bundling issues
      const { pipeline, env } = await import('@huggingface/transformers');
      
      // Configure transformers.js to use browser cache
      // @ts-ignore
      env.useBrowserCache = true;
      // @ts-ignore
      env.allowLocalModels = false;
      // @ts-ignore
      env.allowRemoteModels = true;

      // Set up progress callback if available
      let lastProgress = 0;
      const progressCallback = (progress: any) => {
        if (progress.status === 'progress' && progress.file) {
          const percentage = Math.round((progress.loaded / progress.total) * 100);
          // Only update on significant changes (every 5%)
          if (percentage - lastProgress >= 5) {
            lastProgress = percentage;
            console.log(`Downloading ${progress.file}: ${percentage}%`);
            if (this.downloadProgressCallback) {
              this.downloadProgressCallback({
                loaded: progress.loaded,
                total: progress.total,
                percentage
              });
            }
          }
        }
      };

      this.generator = await pipeline(
        'text-generation',
        this.modelName,
        { 
          device: 'webgpu',
          progress_callback: progressCallback
        }
      );

      this.isInitialized = true;
      console.log('Local LLM initialized successfully - model cached for future use');
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

  async clearModelCache(): Promise<void> {
    try {
      // Clear all transformers-related caches
      const cacheKeys = await caches.keys();
      for (const key of cacheKeys) {
        if (key.includes('transformers') || key.includes('onnx') || key.includes('huggingface')) {
          await caches.delete(key);
          console.log(`Deleted cache: ${key}`);
        }
      }
      
      // Reset initialization state
      this.isInitialized = false;
      this.generator = null;
      
      console.log('Model cache cleared successfully');
    } catch (error) {
      console.error('Error clearing model cache:', error);
      throw error;
    }
  }

  async getModelCacheSize(): Promise<number> {
    try {
      let totalSize = 0;
      const cacheKeys = await caches.keys();
      
      for (const key of cacheKeys) {
        if (key.includes('transformers') || key.includes('onnx') || key.includes('huggingface')) {
          const cache = await caches.open(key);
          const requests = await cache.keys();
          
          for (const request of requests) {
            const response = await cache.match(request);
            if (response) {
              const blob = await response.blob();
              totalSize += blob.size;
            }
          }
        }
      }
      
      return totalSize;
    } catch (error) {
      console.error('Error getting cache size:', error);
      return 0;
    }
  }
}
