package io.github.jrohila.simpleragserver.repository;

import io.github.jrohila.simpleragserver.client.hf.HfModelInfo;
import io.github.jrohila.simpleragserver.domain.HfModelEntity;
import java.util.List;
import java.util.stream.Collectors;

public class HfModelConverter {

    public static HfModelEntity convert(HfModelInfo info) {
        if (info == null) return null;
        HfModelEntity e = new HfModelEntity();
        e.setId(info.getId());
        e.setAuthor(info.getAuthor());
        e.setTags(info.getTags());
        e.setPipelineTags(info.getPipelineTags());
        e.setTotalWeightBytes(info.getTotalWeightBytes());
        e.setTotalWeightMB(info.getTotalWeightMB());
        e.setHasOnnx(info.isHasOnnx());
        if (info.getFiles() != null) {
            List<HfModelEntity.HfModelFile> files = info.getFiles().stream().map(f -> {
                HfModelEntity.HfModelFile mf = new HfModelEntity.HfModelFile();
                mf.setFilename(f.getFilename());
                mf.setUrl(f.getUrl());
                return mf;
            }).collect(Collectors.toList());
            e.setFiles(files);
        }
        return e;
    }

}
