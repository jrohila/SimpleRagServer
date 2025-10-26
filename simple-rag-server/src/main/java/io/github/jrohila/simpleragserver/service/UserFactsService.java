/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.dto.ExtractedFactDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author Jukka
 */
@Service
public class UserFactsService {

    private static final Logger log = LoggerFactory.getLogger(UserFactsService.class);
    private final Map<Integer, List<ExtractedFactDTO>> chatFacts = new HashMap<>();

    public List<ExtractedFactDTO> getFacts(List<Integer> chatTokens) {
        List<ExtractedFactDTO> currentFacts = null;
        if ((chatTokens != null) && (!chatTokens.isEmpty())) {
            for (Integer chatToken : chatTokens) {
                List<ExtractedFactDTO> existing = chatFacts.get(chatToken);
                if (existing != null) {
                    currentFacts = existing; // reuse same reference to keep alias keys pointing to same list
                    break;
                }
            }
        }
        return currentFacts;
    }

    public void updateFacts(List<ExtractedFactDTO> facts, List<Integer> chatTokens) {
        if (facts == null || facts.isEmpty() || chatTokens == null || chatTokens.isEmpty()) {
            return;
        }
        // Find existing fact list by the first token that has data; else create new
        List<ExtractedFactDTO> currentFacts = null;
        for (Integer chatToken : chatTokens) {
            List<ExtractedFactDTO> existing = chatFacts.get(chatToken);
            if (existing != null) {
                currentFacts = existing; // reuse same reference to keep alias keys pointing to same list
                break;
            }
        }
        if (currentFacts == null) {
            currentFacts = new ArrayList<>();
        }

        for (ExtractedFactDTO incoming : facts) {
            if (incoming == null) {
                continue;
            }
            String subject = trimToEmpty(incoming.getSubject());
            String relation = trimToEmpty(incoming.getRelation());
            String value = trimToEmpty(incoming.getValue());
            String statement = trimToEmpty(incoming.getStatement());
            String confidence = trimToEmpty(incoming.getConfidence());
            String strategy = trimToEmpty(incoming.getMergeStrategy());

            boolean isOverwrite = strategy.equalsIgnoreCase("overwrite");
            boolean isMerge = !isOverwrite; // default to merge if not explicitly overwrite

            if (isOverwrite) {
                int idx = indexOfBySubjectRelation(currentFacts, subject, relation);
                if (idx >= 0) {
                    ExtractedFactDTO existing = currentFacts.get(idx);
                    existing.setStatement(statement);
                    existing.setConfidence(confidence);
                    existing.setMergeStrategy("overwrite");
                    if (log.isDebugEnabled()) {
                        log.debug("[UserFacts] Overwrite updated: subj='{}' rel='{}' stmt='{}'", subject, relation, statement);
                    }
                } else {
                    ExtractedFactDTO copy = copyOf(subject, relation, value, statement, confidence, "overwrite");
                    currentFacts.add(copy);
                    if (log.isDebugEnabled()) {
                        log.debug("[UserFacts] Overwrite added: subj='{}' rel='{}' stmt='{}'", subject, relation, statement);
                    }
                }
            } else if (isMerge) {
                int idx = indexOfBySubjectRelationStatement(currentFacts, subject, relation, statement);
                if (idx >= 0) {
                    // Duplicate exists; confirm and skip
                    if (log.isDebugEnabled()) {
                        log.debug("[UserFacts] Merge skipped duplicate: subj='{}' rel='{}' stmt='{}'", subject, relation, statement);
                    }
                } else {
                    ExtractedFactDTO copy = copyOf(subject, relation, value, statement, confidence, "merge");
                    currentFacts.add(copy);
                    if (log.isDebugEnabled()) {
                        log.debug("[UserFacts] Merge added: subj='{}' rel='{}' stmt='{}'", subject, relation, statement);
                    }
                }
            }
        }

        // Log out the updated currentFacts
        if (log.isInfoEnabled()) {
            log.info("[UserFacts] currentFacts updated: count={}", currentFacts.size());
            for (ExtractedFactDTO f : currentFacts) {
                log.info("[UserFacts]   -> {}", f);
            }
        }

        // Store the same list reference for all alias tokens
        for (Integer chatToken : chatTokens) {
            if (chatToken == null) {
                continue;
            }
            chatFacts.put(chatToken, currentFacts);
        }
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean eq(String a, String b) {
        return trimToEmpty(a).equalsIgnoreCase(trimToEmpty(b));
    }

    private static int indexOfBySubjectRelation(List<ExtractedFactDTO> facts, String subject, String relation) {
        if (facts == null) {
            return -1;
        }
        for (int i = 0; i < facts.size(); i++) {
            ExtractedFactDTO f = facts.get(i);
            if (f == null) {
                continue;
            }
            if (eq(f.getSubject(), subject) && eq(f.getRelation(), relation) && eq(f.getRelation(), relation)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfBySubjectRelationStatement(List<ExtractedFactDTO> facts, String subject, String relation, String statement) {
        if (facts == null) {
            return -1;
        }
        for (int i = 0; i < facts.size(); i++) {
            ExtractedFactDTO f = facts.get(i);
            if (f == null) {
                continue;
            }
            if (eq(f.getSubject(), subject) && eq(f.getRelation(), relation) && eq(f.getStatement(), statement)) {
                return i;
            }
        }
        return -1;
    }

    private static ExtractedFactDTO copyOf(String subject, String relation, String value, String statement, String confidence, String strategy) {
        ExtractedFactDTO f = new ExtractedFactDTO();
        f.setSubject(subject);
        f.setRelation(relation);
        f.setValue(value);
        f.setStatement(statement);
        f.setConfidence(confidence);
        f.setMergeStrategy(strategy);
        return f;
    }

}
