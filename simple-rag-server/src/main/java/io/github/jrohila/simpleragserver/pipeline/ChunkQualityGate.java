package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import io.github.jrohila.simpleragserver.service.NlpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Applies quality filters to generated chunks before downstream processing.
 */
@Service
public class ChunkQualityGate {

    private static final Logger log = LoggerFactory.getLogger(ChunkQualityGate.class);

    @Autowired
    private NlpService nlpService;

    /**
     * Filters the provided chunks and returns a new list.
     * Current rule: drop any chunk whose language equals "und" (undetermined),
     * logging each filtered chunk.
     *
     * @param chunks input list of chunks (may be empty)
     * @return filtered list of chunks (never null)
     */
    public List<ChunkEntity> filter(List<ChunkEntity> chunks) {
        chunks = this.filterOutUndeterminedLanguage(chunks);
    chunks = this.filterByEnglishSentenceCount(chunks, 3);

        return chunks;
    }

    private List<ChunkEntity> filterOutUndeterminedLanguage(List<ChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChunkEntity> kept = new ArrayList<>(chunks.size());
        for (ChunkEntity ch : chunks) {
            String lang = ch.getLanguage();
            if ("und".equalsIgnoreCase(lang)) {
                log.info("Chunk filtered out due to undetermined language: id={}, sectionTitle='{}', preview='{}'",
                        ch.getId(),
                        safe(ch.getSectionTitle()),
                        preview(ch.getText()));
                continue;
            }
            kept.add(ch);
        }
        return kept;
    }

    private List<ChunkEntity> filterByEnglishSentenceCount(List<ChunkEntity> chunks, int minSentences) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChunkEntity> kept = new ArrayList<>(chunks.size());
        for (ChunkEntity ch : chunks) {
            // Only apply to English chunks
            if ("en".equalsIgnoreCase(ch.getLanguage())) {
                int count = nlpService.countEnglishSentences(safe(ch.getText()));
                if (count < minSentences) {
                    log.info("Chunk filtered out due to too few English sentences ({} < {}): id={}, sectionTitle='{}', preview='{}'",
                            count, minSentences, ch.getId(), safe(ch.getSectionTitle()), preview(ch.getText()));
                    continue;
                }
            }
            kept.add(ch);
        }
        return kept;
    }


    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String preview(String s) {
        if (s == null) return "";
        String trimmed = s.replaceAll("\n", " ").trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed;
    }
}
