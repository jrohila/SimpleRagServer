/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.pipeline;

import io.github.jrohila.simpleragserver.entity.ChunkEntity;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Applies simple quality checks to chunks before they are embedded and stored.
 * Currently: require at least 3 sentences in the chunk text.
 */
@Component
public class ChunkQualityGate {

	private static final Logger LOGGER = Logger.getLogger(ChunkQualityGate.class.getName());

	/** Minimum number of sentences required for a chunk to pass. */
	private static final int MIN_SENTENCES = 3;

	/**
	 * Returns true if the chunk passes quality checks; false if it should be dropped.
	 *
	 * Current rules:
	 * - Text must contain at least 3 sentences.
	 */
	public boolean filter(ChunkEntity chunk) {
		if (chunk == null) {
			return false;
		}
		String text = chunk.getText();
		int sentenceCount = countSentences(text);
		boolean ok = sentenceCount >= MIN_SENTENCES;
		if (!ok) {
			LOGGER.fine(() -> "ChunkQualityGate: rejected chunk due to low sentence count=" + sentenceCount);
		}
		return ok;
	}

	/**
	 * Counts sentences using Java's BreakIterator to handle locale-aware boundaries.
	 * Null or blank text yields 0.
	 */
	static int countSentences(String text) {
		if (text == null) {
			return 0;
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		BreakIterator it = BreakIterator.getSentenceInstance(Locale.ROOT);
		it.setText(trimmed);
		int count = 0;
		int start = it.first();
		for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
			// Ignore whitespace-only "sentences"
			if (!trimmed.substring(start, end).trim().isEmpty()) {
				count++;
			}
		}
		return count;
	}
}
