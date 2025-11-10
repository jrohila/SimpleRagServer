// src/api/documents.js
import apiClient from './apiClient';

// List documents (optionally by collectionId, paginated)
export const getDocuments = (collectionId, page = 0, size = 20) =>
  apiClient.get('/documents', {
    params: { collectionId, page, size },
  });

// Upload a document (multipart/form-data)
export const uploadDocument = (collectionId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.post(`/documents?collectionId=${encodeURIComponent(collectionId)}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// Get a single document by collectionId and document id
export const getDocument = (collectionId, id) =>
  apiClient.get(`/documents/${encodeURIComponent(collectionId)}/${encodeURIComponent(id)}`);

// Update a document (PUT, multipart/form-data, can update file)
export const updateDocument = (collectionId, id, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.put(`/documents/${encodeURIComponent(collectionId)}/${encodeURIComponent(id)}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// Delete a single document
export const deleteDocument = (collectionId, id) =>
  apiClient.delete(`/documents/${encodeURIComponent(collectionId)}/${encodeURIComponent(id)}`);

// Delete all documents in a collection
export const deleteAllDocuments = (collectionId) =>
  apiClient.delete(`/documents/${encodeURIComponent(collectionId)}`);
