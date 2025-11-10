// src/api/chats.js
import apiClient from './apiClient';

export const getChats = (page = 0, size = 10) =>
  apiClient.get('/chats', { params: { page, size } });

export const getChatById = (id) =>
  apiClient.get(`/chats/${id}`);

export const createChat = (data) =>
  apiClient.post('/chats', data);

export const updateChat = (id, data) =>
  apiClient.put(`/chats/${id}`, data);

export const deleteChat = (id) =>
  apiClient.delete(`/chats/${id}`);
