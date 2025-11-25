package io.github.jrohila.simpleragserver.factory;

import io.github.jrohila.simpleragserver.domain.LLMConfig;
import org.springframework.stereotype.Component;

/**
 * Factory for producing initialized {@link LLMConfig} instances for common use-cases.
 */
@Component
public class LLMConfigFactory {

    public LLMConfig create(LLMConfig.UseCase useCase) {
        LLMConfig cfg = new LLMConfig();
        cfg.setUseCase(useCase);

        // Common sensible defaults
        cfg.setTopK(50);
        cfg.setRepetitionPenalty(1.1);
        cfg.setMinNewTokens(1);

        switch (useCase) {
            case RAG_QA -> {
                cfg.setMaxNewTokens(384);
                cfg.setTemperature(0.1);
                cfg.setDoSample(false);
                cfg.setTopK(20);
                cfg.setTopP(0.9);
                cfg.setRepetitionPenalty(1.05);
                cfg.setMinNewTokens(1);
            }

            case RAG_CONVERSATIONAL -> {
                cfg.setMaxNewTokens(768);
                cfg.setTemperature(0.5);
                cfg.setDoSample(true);
                cfg.setTopK(50);
                cfg.setTopP(0.95);
                cfg.setRepetitionPenalty(1.1);
                cfg.setMinNewTokens(1);
            }

            case RAG_SUMMARIZATION -> {
                cfg.setMaxNewTokens(256);
                cfg.setTemperature(0.1);
                cfg.setDoSample(false);
                cfg.setTopK(20);
                cfg.setTopP(0.9);
                cfg.setRepetitionPenalty(1.0);
                cfg.setMinNewTokens(8);
            }

            case RAG_EXTRACTION -> {
                cfg.setMaxNewTokens(256);
                cfg.setTemperature(0.0);
                cfg.setDoSample(false);
                cfg.setTopK(10);
                cfg.setTopP(0.8);
                cfg.setRepetitionPenalty(1.0);
                cfg.setMinNewTokens(8);
            }

            case RAG_CODE_ASSIST -> {
                cfg.setMaxNewTokens(1024);
                cfg.setTemperature(0.1);
                cfg.setDoSample(false);
                cfg.setTopK(20);
                cfg.setTopP(0.9);
                cfg.setRepetitionPenalty(1.0);
                cfg.setMinNewTokens(1);
            }

            case CREATIVE_ASSIST -> {
                cfg.setMaxNewTokens(2048);
                cfg.setTemperature(0.9);
                cfg.setDoSample(true);
                cfg.setTopK(200);
                cfg.setTopP(0.95);
                cfg.setRepetitionPenalty(1.0);
                cfg.setMinNewTokens(8);
            }

            case CLASSIFICATION -> {
                cfg.setMaxNewTokens(32);
                cfg.setTemperature(0.0);
                cfg.setDoSample(false);
                cfg.setTopK(5);
                cfg.setTopP(0.8);
                cfg.setRepetitionPenalty(1.0);
                cfg.setMinNewTokens(1);
            }
        }

        return cfg;
    }
}
