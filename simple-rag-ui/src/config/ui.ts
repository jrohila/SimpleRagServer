// Central UI configuration and feature flags for the frontend
import api from './api';

const env = (import.meta as any)?.env ?? {};

export const ENABLE_LOCAL_LLM = (env.VITE_ENABLE_LOCAL_LLM === 'true') || true;
export const LOCAL_LLM_MODEL_NAME = env.VITE_LOCAL_LLM_MODEL_NAME || 'onnx-community/granite-4.0-micro-ONNX-web';
export const LOCAL_LLM_DEVICE = env.VITE_LOCAL_LLM_DEVICE || 'webgpu';
export const LOCAL_LLM_USE_BROWSER_CACHE = env.VITE_LOCAL_LLM_USE_BROWSER_CACHE !== 'false';

export const DEFAULT_GENERATION = {
  maxNewTokens: Number(env.VITE_DEFAULT_MAX_NEW_TOKENS || 8192), // High limit for WebGPU - no API costs
  temperature: Number(env.VITE_DEFAULT_TEMPERATURE || 0.7),
  topK: Number(env.VITE_DEFAULT_TOP_K || 50),
  topP: Number(env.VITE_DEFAULT_TOP_P || 0.95),
  repetitionPenalty: Number(env.VITE_DEFAULT_REPETITION_PENALTY || 1.1),
};

export const WEBGPU = {
  apiBase: api.API_BASE_URL.replace(/\/$/, '') + '/api/webgpu',
  maxContextLength: Number(env.VITE_WEBGPU_MAX_CONTEXT_LENGTH || 4096),
  completionLength: Number(env.VITE_WEBGPU_COMPLETION_LENGTH || 1024),
  headroomLength: Number(env.VITE_WEBGPU_HEADROOM_LENGTH || 1024),
};

export const DOWNLOAD_PROGRESS_THRESHOLD_PERCENT = Number(env.VITE_DOWNLOAD_PROGRESS_THRESHOLD || 5);

export default {
  ENABLE_LOCAL_LLM,
  LOCAL_LLM_MODEL_NAME,
  LOCAL_LLM_DEVICE,
  LOCAL_LLM_USE_BROWSER_CACHE,
  DEFAULT_GENERATION,
  WEBGPU,
  DOWNLOAD_PROGRESS_THRESHOLD_PERCENT,
};
