/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragnlp.impl;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import io.github.jrohila.simpleragnlp.StanfordCoreNlpFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jukka
 */
public class TermFinderSCNImpl {

    private static final Logger log = LoggerFactory.getLogger(TermFinderSCNImpl.class);

    public static List<String> extractTerms(String text) {
        List<String> terms = new ArrayList<>();
        try {
            long t0 = System.nanoTime();
            log.debug("StanfordCoreNLP term extraction: inputLen={}", text == null ? 0 : text.length());

            if (text == null || text.isBlank()) {
                return terms;
            }

            StanfordCoreNLP pipeline = StanfordCoreNlpFactory.getInstance().getStanfordPipeline();
            Annotation doc = new Annotation(text);
            pipeline.annotate(doc);

            LinkedHashSet<String> unique = new LinkedHashSet<>();

            List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
            int sentenceCount = sentences == null ? 0 : sentences.size();
            if (sentences != null) {
                for (CoreMap sentence : sentences) {
                    SemanticGraph graph = getDependencyGraph(sentence);
                    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                    if (graph == null || tokens == null || tokens.isEmpty()) {
                        continue;
                    }

                    for (CoreLabel tok : tokens) {
                        String pos = tok.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                        if (!isNoun(pos)) {
                            continue;
                        }

                        var node = graph.getNodeByIndexSafe(tok.index());
                        if (node == null) {
                            continue;
                        }

                        // Skip if this noun is itself a dependent in [compound|flat|fixed|goeswith]
                        SemanticGraphEdge incoming = graph.incomingEdgeList(node).stream().findFirst().orElse(null);
                        if (incoming != null) {
                            String relIn = incoming.getRelation().getShortName();
                            if (relIn.equals("compound") || relIn.equals("flat") || relIn.equals("fixed") || relIn.equals("goeswith")) {
                                continue;
                            }
                        }

                        // Check for light scaffolding via UD nmod+case OR SD prep_of
                        List<CoreLabel> promoted = new ArrayList<>();
                        boolean scaffold = false;
                        for (var e : graph.outgoingEdgeList(node)) {
                            if (isUdNmodWithCase(graph, e) || isPrepOfEdge(e)) {
                                var child = e.getDependent();
                                String childPos = child.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                                if (isNoun(childPos)) {
                                    scaffold = true;
                                    promoted.add(child.backingLabel());
                                }
                            }
                        }

                        if (scaffold && !promoted.isEmpty()) {
                            for (CoreLabel child : promoted) {
                                String phrase = buildNounPhrase(tokens, graph, child);
                                if (!phrase.isBlank()) {
                                    unique.add(phrase);
                                }
                            }
                            continue;
                        }

                        String phrase = buildNounPhrase(tokens, graph, node.backingLabel());
                        if (!phrase.isBlank()) {
                            unique.add(phrase);
                        }
                    }
                }
            }

            long tookMs = (System.nanoTime() - t0) / 1_000_000L;
            log.info("StanfordCoreNLP term extraction: sentences={} terms={} took={}ms", sentenceCount, unique.size(), tookMs);
            log.debug("StanfordCoreNLP terms={}", unique);

            terms.addAll(unique);
        } catch (Throwable t) {
            log.warn("StanfordCoreNLP extraction failed: {}", t.toString());
        }
        return terms;
    }

    // Prefer UD Enhanced++ if present; else use collapsed CC-processed dependencies from constituency parse
    private static SemanticGraph getDependencyGraph(CoreMap sentence) {
        SemanticGraph g = sentence.get(SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation.class);
        if (g != null) {
            return g;
        }
        g = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
        if (g != null) {
            return g;
        }
        // Last resort
        return sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
    }

    // UD: nmod child with a 'case' dependent (preposition)
    private static boolean isUdNmodWithCase(SemanticGraph graph, SemanticGraphEdge e) {
        if (e == null) {
            return false;
        }
        if (!"nmod".equals(e.getRelation().getShortName())) {
            return false;
        }
        var child = e.getDependent();
        if (child == null) {
            return false;
        }
        return graph.outgoingEdgeList(child).stream()
                .anyMatch(ed -> "case".equals(ed.getRelation().getShortName()));
    }

    // SD: collapsed preposition edge like prep_of (shortName may be "prep" with specific "of", or "prep_of")
    private static boolean isPrepOfEdge(SemanticGraphEdge e) {
        if (e == null) {
            return false;
        }
        String shortName = e.getRelation().getShortName();
        String specific = e.getRelation().getSpecific();
        if ("prep".equals(shortName) && "of".equalsIgnoreCase(specific)) {
            return true;
        }
        return "prep_of".equalsIgnoreCase(shortName) || "case:of".equalsIgnoreCase(shortName);
    }

    // Build NP: collect amod/compound/flat(:name)/nummod modifiers around the head; join in surface order.
    private static String buildNounPhrase(List<CoreLabel> sentTokens, SemanticGraph graph, CoreLabel head) {
        if (head == null) {
            return "";
        }
        int headIdx = head.index();

        // Collect indices: modifiers + head
        ArrayList<Integer> idxs = new ArrayList<>();
        idxs.add(headIdx);

        var headNode = graph.getNodeByIndexSafe(headIdx);
        if (headNode != null) {
            for (var e : graph.outgoingEdgeList(headNode)) {
                String rel = e.getRelation().getShortName();
                String specific = e.getRelation().getSpecific();
                if (rel.equals("amod") || rel.equals("compound") || rel.equals("nummod") || rel.equals("flat")) {
                    idxs.add(e.getDependent().index());
                } else if (rel.equals("flat") && "name".equals(specific)) {
                    idxs.add(e.getDependent().index());
                }
            }
        }

        idxs.sort(Integer::compareTo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < idxs.size(); i++) {
            int idx = idxs.get(i);
            if (idx < 1 || idx > sentTokens.size()) {
                continue;
            }
            String w = sentTokens.get(idx - 1).get(CoreAnnotations.TextAnnotation.class);
            if (w == null || w.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(w);
        }
        return sb.toString();
    }

    private static boolean isNoun(String tag) {
        // Support both UD (NOUN, PROPN) and PTB (NN, NNS, NNP, NNPS)
        if (tag == null) {
            return false;
        }
        return tag.equals("NOUN") || tag.equals("PROPN") || tag.startsWith("NN");
    }
}
