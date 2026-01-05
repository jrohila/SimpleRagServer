package io.github.jrohila.simpleragserver.domain;

// Jackson annotations removed for SimpleRagData module
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class DoclingConversionRequest {

    // Matches OpenAPI: ConvertDocumentsRequest
    private Options options = Options.defaultForRag();
    private List<SourceInput> sources;

    // Optional: target (defaults to inbody on server). Not required to set.
    // @JsonProperty("target")
    // private Target target;

    // Factory methods for convenience
    public static DoclingConversionRequest fromUrl(String url) {
        DoclingConversionRequest request = new DoclingConversionRequest();
        SourceInput source = new SourceInput();
        source.setKind("http");
        source.setUrl(url);
        request.setSources(List.of(source));
        return request;
    }

    public static DoclingConversionRequest fromBase64(String filename, String base64Content) {
        DoclingConversionRequest request = new DoclingConversionRequest();
        SourceInput source = new SourceInput();
        source.setKind("file");
        source.setFilename(filename);
        source.setBase64String(base64Content);
        request.setSources(List.of(source));
        return request;
    }
}