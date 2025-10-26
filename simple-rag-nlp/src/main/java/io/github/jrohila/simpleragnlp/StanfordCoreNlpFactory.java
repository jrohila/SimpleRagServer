/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragnlp;

import io.github.jrohila.simpleragnlp.impl.TermFinderSCNImpl;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jukka
 */
public class StanfordCoreNlpFactory {

    private static final Logger log = LoggerFactory.getLogger(StanfordCoreNlpFactory.class);

    private static StanfordCoreNlpFactory instance;

    private volatile StanfordCoreNLP stanfordPipeline;

    private StanfordCoreNlpFactory() {
        Properties props = new Properties();
        props.setProperty("tokenize.language", "en");

        // POS model: prefer cased, fall back to caseless
        String posModel = firstAvailableResource(
                "edu/stanford/nlp/models/pos-tagger/english-left3words-distsim.tagger",
                "edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger"
        );
        props.setProperty("pos.model", posModel);

        // Prefer UD depparse; otherwise fallback to constituency parse (SR)
        String depparseModel = firstAvailableResourceOrNull(
                "edu/stanford/nlp/models/parser/nndep/english_UD.gz",
                "edu/stanford/nlp/models/parser/nndep/UD_English.gz",
                "edu/stanford/nlp/models/parser/nndep/english_SD.gz"
        );

        if (depparseModel != null) {
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse");
            props.setProperty("depparse.model", depparseModel);
            log.info("StanfordCoreNLP: using depparse model={}", depparseModel);
        } else {
            // SR parser models in your JAR
            String parseModel = firstAvailableResource(
                    "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
                    "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"
            );
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse");
            props.setProperty("parse.model", parseModel);
            // Optional: cap sentence length for speed
            props.setProperty("parse.maxlen", "100");
            log.info("StanfordCoreNLP: using parse model={}", parseModel);
        }

        this.stanfordPipeline = new StanfordCoreNLP(props);
        log.info("StanfordCoreNLP initialized. pos.model={}", posModel);
    }

    public StanfordCoreNLP getStanfordPipeline() {
        return stanfordPipeline;
    }

    public List<String> extractTerms(String text) {
        return TermFinderSCNImpl.extractTerms(text);
    }

    public static StanfordCoreNlpFactory getInstance() {
        if (instance == null) {
            instance = new StanfordCoreNlpFactory();
        }
        return instance;
    }

    private String firstAvailableResourceOrNull(String... candidates) {
        ClassLoader cl = getClass().getClassLoader();
        for (String c : candidates) {
            if (cl.getResource(c) != null) {
                return c;
            }
        }
        return null;
    }

    private String firstAvailableResource(String... candidates) {
        ClassLoader cl = getClass().getClassLoader();
        for (String c : candidates) {
            if (cl.getResource(c) != null) {
                return c;
            }
        }
        throw new IllegalStateException("CoreNLP resource not found on classpath. Tried: " + java.util.Arrays.toString(candidates));
    }

}
