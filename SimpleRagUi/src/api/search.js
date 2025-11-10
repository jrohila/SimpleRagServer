import apiClient from './apiClient';

const BASE_URL = '/search';

/**
 * Search for documents/chunks based on query
 * @param {Object} params
 * @param {string} params.collectionId - Collection ID to search in
 * @param {string} params.query - Search query text
 * @param {number} [params.size=10] - Number of results to return
 * @param {string} [params.language] - Optional language filter
 * @param {boolean} [params.enableFuzziness=false] - Enable fuzzy matching
 * @param {Array<{term: string, boostWeight: number, mandatory: boolean}>} [params.terms] - Boost terms
 */
export function search({ collectionId, query, size = 10, language, enableFuzziness = false, terms } = {}) {
  const body = { query, size };
  if (language) body.language = language;
  if (enableFuzziness) body.enableFuzziness = enableFuzziness;
  if (terms && terms.length > 0) body.terms = terms;
  
  return apiClient.post(BASE_URL, body, { params: { collectionId } });
}

/**
 * Perform hybrid search (combines vector and keyword search)
 * @param {Object} params
 * @param {string} params.collectionId - Collection ID to search in
 * @param {string} params.query - Search query text
 * @param {number} [params.size=10] - Number of results to return
 * @param {string} [params.matchType] - Match type (MATCH, MATCH_PHRASE, etc.)
 * @param {string} [params.language] - Optional language filter
 * @param {boolean} [params.enableFuzziness=false] - Enable fuzzy matching
 * @param {Array<{term: string, boostWeight: number, mandatory: boolean}>} [params.terms] - Boost terms
 */
export function hybridSearch({ collectionId, query, size = 10, matchType, language, enableFuzziness = false, terms } = {}) {
  const body = { query, size };
  if (matchType) body.matchType = matchType;
  if (language) body.language = language;
  if (enableFuzziness) body.enableFuzziness = enableFuzziness;
  if (terms && terms.length > 0) body.terms = terms;
  
  return apiClient.post(`${BASE_URL}/hybrid`, body, { params: { collectionId } });
}

/**
 * Perform semantic (vector) search
 * @param {Object} params
 * @param {string} params.collectionId - Collection ID to search in
 * @param {string} params.query - Search query text
 * @param {number} [params.size=10] - Number of results to return
 * @param {string} [params.language] - Optional language filter
 * @param {Array<{term: string, boostWeight: number, mandatory: boolean}>} [params.terms] - Boost terms
 */
export function semanticSearch({ collectionId, query, size = 10, language, terms } = {}) {
  const body = { query, size };
  if (language) body.language = language;
  if (terms && terms.length > 0) body.terms = terms;
  
  return apiClient.post(`${BASE_URL}/vector`, body, { params: { collectionId } });
}

/**
 * Perform keyword (lexical/BM25) search
 * @param {Object} params
 * @param {string} params.collectionId - Collection ID to search in
 * @param {string} params.query - Search query text
 * @param {number} [params.size=10] - Number of results to return
 * @param {string} [params.matchType] - Match type (MATCH, MATCH_PHRASE, etc.)
 * @param {string} [params.language] - Optional language filter
 * @param {boolean} [params.enableFuzziness=false] - Enable fuzzy matching
 * @param {Array<{term: string, boostWeight: number, mandatory: boolean}>} [params.terms] - Boost terms
 */
export function keywordSearch({ collectionId, query, size = 10, matchType, language, enableFuzziness = false, terms } = {}) {
  const body = { query, size };
  if (matchType) body.matchType = matchType;
  if (language) body.language = language;
  if (enableFuzziness) body.enableFuzziness = enableFuzziness;
  if (terms && terms.length > 0) body.terms = terms;
  
  return apiClient.post(`${BASE_URL}/lexical`, body, { params: { collectionId } });
}
