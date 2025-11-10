// src/api/collections.js
import apiClient from './apiClient';

export const getCollections = (page = 0, size = 10) =>
  apiClient.get(`/collections`, { params: { page, size } });

export const getCollectionById = (id) =>
  apiClient.get(`/collections/${id}`);

export const createCollection = (data) =>
  apiClient.post(`/collections`, data);

export const updateCollection = (id, data) =>
  apiClient.put(`/collections/${id}`, data);

export const deleteCollection = (id) =>
  apiClient.delete(`/collections/${id}`);
