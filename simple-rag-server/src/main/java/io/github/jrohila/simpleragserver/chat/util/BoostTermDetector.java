/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.chat.util;

import io.github.jrohila.simpleragserver.service.NlpService;
import io.github.jrohila.simpleragserver.service.NlpService.NlpEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
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
    
    public List<BoostTerm> getBoostTermsByNouns(List<Message> messages) {
        // Map term -> [count, oldestMessageIndex, newestMessageIndex]
        Map<String, int[]> termStats = new HashMap<>();
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (!(MessageType.USER.equals(message.getMessageType()))) {
                continue; // Only consider conversational content
            }
                        
            List<String> candidates = nlpService.extractCandidateTerms(message.getText(), NlpEngine.STANFORD_CORE_NLP);
            if (candidates == null) continue;
            for (String candidate : candidates) {
                if (candidate == null || candidate.isBlank()) continue;
                String term = candidate.trim();
                int[] stats = termStats.get(term);
                if (stats == null) {
                    // count=1, oldest=i, newest=i
                    termStats.put(term, new int[]{1, i, i});
                } else {
                    stats[0]++; // increment count
                    if (i < stats[1]) stats[1] = i; // update oldest
                    if (i > stats[2]) stats[2] = i; // update newest
                }
            }
        }
        if (termStats.isEmpty()) {
            return new ArrayList<>();
        }
        // Determine global min/max counts for normalization use later
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        for (int[] stats : termStats.values()) {
            int c = stats[0];
            if (c < globalMin) globalMin = c;
            if (c > globalMax) globalMax = c;
        }
        List<BoostTerm> boostTerms = new ArrayList<>(termStats.size());
        for (Map.Entry<String, int[]> e : termStats.entrySet()) {
            BoostTerm bt = new BoostTerm();
            bt.setTerm(e.getKey());
            bt.setCount(e.getValue()[0]);
            bt.setOldestMessage(e.getValue()[1]);
            bt.setNewestMessage(e.getValue()[2]);
            bt.setMinCount(globalMin);
            bt.setMaxCount(globalMax);
            boostTerms.add(bt);
        }
        // Sort by descending count; tie-break by earliest oldestMessage (terms introduced earlier first)
        boostTerms.sort((a, b) -> {
            int cmp = Integer.compare(b.getCount(), a.getCount());
            if (cmp != 0) return cmp;
            return Integer.compare(a.getOldestMessage(), b.getOldestMessage());
        });
        return boostTerms;
    }

    public List<String> popularNouns(List<Message> messages) {
        Map<String, Integer> terms = new HashMap<String, Integer>();
        
        for (Message message : messages) {
            if (MessageType.USER.equals(message.getMessageType()) || MessageType.ASSISTANT.equals(message.getMessageType())) {
                List<String> candidates = nlpService.extractCandidateTerms(message.getText(), NlpEngine.STANFORD_CORE_NLP);
                if (candidates == null) continue;
                for (String candidate : candidates) {
                    if (candidate == null || candidate.isBlank()) continue;
                    terms.merge(candidate, 1, Integer::sum);
                }
            }            
        }
        // Order terms by highest count first
        List<String> ordered = new ArrayList<>(terms.keySet());
        ordered.sort((a, b) -> Integer.compare(terms.getOrDefault(b, 0), terms.getOrDefault(a, 0)));
        return ordered;
    }
    
    
}
