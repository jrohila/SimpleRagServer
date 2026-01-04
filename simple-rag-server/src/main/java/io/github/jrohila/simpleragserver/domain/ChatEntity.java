package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatEntity {

    private String id;

    @JsonProperty("publicName")
    private String publicName;

    @JsonProperty("internalName")
    private String internalName;

    @JsonProperty("internalDescription")
    private String internalDescription;

    @JsonProperty("defaultLanguage")
    private String defaultLanguage;

    @JsonProperty("defaultCollectionId")
    private String defaultCollectionId;

    @JsonProperty("defaultSystemPrompt")
    private String defaultSystemPrompt;

    @JsonProperty("defaultSystemPromptAppend")
    private String defaultSystemPromptAppend;

    @JsonProperty("defaultContextPrompt")
    private String defaultContextPrompt;

    @JsonProperty("defaultMemoryPrompt")
    private String defaultMemoryPrompt;

    @JsonProperty("defaultExtractorPrompt")
    private String defaultExtractorPrompt;

    private boolean overrideSystemMessage;
    private boolean overrideAssistantMessage;

    private boolean useUserPromptRewriting;
    private String userPromptRewritingPrompt;

    @JsonProperty("defaultOutOfScopeMessage")
    private String defaultOutOfScopeMessage;

    private String welcomeMessage;
    
    private LLMConfig llmConfig;

    private WebGpuConfig webGpuConfig;

}
