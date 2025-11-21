/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.pipeline;

import io.github.jrohila.simpleragserver.chat.ChatService;
import io.github.jrohila.simpleragserver.chat.util.BoostTermDetector;
import io.github.jrohila.simpleragserver.chat.util.ChatHelper;
import io.github.jrohila.simpleragserver.domain.ChatEntity;
import io.github.jrohila.simpleragserver.domain.ExtractedFactDTO;
import io.github.jrohila.simpleragserver.domain.ChunkEntity;
import io.github.jrohila.simpleragserver.repository.ChunkSearchService;
import io.github.jrohila.simpleragserver.service.UserFactsService;
import io.github.jrohila.simpleragserver.service.util.SearchResult;
import io.github.jrohila.simpleragserver.service.util.SearchTerm;
import io.github.jrohila.simpleragserver.util.CosineSimilarityCalculator;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jukka
 */
@Component
public class ContextAdditionPipe {

    public static enum OperationResult {
        CONTEXT_ADDED, PROMPT_OUT_OF_CONTEXT
    }

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${processing.chat.rag.max-results:200}")
    private int ragMaxResults;
    @Value("${opensearch.rrf.window-size:50}")
    private int rrfWindowSize;
    @Value("${processing.chat.token.max-context-tokens:32000}")
    private int maxContextTokens;
    @Value("${processing.chat.token.reserve-completion:4000}")
    private int reserveCompletionTokens;
    @Value("${processing.chat.token.reserve-headroom:4000}")
    private int reserveHeadroomTokens;

    @Autowired
    private BoostTermDetector boostTermDetector;

    @Autowired
    private ChatHelper chatHelper;

    @Autowired
    private UserFactsService userFactsService;

    @Autowired
    private ChunkSearchService chunkSearchService;

    public List<Message> appendMemory(List<Message> springMessages, List<Integer> fingerprints, ChatEntity chatEntity) {
        List<ExtractedFactDTO> facts = userFactsService.getFacts(fingerprints);
        if ((facts != null) && (!facts.isEmpty())) {
            try {
                String memory = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(facts);
                if (!memory.isBlank()) {
                    String prefix = (chatEntity.getDefaultMemoryPrompt() != null ? chatEntity.getDefaultMemoryPrompt().trim() : "");
                    if (!prefix.isEmpty()) {
                        springMessages.add(0, new SystemMessage(prefix + "\n" + memory));
                    } else {
                        springMessages.add(0, new SystemMessage(memory));
                    }
                }
            } catch (Exception e) {
                // fallback: do not add memory if serialization fails
            }
        }
        return springMessages;
    }

    public Pair<OperationResult, List<Message>> process(List<Message> springMessages, ChatEntity chatEntity) {
        return this.process(springMessages, chatEntity, maxContextTokens, reserveCompletionTokens, reserveHeadroomTokens);
    }
    
    public Pair<OperationResult, List<Message>> process(List<Message> springMessages, ChatEntity chatEntity, int maxContextLenght, int completionLength, int headroomLength) {
        String userPrompt = null;
        if (springMessages != null) {
            for (Message m : springMessages) {
                if (MessageType.USER.equals(m.getMessageType())) {
                    userPrompt = m.getText();
                }
            }
        }

        String context = "";
        String prefix = (chatEntity.getDefaultContextPrompt() != null ? chatEntity.getDefaultContextPrompt().trim() : "");
        boolean promptOutOfScope = false;

        if (userPrompt != null && !userPrompt.isBlank()) {
            try {
                // Use boosted hybrid search with knn; dynamically sized
                // Choose a coherent candidate set size: ensure at least RRF window size, and use ragMaxResults to cap
                int minNeeded = Math.max(25, rrfWindowSize);
                int size = Math.max(minNeeded, ragMaxResults);
                log.info("[ChatService] RAG search size={} (minNeeded={}, rrfWindowSize={}, ragMaxResults={})", size, minNeeded, rrfWindowSize, ragMaxResults);

                List<SearchTerm> terms = boostTermDetector.buildSearchTerms(userPrompt, springMessages, 5d, 2d, 1d);

                log.info(terms.toString());

                Pair<List<SearchResult<ChunkEntity>>, List<Float>> resultsWithEmbedding = chunkSearchService.hybridSearchWithEmbedding(chatEntity.getDefaultCollectionId(), userPrompt, ChunkSearchService.MatchType.MATCH, terms, size, true, null);
                List<SearchResult<ChunkEntity>> results = resultsWithEmbedding.getKey();

                List<List<Float>> searchResults = new ArrayList<>();
                for (SearchResult<ChunkEntity> r : results) {
                    searchResults.add(r.getContent().getEmbedding());
                    if (searchResults.size() > 25) {
                        break;
                    }
                }

                if (springMessages.size() > 4) {
                    promptOutOfScope = !CosineSimilarityCalculator.isSimilar(resultsWithEmbedding.getValue(), searchResults, 0.5);
                }

                if (!promptOutOfScope) {
                    if (results != null && !results.isEmpty()) {
                        // Compute token budget based on current messages (without context yet)
                        int currentTokens = 0;
                        try {
                            currentTokens = this.chatHelper.countTokensForMessages(springMessages);
                        } catch (Exception ignore) {
                        }
                        int prefixTokens = this.chatHelper.countTokens(prefix);
                        int budget = Math.max(0, maxContextLenght - currentTokens - prefixTokens - completionLength - headroomLength);
                        if (budget <= 0) {
                            log.info("[ChatService] Context budget is 0 or negative (currentTokens={}, prefixTokens={}). Skipping RAG context.", currentTokens, prefixTokens);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            int used = 0;
                            int added = 0;

                            // Group chunks by document
                            java.util.Map<String, List<SearchResult<ChunkEntity>>> groupedByDocument = new java.util.LinkedHashMap<>();
                            for (SearchResult<ChunkEntity> r : results) {
                                String docName = r.getContent().getDocumentName();
                                if (docName == null || docName.isBlank()) {
                                    docName = "Unknown";
                                }
                                groupedByDocument.computeIfAbsent(docName, k -> new ArrayList<>()).add(r);
                            }

                            sb.append("<documents>\n");
                            int documentsTagTokens = this.chatHelper.countTokens("<documents>\n</documents>\n");
                            used += documentsTagTokens;

                            for (java.util.Map.Entry<String, List<SearchResult<ChunkEntity>>> entry : groupedByDocument.entrySet()) {
                                String docName = entry.getKey();
                                List<SearchResult<ChunkEntity>> chunks = entry.getValue();

                                String docOpenTag = "  <document name=\"" + docName + "\">\n";
                                String docCloseTag = "  </document>\n";
                                int docTagsTokens = this.chatHelper.countTokens(docOpenTag + docCloseTag);

                                if (used + docTagsTokens > budget) {
                                    break;
                                }

                                sb.append(docOpenTag);
                                used += docTagsTokens;

                                for (SearchResult<ChunkEntity> r : chunks) {
                                    String t = r.getContent().getText();
                                    if (t == null || t.isBlank()) {
                                        continue;
                                    }
                                    String normalized = t.trim();
                                    int pageNum = r.getContent().getPageNumber();
                                    String chunkOpenTag = "    <chunk page=\"" + pageNum + "\">";
                                    String chunkCloseTag = "</chunk>\n";
                                    int chunkTokens = this.chatHelper.countTokens(chunkOpenTag + normalized + chunkCloseTag);

                                    if (used + chunkTokens > budget) {
                                        break;
                                    }

                                    sb.append(chunkOpenTag).append(normalized).append(chunkCloseTag);
                                    used += chunkTokens;
                                    added++;
                                }

                                sb.append(docCloseTag);
                            }

                            sb.append("</documents>\n");
                            context = sb.toString();
                            log.info("[ChatService] RAG packing: results={} addedChunks={} contextTokensUsed={} budget={} currentTokens={} prefixTokens={} reserve={} headroom={} ", (results == null ? 0 : results.size()), added, used, budget, currentTokens, prefixTokens, reserveCompletionTokens, reserveHeadroomTokens);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("[ChatService] RAG search failed: {}", e.getMessage());
            }
        }

        // Add context as a system message if found
        if (!context.isBlank()) {
            if (!prefix.isEmpty()) {
                springMessages.add(0, new SystemMessage(prefix + "\n" + context));
            } else {
                springMessages.add(0, new SystemMessage(context));
            }
        } else if (context.isBlank()) {
            log.info("[ChatService] No context found for prompt: '{}'. RAG enabled but no relevant chunks found.", userPrompt);
        }

        if (promptOutOfScope) {
            return Pair.of(OperationResult.PROMPT_OUT_OF_CONTEXT, springMessages);
        } else {
            return Pair.of(OperationResult.CONTEXT_ADDED, springMessages);
        }
    }

}
