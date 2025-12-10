// src/api/hfModels.js
import apiClient from './apiClient';

export const searchHfModels = (name = '', sizeMb = null, limit = 25) => {
  const params = { limit };
  if (name) params.name = name;
  if (sizeMb !== null && sizeMb !== '') params.sizeMb = sizeMb;
  
  return apiClient.get('/internal/hf/models/search', { params });
};
