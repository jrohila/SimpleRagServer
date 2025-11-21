import { LLMMessage, LLMServiceConfig, StreamCallback } from './RemoteLLMService';
import { WebGpuMessageService } from './WebGpuMessageService';
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
  private isGenerating = false;
  private modelName = 'onnx-community/granite-4.0-micro-ONNX-web';
  private downloadProgressCallback?: (progress: { loaded: number; total: number; percentage: number }) => void;
  private webGpuMessageService = new WebGpuMessageService();

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

    // Check if already generating
    if (this.isGenerating) {
      callbacks.onError(new Error('A generation is already in progress. Please wait...'));
      return;
    }

    try {
      this.isGenerating = true;
      
      if (!this.isInitialized) {
        await this.initialize();
      }

      // Process messages through backend if publicName is provided and RAG is enabled
      let processedMessages = messages;
      if (config.useRag && config.publicName) {
        try {
          console.log('Fetching RAG context from backend...', { 
            publicName: config.publicName, 
            messageCount: messages.length 
          });
          
          const response = await this.webGpuMessageService.processMessagesForWebGPU(
            config.publicName,
            messages
          );

          console.log('Backend response:', { 
            messageCount: response.length,
            messages: response 
          });

          // Validate and convert messages
          processedMessages = response
            .filter(m => m && m.role && m.content) // Filter out invalid messages
            .map(m => {
              // Normalize role to lowercase
              const role = m.role.toLowerCase();
              if (!['system', 'user', 'assistant'].includes(role)) {
                console.warn(`Invalid role "${m.role}", defaulting to user`);
                return { role: 'user' as const, content: m.content };
              }
              return {
                role: role as 'system' | 'user' | 'assistant',
                content: m.content
              };
            });

          console.log('Messages processed with RAG context from backend', {
            processedCount: processedMessages.length
          });
        } catch (error) {
          console.warn('Failed to get RAG context from backend, proceeding without it:', error);
          // Continue with original messages if backend fails
        }
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

      // Validate messages before passing to generator
      if (!processedMessages || processedMessages.length === 0) {
        throw new Error('No messages to process');
      }

      // Truncate context if too large - reduced to 2048 tokens for WebGPU stability
      const MAX_INPUT_TOKENS = 2048; // Reduced from 4096 to avoid WebGPU memory errors
      let truncatedMessages = processedMessages;
      
      // Estimate token count (rough: ~4 chars per token)
      const estimateTokens = (msgs: typeof processedMessages) => {
        return msgs.reduce((sum, msg) => sum + Math.ceil(msg.content.length / 4), 0);
      };
      
      const totalTokens = estimateTokens(processedMessages);
      
      if (totalTokens > MAX_INPUT_TOKENS) {
        console.log(`Context too large: ${totalTokens} tokens, truncating to ${MAX_INPUT_TOKENS} tokens`);
        
        // Keep system message (if any) and most recent messages
        const systemMessages = processedMessages.filter(m => m.role === 'system');
        const conversationMessages = processedMessages.filter(m => m.role !== 'system');
        
        // Start with system messages
        truncatedMessages = [...systemMessages];
        let currentTokens = estimateTokens(truncatedMessages);
        const includedMessages: typeof conversationMessages = [];
        
        // Add messages from the end (most recent) until we hit the limit
        for (let i = conversationMessages.length - 1; i >= 0; i--) {
          const msgTokens = Math.ceil(conversationMessages[i].content.length / 4);
          if (currentTokens + msgTokens > MAX_INPUT_TOKENS) {
            console.log(`Truncated ${i + 1} older messages`);
            break;
          }
          includedMessages.unshift(conversationMessages[i]);
          currentTokens += msgTokens;
        }
        
        // Combine system messages with included conversation messages
        truncatedMessages = [...systemMessages, ...includedMessages];
        
        const finalTokens = estimateTokens(truncatedMessages);
        console.log(`Final context size: ${finalTokens} tokens with ${truncatedMessages.length} messages`);
      } else {
        console.log(`Context size: ${totalTokens} tokens (within ${MAX_INPUT_TOKENS} limit)`);
      }

      console.log('\n=== Sending to LLM generator ===');
      console.log(`Message count: ${truncatedMessages.length}`);
      truncatedMessages.forEach((msg, idx) => {
        console.log(`[${idx + 1}] ${msg.role.toUpperCase()}:`);
        console.log(msg.content);
        console.log('---');
      });
      console.log('================================\n');

      // Generate response with configurable parameters
      await this.generator(truncatedMessages, {
        max_new_tokens: 2048, // Increased from 512 to 2048 tokens (~1600 words)
        temperature: config.temperature || 0.7,
        do_sample: config.temperature ? config.temperature > 0 : false,
        top_k: 50,
        top_p: 0.95,
        repetition_penalty: 1.1,
        min_new_tokens: 1,
        streamer,
      });

      callbacks.onComplete();
    } catch (error) {
      console.error('Error in local LLM generation:', error);
      callbacks.onError(error as Error);
    } finally {
      this.isGenerating = false;
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
