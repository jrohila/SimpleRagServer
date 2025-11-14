// ...existing code...
package io.github.jrohila.simpleragserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Value("${processing.chat.out-of-scope-message}")
    private String outOfScopeMessage;

    @Value("${processing.chat.welcome-message}")
    private String welcomeMessage;

    @Value("${processing.chat.system.prompt}")
    private String systemPrompt;
    @Value("${processing.chat.system.append}")
    private String systemPromptAppend;

    @Value("${processing.chat.rag.context-prompt}")
    private String contextPrompt;

    @Value("${processing.chat.rag.memory-prompt}")
    private String memoryPrompt;

    @Value("${processing.post.chat.fact.extractor.append}")
    private String extractorPrompt;

    @Value("${processing.chat.rag.out-of-scope-prompt}")
    private String outOfScopePrompt;

    @Bean
    public OpenApiCustomizer onboardingDtoExampleCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components != null && components.getSchemas() != null) {
                Schema<?> onboardingSchema = components.getSchemas().get("OnboardingDTO");
                if (onboardingSchema != null) {
                    onboardingSchema.getProperties().put("publicName", new StringSchema().example("MyChat"));
                    onboardingSchema.getProperties().put("internalName", new StringSchema().example("My Chat Assistant"));
                    onboardingSchema.getProperties().put("internalDescription", new StringSchema().example("General purpose chat assistant using RAG to power MyChat."));
                    onboardingSchema.getProperties().put("defaultLanguage", new StringSchema().example("en"));
                    onboardingSchema.getProperties().put("defaultSystemPrompt", new StringSchema().example(systemPrompt));
                    onboardingSchema.getProperties().put("defaultSystemPromptAppend", new StringSchema().example(systemPromptAppend));
                    onboardingSchema.getProperties().put("defaultContextPrompt", new StringSchema().example(contextPrompt));
                    onboardingSchema.getProperties().put("defaultMemoryPrompt", new StringSchema().example(memoryPrompt));
                    onboardingSchema.getProperties().put("defaultExtractorPrompt", new StringSchema().example(extractorPrompt));
                    onboardingSchema.getProperties().put("defaultOutOfScopeContext", new StringSchema().example(outOfScopePrompt));
                    onboardingSchema.getProperties().put("defaultOutOfScopeMessage", new StringSchema().example(outOfScopeMessage));
                    onboardingSchema.getProperties().put("welcomeMessage", new StringSchema().example(welcomeMessage));
                    onboardingSchema.getProperties().put("collectionName", new StringSchema().example("Onboarding Collection"));
                    onboardingSchema.getProperties().put("collectionDescription", new StringSchema().example("A collection for onboarding documents."));
                    onboardingSchema.getProperties().put("overrideSystemMessage", new Schema<Boolean>().type("boolean").example(true));
                    onboardingSchema.getProperties().put("overrideAssistantMessage", new Schema<Boolean>().type("boolean").example(true));
                }
                // Set default values for request params in the operation parameters
                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getOperationId() != null && operation.getOperationId().contains("createNewChat")) {
                            if (operation.getParameters() != null) {
                                operation.getParameters().forEach(param -> {
                                    if (param.getName().equals("publicName")) {
                                        param.setExample("MyChat");
                                    }
                                    if (param.getName().equals("internalName")) {
                                        param.setExample("My Chat Assistant");
                                    }
                                    if (param.getName().equals("internalDescription")) {
                                        param.setExample("General purpose chat assistant using RAG to power MyChat.");
                                    }
                                    if (param.getName().equals("defaultLanguage")) {
                                        param.setExample("en");
                                    }
                                    if (param.getName().equals("defaultSystemPrompt")) {
                                        param.setExample(systemPrompt);
                                    }
                                    if (param.getName().equals("defaultSystemPromptAppend")) {
                                        param.setExample(systemPromptAppend);
                                    }
                                    if (param.getName().equals("defaultContextPrompt")) {
                                        param.setExample(contextPrompt);
                                    }
                                    if (param.getName().equals("defaultMemoryPrompt")) {
                                        param.setExample(memoryPrompt);
                                    }
                                    if (param.getName().equals("defaultExtractorPrompt")) {
                                        param.setExample(extractorPrompt);
                                    }
                                    if (param.getName().equals("defaultOutOfScopeContext")) {
                                        param.setExample(outOfScopePrompt);
                                    }
                                    if (param.getName().equals("defaultOutOfScopeMessage")) {
                                        param.setExample(outOfScopeMessage);
                                    }
                                    if (param.getName().equals("welcomeMessage")) {
                                        param.setExample(welcomeMessage);
                                    }
                                    if (param.getName().equals("collectionName")) {
                                        param.setExample("Onboarding Collection");
                                    }
                                    if (param.getName().equals("collectionDescription")) {
                                        param.setExample("A collection for onboarding documents.");
                                    }
                                    if (param.getName().equals("overrideSystemMessage")) {
                                        param.setExample(true);
                                    }
                                    if (param.getName().equals("overrideAssistantMessage")) {
                                        param.setExample(true);
                                    }
                                });
                            }
                        }
                    });
                });
            }
        };
    }
}
