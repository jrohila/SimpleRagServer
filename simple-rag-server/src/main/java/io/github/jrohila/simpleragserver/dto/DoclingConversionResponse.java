package io.github.jrohila.simpleragserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("document")
    private DocumentContent document;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    @JsonProperty("processing_time")
    private Double processingTime;
    
    @JsonProperty("timings")
    private Map<String, Object> timings;
    
    @Getter
    @Setter
    @ToString
    public static class DocumentContent {
        @JsonProperty("filename")
        private String filename;
        
        @JsonProperty("md_content")
        private String mdContent;
        
        @JsonProperty("json_content")
        private JsonNode jsonContent; // Structured document data
        
        @JsonProperty("html_content")
        private String htmlContent;
        
        @JsonProperty("text_content")
        private String textContent;
        
        @JsonProperty("doctags_content")
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