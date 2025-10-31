/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.pipeline;

import io.github.jrohila.simpleragserver.chat.ChatService;
import io.github.jrohila.simpleragserver.chat.OpenAiChatRequest;
import io.github.jrohila.simpleragserver.chat.util.BoostTermDetector;
import io.github.jrohila.simpleragserver.chat.util.ChatHelper;
import io.github.jrohila.simpleragserver.dto.ExtractedFactDTO;
import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.service.ChunkSearchService;
import io.github.jrohila.simpleragserver.service.UserFactsService;
import io.github.jrohila.simpleragserver.service.util.SearchResult;
import io.github.jrohila.simpleragserver.service.util.SearchTerm;
import java.util.List;
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

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${processing.chat.rag.memory-prompt}")
    private String memoryContextPrompt;

    @Value("${processing.chat.rag.context-prompt}")
    private String ragContextPrompt;

    @Value("${processing.chat.rag.max-results:200}")
    private int ragMaxResults;
    @Value("${opensearch.rrf.window-size:50}")
    private int rrfWindowSize;
    @Value("${processing.chat.token.max-context-tokens:26000}")
    private int maxContextTokens;
    @Value("${processing.chat.token.reserve-completion:4000}")
    private int reserveCompletionTokens;
    @Value("${processing.chat.token.reserve-headroom:2000}")
    private int reserveHeadroomTokens;

    @Autowired
    private BoostTermDetector boostTermDetector;

    @Autowired
    private ChatHelper chatHelper;

    @Autowired
    private UserFactsService userFactsService;

    @Autowired
    private ChunkSearchService chunkSearchService;

    public List<Message> appendMemory(List<Message> springMessages, List<Integer> fingerprints) {
        List<ExtractedFactDTO> facts = userFactsService.getFacts(fingerprints);
        if ((facts != null) && (!facts.isEmpty())) {
            try {
                String memory = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(facts);
                if (!memory.isBlank()) {
                    String prefix = (memoryContextPrompt != null ? memoryContextPrompt.trim() : "");
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

    public List<Message> process(List<Message> springMessages) {
        String userPrompt = null;
        if (springMessages != null) {
            for (Message m : springMessages) {
                if (MessageType.USER.equals(m.getMessageType())) {
                    userPrompt = m.getText();
                }
            }
        }

        String context = "";
        if (userPrompt != null && !userPrompt.isBlank()) {
            try {
                // Use boosted hybrid search with knn; dynamically sized
                // Choose a coherent candidate set size: ensure at least RRF window size, and use ragMaxResults to cap
                int minNeeded = Math.max(25, rrfWindowSize);
                int size = Math.max(minNeeded, ragMaxResults);
                log.info("[ChatService] RAG search size={} (minNeeded={}, rrfWindowSize={}, ragMaxResults={})", size, minNeeded, rrfWindowSize, ragMaxResults);

                List<SearchTerm> terms = boostTermDetector.buildSearchTerms(userPrompt, springMessages, 5d, 2d, 1d);

                log.info(terms.toString());

                List<SearchResult<ChunkEntity>> results = chunkSearchService.hybridSearch(userPrompt, ChunkSearchService.MatchType.MATCH, terms, size, true, null);

                if (results != null && !results.isEmpty()) {
                    // Compute token budget based on current messages (without context yet)
                    int currentTokens = 0;
                    try {
                        currentTokens = this.chatHelper.countTokensForMessages(springMessages);
                    } catch (Exception ignore) {
                    }
                    String prefix = (ragContextPrompt != null ? ragContextPrompt.trim() : "");
                    int prefixTokens = this.chatHelper.countTokens(prefix);
                    int budget = Math.max(0, maxContextTokens - currentTokens - prefixTokens - reserveCompletionTokens - reserveHeadroomTokens);
                    if (budget <= 0) {
                        log.info("[ChatService] Context budget is 0 or negative (currentTokens={}, prefixTokens={}). Skipping RAG context.", currentTokens, prefixTokens);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        int used = 0;
                        int added = 0;

                        for (SearchResult<ChunkEntity> r : results) {
                            String t = r.getContent().getText();
                            if (t == null || t.isBlank()) {
                                continue;
                            }
                            String normalized = t.trim();
                            int tokens = this.chatHelper.countTokens(normalized) + 1; // +1 for newline separator
                            if (used + tokens > budget) {
                                break;
                            }
                            sb.append(normalized).append('\n');
                            used += tokens;
                            added++;
                        }
                        context = sb.toString();
                        log.info("[ChatService] RAG packing: results={} addedChunks={} contextTokensUsed={} budget={} currentTokens={} prefixTokens={} reserve={} headroom={} ",
                                (results == null ? 0 : results.size()), added, used, budget, currentTokens, prefixTokens, reserveCompletionTokens, reserveHeadroomTokens);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("[ChatService] RAG search failed: {}", e.getMessage());
            }
        }

        // Add context as a system message if found
        if (!context.isBlank()) {
            String prefix = (ragContextPrompt != null ? ragContextPrompt.trim() : "");
            if (!prefix.isEmpty()) {
                springMessages.add(0, new SystemMessage(prefix + "\n" + context));
            } else {
                springMessages.add(0, new SystemMessage(context));
            }
        } else if (context.isBlank()) {
            log.info("[ChatService] No context found for prompt: '{}'. RAG enabled but no relevant chunks found.", userPrompt);
        }
        return springMessages;
    }

}
