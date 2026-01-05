package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Jukka
 */
@Getter
@Setter
@ToString
public class LLMConfig {
    
    public static enum UseCase {
        RAG_QA,
        RAG_CONVERSATIONAL,
        RAG_SUMMARIZATION,
        RAG_EXTRACTION,
        RAG_CODE_ASSIST,
        CREATIVE_ASSIST,
        CLASSIFICATION,
    };
    
    private UseCase useCase;
    
    private Integer maxNewTokens;

    private Double temperature;

    private Boolean doSample;

    private Integer topK;

    private Double topP;

    private Double repetitionPenalty;

    private Integer minNewTokens;

}
