package io.github.jrohila.simpleragserver.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

public class OpenNlpRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // Register factory classes for reflection
        hints.reflection()
            .registerType(opennlp.tools.sentdetect.SentenceDetectorFactory.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
            .registerType(opennlp.tools.postag.POSTaggerFactory.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
            .registerType(opennlp.tools.langdetect.LanguageDetectorFactory.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
            .registerType(opennlp.tools.tokenize.TokenizerFactory.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)
            .registerType(opennlp.tools.util.BaseToolFactory.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);

        // Register ServiceLoader and jtokkit resources
        hints.resources()
            .registerPattern("META-INF/services/*")
            .registerPattern("com/knuddels/jtokkit/*.tiktoken");
    }
}
