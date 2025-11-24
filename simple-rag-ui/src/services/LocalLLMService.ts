import { LLMMessage, LLMServiceConfig, StreamCallback } from './RemoteLLMService';
import { WebGpuMessageService } from './WebGpuMessageService';
import { Platform } from 'react-native';

// LLMConfig interface for local WebGPU generation
// (Note: RemoteLLMService doesn't need this since backend overrides with server-side ChatEntity.llmConfig)
export interface LLMConfig {
  useCase?: string;
  maxNewTokens?: number;
  temperature?: number;
  doSample?: boolean;
  topK?: number;
  topP?: number;
  repetitionPenalty?: number;
  minNewTokens?: number;
}

// Extended config for LocalLLMService that includes llmConfig
export interface LocalLLMServiceConfig extends LLMServiceConfig {
  llmConfig?: LLMConfig;
}

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
  private initializationPromise: Promise<void> | null = null;
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

    // If already initialized, resolve immediately
    if (this.isInitialized) {
      return;
    }

    // If initialization is already running, return the promise so callers can await it
    if (this.isInitializing && this.initializationPromise) {
      return this.initializationPromise;
    }

    this.isInitializing = true;

    this.initializationPromise = (async () => {
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
        // Rethrow so callers can observe initialization failures
        throw error;
      } finally {
        this.isInitializing = false;
        this.initializationPromise = null;
      }
    })();

    return this.initializationPromise;
  }

  /**
   * Start initialization in background (non-blocking).
   * Call this from UI when user enables Local mode.
   */
  startInitialization(): void {
    if (this.isInitialized || this.isInitializing) {
      return;
    }
    // Kick off initialization but don't await here
    this.initialize().catch(err => {
      console.warn('Local model preload failed:', err);
    });
  }

  /**
   * Rewrite user prompt using the LLM if enabled in chat configuration
   */
  private async rewriteUserPrompt(
    userPrompt: string,
    lastAssistantMessage: string | null,
    systemPrompt: string,
    llmConfig?: LLMConfig
  ): Promise<string> {
    try {
      console.log('\n=== Rewriting User Prompt (Granite Tokens) ===');
      console.log('Original prompt:', userPrompt);
      console.log('Last assistant message:', lastAssistantMessage || 'None');

      // Build role-typed messages: system, assistant (if present), user
      const roleMessages: LLMMessage[] = [];
      if (systemPrompt) {
        roleMessages.push({ role: 'system', content: systemPrompt });
      }
      if (lastAssistantMessage) {
        roleMessages.push({ role: 'assistant', content: lastAssistantMessage });
      }
      roleMessages.push({ role: 'user', content: userPrompt });

      // Format with Granite special tokens
      const graniteFormatted = this.formatMessagesWithGraniteTokens(roleMessages);

      // Wrap as a single user message so the generator receives the structured prompt
      const rewriteMessages: LLMMessage[] = [
        { role: 'user', content: graniteFormatted }
      ];

      // Dynamic import
      const { TextStreamer } = await import('@huggingface/transformers');

      let rewrittenPrompt = '';
      const streamer = new TextStreamer(this.generator.tokenizer, {
        skip_prompt: true,
        skip_special_tokens: true,
        callback_function: (text: string) => {
          rewrittenPrompt += text;
        }
      });

      await this.generator(rewriteMessages, {
        max_new_tokens: llmConfig?.maxNewTokens || 256,  // Shorter for prompt rewriting
        temperature: llmConfig?.temperature !== undefined ? llmConfig.temperature : 0.3,
        do_sample: llmConfig?.doSample !== undefined ? llmConfig.doSample : false,
        top_k: llmConfig?.topK || 50,
        top_p: llmConfig?.topP || 0.95,
        repetition_penalty: llmConfig?.repetitionPenalty || 1.1,
        min_new_tokens: llmConfig?.minNewTokens || 1,
        streamer,
      });

      const trimmedPrompt = rewrittenPrompt.trim();
      console.log('Rewritten prompt (trimmed):', trimmedPrompt);
      console.log('=================================\n');

      return trimmedPrompt || userPrompt; // Fallback to original if rewriting fails
    } catch (error) {
      console.error('Error rewriting prompt (Granite flow), using original:', error);
      return userPrompt;
    }
  }

  /**
   * Format messages with Granite special tokens
   * Converts message array to single string with role markers:
   * <|start_of_role|>role_name<|end_of_role|>content<|end_of_text|>
   */
  private formatMessagesWithGraniteTokens(messages: LLMMessage[]): string {
    const formatted = messages.map(msg => {
      return `<|start_of_role|>${msg.role}<|end_of_role|>${msg.content}<|end_of_text|>`;
    }).join('\n');
    
    console.log('\n=== Formatted with Granite Tokens ===');
    console.log(formatted);
    console.log('======================================\n');
    
    return formatted;
  }

  async sendMessage(
    config: LocalLLMServiceConfig,
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
        console.log('Local model not ready - waiting for initialization...');
        try {
          await this.initialize();
        } catch (initError) {
          console.error('Initialization failed while waiting in sendMessage():', initError);
          callbacks.onError(initError as Error);
          return;
        }
      }

      // Limit to 10 most recent messages to avoid context overflow
      // Backend adds system messages (RAG context, memory, etc.), so we only send conversation history
      const MAX_MESSAGES = 10;
      const conversationMessages = messages.filter(m => m.role !== 'system');
      
      // Check if we need to rewrite the user's prompt
      let processedConversationMessages = conversationMessages;
      if (config.useUserPromptRewriting && config.userPromptRewritingPrompt) {
        // Get the last user message and last assistant message
        const lastUserMessage = [...conversationMessages].reverse().find(m => m.role === 'user');
        const lastAssistantMessage = [...conversationMessages].reverse().find(m => m.role === 'assistant');
        
        // Count user messages to determine if this is the first one
        const userMessageCount = conversationMessages.filter(m => m.role === 'user').length;
        
        console.log('\n=== Prompt Rewriting Check ===');
        console.log('Total conversation messages:', conversationMessages.length);
        console.log('User message count:', userMessageCount);
        console.log('Last user message found:', !!lastUserMessage);
        console.log('Last assistant message found:', !!lastAssistantMessage);
        console.log('Messages:', conversationMessages.map(m => ({ role: m.role, contentLength: m.content.length })));
        
        // Only rewrite if this is NOT the first user message (userMessageCount > 1 means we have previous user messages)
        if (lastUserMessage && lastAssistantMessage && userMessageCount > 1) {
          try {
            console.log('\n========================================');
            console.log('USER PROMPT REWRITING ENABLED');
            console.log('========================================');
            console.log('Original user prompt:');
            console.log(lastUserMessage.content);
            console.log('----------------------------------------');
            
            const rewrittenContent = await this.rewriteUserPrompt(
              lastUserMessage.content,
              lastAssistantMessage.content,
              config.userPromptRewritingPrompt,
              config.llmConfig
            );
            
            console.log('----------------------------------------');
            console.log('Rewritten user prompt:');
            console.log(rewrittenContent);
            console.log('========================================\n');
            
            // Replace the last user message with the rewritten one
            processedConversationMessages = conversationMessages.map(m => 
              m === lastUserMessage ? { ...m, content: rewrittenContent } : m
            );
          } catch (error) {
            console.warn('Failed to rewrite prompt, proceeding with original:', error);
          }
        } else {
          console.log('Skipping prompt rewriting - first user message (userMessageCount:', userMessageCount, ')');
        }
      }
      
      let limitedMessages = processedConversationMessages;
      if (processedConversationMessages.length > MAX_MESSAGES) {
        // Keep only the most recent messages
        limitedMessages = processedConversationMessages.slice(-MAX_MESSAGES);
        console.log(`Limited conversation history from ${processedConversationMessages.length} to ${MAX_MESSAGES} messages (system messages will be added by backend)`);
      } else {
        console.log(`Sending ${processedConversationMessages.length} conversation messages (system messages will be added by backend)`);
      }

      // Process messages through backend if publicName is provided and RAG is enabled
      let processedMessages = limitedMessages;
      if (config.useRag && config.publicName) {
        try {
          console.log('Fetching RAG context from backend...', { 
            publicName: config.publicName, 
            messageCount: limitedMessages.length 
          });
          
          // WebGPU client capabilities for context sizing
          const maxContextLength = 4096;  // Total context window
          const completionLength = 1024;  // Reserved for model output
          const headroomLength = 1024;    // Safety buffer
          
          const response = await this.webGpuMessageService.processMessagesForWebGPU(
            config.publicName,
            limitedMessages,
            maxContextLength,
            completionLength,
            headroomLength
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

      // Calculate token count before sending to LLM
      const calculateTokens = (messages: typeof processedMessages): number => {
        let totalTokens = 0;
        for (const msg of messages) {
          // Rough estimation: ~4 characters per token (OpenAI-style)
          // This is a simple heuristic; actual tokenization may vary
          const contentTokens = Math.ceil(msg.content.length / 4);
          // Add overhead for role and message structure (~4 tokens per message)
          totalTokens += contentTokens + 4;
        }
        return totalTokens;
      };

      const inputTokens = calculateTokens(processedMessages);
      console.log(`\n=== Token Calculation ===`);
      console.log(`Total input tokens (estimated): ${inputTokens}`);
      console.log(`Messages being sent: ${processedMessages.length}`);
      
      // Calculate per-message breakdown
      const tokenBreakdown = processedMessages.map((msg, idx) => {
        const msgTokens = Math.ceil(msg.content.length / 4) + 4;
        return {
          index: idx + 1,
          role: msg.role,
          tokens: msgTokens,
          contentLength: msg.content.length
        };
      });
      console.table(tokenBreakdown);

      console.log('\n=== Sending to LLM generator ===');
      console.log(`Message count: ${processedMessages.length}`);
      processedMessages.forEach((msg, idx) => {
        console.log(`[${idx + 1}] ${msg.role.toUpperCase()}:`);
        console.log(msg.content);
        console.log('---');
      });
      console.log('================================\n');

      // Format messages with Granite special tokens
      const graniteFormattedPrompt = this.formatMessagesWithGraniteTokens(processedMessages);
      
      // Create a single user message with the formatted prompt
      const graniteMessages: LLMMessage[] = [
        {
          role: 'user',
          content: graniteFormattedPrompt
        }
      ];

      // Generate response with configurable parameters from llmConfig
      const llmConfig = config.llmConfig;
      await this.generator(graniteMessages, {
        max_new_tokens: llmConfig?.maxNewTokens || 2048,
        temperature: llmConfig?.temperature !== undefined ? llmConfig.temperature : (config.temperature || 0.7),
        do_sample: llmConfig?.doSample !== undefined ? llmConfig.doSample : (config.temperature ? config.temperature > 0 : false),
        top_k: llmConfig?.topK || 50,
        top_p: llmConfig?.topP || 0.95,
        repetition_penalty: llmConfig?.repetitionPenalty || 1.1,
        min_new_tokens: llmConfig?.minNewTokens || 1,
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
