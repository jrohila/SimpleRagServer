import apiClient from './apiClient';

const BASE_URL = '/chunks';

// List chunks with optional filters
/**
 * @param {Object} opts
 * @param {number} [opts.page]
 * @param {number} [opts.size]
 * @param {string} [opts.collectionId]
 * @param {string} [opts.documentId]
 */
export function getChunks({ page = 0, size = 20, collectionId, documentId } = {}) {
  const params = { page, size };
  if (collectionId) params.collectionId = collectionId;
  if (documentId) params.documentId = documentId;
  return apiClient.get(BASE_URL, { params });
}

// Create a new chunk
export function createChunk(chunk, collectionId) {
  const params = {};
  if (collectionId) params.collectionId = collectionId;
  return apiClient.post(BASE_URL, chunk, { params });
}

// Update a chunk by collectionId and chunk id
export function updateChunk(collectionId, id, chunk) {
  return apiClient.put(`${BASE_URL}/${collectionId}/${id}`, chunk);
}

// Delete a chunk by collectionId and chunk id
export function deleteChunk(collectionId, id) {
  return apiClient.delete(`${BASE_URL}/${collectionId}/${id}`);
}
