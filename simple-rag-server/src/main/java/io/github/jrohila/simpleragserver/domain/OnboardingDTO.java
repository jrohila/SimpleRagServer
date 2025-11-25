package io.github.jrohila.simpleragserver.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OnboardingDTO {
    // ChatEntity fields (without id)

    @Schema(example = "My Public Chat")
    private String publicName;

    @Schema(example = "internal_chat_name")
    private String internalName;

    @Schema(example = "A chat for onboarding new users")
    private String internalDescription;

    @Schema(example = "en")
    private String defaultLanguage;

    @Schema(example = "You are a helpful assistant.")
    private String defaultSystemPrompt;

    @Schema(example = "Follow these universal behavioral and compliance rules: 1. Maintain a professional, respectful, and neutral tone. ...")
    private String defaultSystemPromptAppend;

    @Schema(example = "You are given two information sources: 1. A set of context documents retrieved from a knowledge base. 2. The memory JSON, which contains relevant user and entity facts. Use both to generate your response. ...")
    private String defaultContextPrompt;

    @Schema(example = "You are provided with a JSON object representing the user's long-term memory state. This memory contains factual information about the user and other known entities, expressed as discrete facts. ...")
    private String defaultMemoryPrompt;

    @Schema(example = "Extract and normalize user facts from the message.")
    private String defaultExtractorPrompt;

    @Schema(example = "true")
    private Boolean useUserPromptRewriting;

    @Schema(example = "You are a prompt rewriter. Given the user's latest message and the last assistant response, rewrite the user's request so it is clearer, more explicit, and unambiguous while preserving the original intent. Use the assistant's last response only as context for understanding what the user likely wants next. Do not add new requirements that the user did not imply. Resolve pronouns and vague references (like \"this\", \"that\", \"the above\") into explicit descriptions. Respond with a single rewritten prompt only, no explanations or extra text.")
    private String userPromptRewritingPrompt;

    // CollectionEntity fields (without id)
    @Schema(example = "Onboarding Collection")
    private String collectionName;

    @Schema(example = "A collection for onboarding documents.")
    private String collectionDescription;
    // Add more fields as needed from CollectionEntity
}
