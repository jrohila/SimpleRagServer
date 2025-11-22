/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private String publicName;

    private String internalName;

    private String internalDescription;

    private String defaultLanguage;

    private String defaultCollectionId;

    private String defaultSystemPrompt;

    private String defaultSystemPromptAppend;

    private String defaultContextPrompt;

    private String defaultMemoryPrompt;

    private String defaultExtractorPrompt;

    private boolean overrideSystemMessage;
    private boolean overrideAssistantMessage;

    private boolean useUserPromptRewriting;
    private String userPromptRewritingPrompt;

    private String defaultOutOfScopeMessage;

    private String welcomeMessage;

}
