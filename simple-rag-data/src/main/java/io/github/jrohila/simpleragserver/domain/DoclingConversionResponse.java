package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class DoclingConversionResponse {
    
    private DocumentContent document;
    private String status;
    private List<String> errors;
    private Double processingTime;
    private Map<String, Object> timings;
    
    @Getter
    @Setter
    @ToString
    public static class DocumentContent {
        private String filename;
        private String mdContent;
        private JsonNode jsonContent; // Structured document data
        private String htmlContent;
        private String textContent;
        private String doctagsContent;
        
        // Helper methods
        public boolean hasJsonContent() {
            return jsonContent != null && !jsonContent.isNull();
        }
        
        public boolean hasMarkdownContent() {
            return mdContent != null && !mdContent.trim().isEmpty();
        }
        
        public boolean hasTextContent() {
            return textContent != null && !textContent.trim().isEmpty();
        }
        
        // Get the best available content for RAG (prioritize structured JSON, then markdown, then text)
        public String getBestContentForRag() {
            if (hasJsonContent()) {
                return jsonContent.toString();
            } else if (hasMarkdownContent()) {
                return mdContent;
            } else if (hasTextContent()) {
                return textContent;
            }
            return "";
        }
    }
    
    // Helper methods
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}