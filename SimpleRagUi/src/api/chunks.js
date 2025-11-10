import axios from 'axios';

const BASE_URL = '/api/chunks';

// List chunks with optional filters
export function getChunks({ page = 0, size = 20, collectionId, documentId } = {}) {
  const params = { page, size };
  if (collectionId) params.collectionId = collectionId;
  if (documentId) params.documentId = documentId;
  return axios.get(BASE_URL, { params });
}

// Create a new chunk
export function createChunk(chunk, collectionId) {
  const params = {};
  if (collectionId) params.collectionId = collectionId;
  return axios.post(BASE_URL, chunk, { params });
}

// Update a chunk by collectionId and chunk id
export function updateChunk(collectionId, id, chunk) {
  return axios.put(`${BASE_URL}/${collectionId}/${id}`, chunk);
}

// Delete a chunk by collectionId and chunk id
export function deleteChunk(collectionId, id) {
  return axios.delete(`${BASE_URL}/${collectionId}/${id}`);
}
