package io.github.jrohila.simpleragserver.client.hf;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HfModelFile {
    private String filename;
    private String url;
}
