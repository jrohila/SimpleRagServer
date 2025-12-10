package io.github.jrohila.simpleragserver.client.hf;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class HfModelInfo {
    private String id;
    private String author;
    private List<String> tags;
    private List<String> pipelineTags;
    private List<HfModelFile> files;
    private long totalWeightBytes;
    private boolean hasOnnx;
    private double totalWeightMB;
}
