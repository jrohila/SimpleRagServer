package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration for WebGPU usage per-chat.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WebGpuConfig {

    /** Model id to use for WebGPU (e.g. onnx-community/Model-Name) */
    private String modelId;

    private String systemPrompt;
    private boolean overrideParentSystemPrompt = false;

    private String systemPromptAppend;
    private boolean overrideParentSystemPromptAppend = false;

    private String contextPrompt;
    private boolean overrideParentContextPrompt = false;

    private String memoryPrompt;
    private boolean overrideParentMemoryPrompt = false;

    private String extractorPrompt;
    private boolean overrideParentExtractorPrompt = false;

    /** If true, apply prompt rewriting for WebGPU prompts. */
    private boolean usePromptRewriting = false;

    /** Optional prompt used to rewrite user input before sending to the model. */
    private String userPromptRewritingPrompt;

    /** If true, override parent chat's user prompt rewriting prompt with this one. */
    private boolean overrideParentUserPromptRewriting = false;

}
