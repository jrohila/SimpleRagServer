/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import io.github.jrohila.simpleragserver.dto.MessageDTO;
import io.github.jrohila.simpleragserver.service.NlpService;
import io.github.jrohila.simpleragserver.service.NlpService.NlpEngine;
import io.github.jrohila.simpleragserver.service.util.SearchTerm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jukka
 */
@Component
public class BoostTermDetector {

    @Autowired
    private NlpService nlpService;

    public List<SearchTerm> buildSearchTerms(String query, List<MessageDTO> messages, Double queryTermWeight, Double userMessageWeight, Double assistantMessageWeight) {
        Set<String> terms = new HashSet<>();
        Map<String, SearchTerm> queryTermMap = new LinkedHashMap<>();
        Map<String, SearchTerm> userMessageTermMap = new LinkedHashMap<>();
        Map<String, SearchTerm> assistantMessageTermMap = new LinkedHashMap<>();    

        Thread termsFromQueryThread = new Thread(() -> {
            List<String> queryTerms = nlpService.extractCandidateTerms(query, NlpEngine.STANFORD_CORE_NLP);
            for (String queryTerm : queryTerms) {
                SearchTerm term = new SearchTerm();
                term.setTerm(queryTerm);
                term.setBoostWeight(queryTermWeight);
                term.setMandatory(false);
                queryTermMap.put(queryTerm, term);
                terms.add(queryTerm);
            }
        });
        termsFromQueryThread.start();

        Thread termsFromUserMessagesThread = new Thread(() -> {
            for (MessageDTO message : messages) {
                if (MessageDTO.Role.USER.equals(message.getRole())) {
                    List<String> messageTerms = nlpService.extractCandidateTerms(message.getContentAsString(), NlpEngine.STANFORD_CORE_NLP);
                    for (String messageTerm : messageTerms) {
                        SearchTerm term = new SearchTerm();
                        term.setTerm(messageTerm);
                        term.setBoostWeight(userMessageWeight);
                        term.setMandatory(false);
                        userMessageTermMap.put(messageTerm, term);
                    }
                }
            }
        });
        termsFromUserMessagesThread.start();

        Thread termsFromAssistantMessagesThread = new Thread(() -> {
            for (MessageDTO message : messages) {
                if (MessageDTO.Role.ASSISTANT.equals(message.getRole())) {
                    List<String> messageTerms = nlpService.extractCandidateTerms(message.getContentAsString(), NlpEngine.STANFORD_CORE_NLP);
                    for (String messageTerm : messageTerms) {
                        SearchTerm term = new SearchTerm();
                        term.setTerm(messageTerm);
                        term.setBoostWeight(assistantMessageWeight);
                        term.setMandatory(false);
                        assistantMessageTermMap.put(messageTerm, term);
                    }
                }
            }
        });
        termsFromAssistantMessagesThread.start();

        try {
            termsFromQueryThread.join();
            termsFromUserMessagesThread.join();
            termsFromAssistantMessagesThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Combine all terms into the results list
        for (String termKey : userMessageTermMap.keySet()) {
            if (!terms.contains(termKey)) {
                terms.add(termKey);
                queryTermMap.put(termKey, userMessageTermMap.get(termKey));
            }
        }
        for (String termKey : assistantMessageTermMap.keySet()) {
            if (!terms.contains(termKey)) {
                terms.add(termKey);
                queryTermMap.put(termKey, assistantMessageTermMap.get(termKey));
            }
        }

        return queryTermMap.values().stream().toList();
    }

}
