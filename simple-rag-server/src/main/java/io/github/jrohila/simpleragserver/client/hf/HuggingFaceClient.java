package io.github.jrohila.simpleragserver.client.hf;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class HuggingFaceClient {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceClient.class);

    private static final String BASE = "https://huggingface.co/api";
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String token;

    public HuggingFaceClient(@Value("${HF_TOKEN:}") String hfToken) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.token = (hfToken != null && !hfToken.isEmpty()) ? hfToken : null;
    }

    public List<HfModelInfo> getListOfWebGpuModels() throws IOException, InterruptedException {
        // Delegate to the Link-header based paging method with default filters for ONNX/community text-generation
        logger.info("Fetching Hugging Face model list (author=onnx-community, pipeline=text-generation) using Link-header pagination");
        Map<String, String> params = Map.of(
                "author", "onnx-community",
                "pipeline_tag", "text-generation",
                "limit", "100",
                "sort", "downloads",
                "direction", "-1"
        );
        return getModelsFromHub(params);
    }

    /**
     * Fetch models from Hugging Face Hub using Link-header pagination and arbitrary query params.
     * Example params: search, author, filter (tags), sort, direction, limit, full, config
     */
    public List<HfModelInfo> getModelsFromHub(Map<String, String> queryParams) throws IOException, InterruptedException {
        // If caller specified an 'author' (organization/user), prefer the org 'models-json' endpoint
        if (queryParams != null && queryParams.containsKey("author")) {
            String org = queryParams.get("author");
            if (org != null && !org.isEmpty()) {
                logger.info("Author param present ({}); using organization models-json endpoint", org);
                return getModelsFromOrganizationModelsJson(org, queryParams);
            }
        }

        List<String> ids = new ArrayList<>();
        String url = BASE + "/models" + buildQueryString(queryParams);
        logger.debug("Starting hub list fetch from {}", url);

        int page = 0;
        final int maxPages = 200; // generous safety cap
        while (page < maxPages) {
            logger.info("Fetching models page {} from {}", page + 1, url);
            HttpResponse<String> resp = sendGetResponse(url);
            if (resp.statusCode() / 100 != 2) {
                String body = resp.body();
                logger.warn("Non-2xx response from HF API: status={} url={} body-trim={}", resp.statusCode(), url,
                        body == null ? "<null>" : (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                throw new IOException("HF API HTTP " + resp.statusCode() + ": " + body);
            }
            JsonNode list = null;
            try {
                list = mapper.readTree(resp.body());
            } catch (IOException e) {
                logger.warn("Failed to parse JSON list response from {}: {}", url, e.toString());
                break;
            }
            if (list == null || !list.isArray() || list.size() == 0) {
                break;
            }
            int fetched = 0;
            for (JsonNode n : list) {
                String id = n.path("id").asText(null);
                if (id == null || id.isEmpty()) {
                    id = n.path("modelId").asText(null);
                }
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                    fetched++;
                }
            }
            logger.info("Page {} returned {} entries", page + 1, fetched);

            Optional<String> linkHeader = resp.headers().firstValue("Link");
            if (linkHeader.isPresent()) {
                String next = parseNextFromLinkHeader(linkHeader.get());
                if (next == null || next.isEmpty()) {
                    break;
                }
                url = next;
                page++;
                continue;
            } else {
                break;
            }
        }

        logger.info("Total model entries collected: {}", ids.size());

        ExecutorService ex = Executors.newFixedThreadPool(Math.min(8, Math.max(2, ids.size())));
        try {
            List<CompletableFuture<HfModelInfo>> futures = ids.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> {
                        try {
                            logger.debug("Fetching details for model {}", id);
                            return fetchModelInfo(id);
                        } catch (Exception e) {
                            logger.warn("Failed to fetch details for model {}: {}", id, e.toString());
                            return null;
                        }
                    }, ex))
                    .collect(Collectors.toList());
            List<HfModelInfo> results = new ArrayList<>();
            for (CompletableFuture<HfModelInfo> f : futures) {
                try {
                    HfModelInfo info = f.get();
                    if (info != null) {
                        results.add(info);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // skip failed
                }
            }
            return results;
        } finally {
            ex.shutdown();
        }
    }

    /**
     * Use the organization-specific JSON endpoint which provides paging metadata:
     * /api/organizations/{org}/models-json?p={page}&pipeline_tag=...&sort=...
     * Page parameter 'p' starts from 0.
     */
    public List<HfModelInfo> getModelsFromOrganizationModelsJson(String org, Map<String, String> queryParams) throws IOException, InterruptedException {
        List<String> ids = new ArrayList<>();
        String encodedOrg = URLEncoder.encode(org, StandardCharsets.UTF_8);
        // Determine whether caller requested a specific page (p). If so, fetch only that page.
        int requestedPage = -1;
        if (queryParams != null && queryParams.containsKey("p")) {
            try {
                requestedPage = Integer.parseInt(queryParams.get("p"));
            } catch (NumberFormatException nfe) {
                requestedPage = 0;
            }
        }

        int page = (requestedPage >= 0) ? requestedPage : 0;
        // only fetch a single page (no automatic collection of all pages)
        // build query string without 'p' and without 'author' or unsupported params like 'direction'
        Map<String, String> copy = new HashMap<>();
        if (queryParams != null) copy.putAll(queryParams);
        copy.remove("p");
        // remove author (we are already calling the org endpoint)
        copy.remove("author");
        // models-json endpoint may not accept 'direction' or 'limit' params; drop them if present
        if (copy.containsKey("direction")) {
            logger.debug("Dropping unsupported 'direction' param for organization models-json endpoint");
            copy.remove("direction");
        }
        if (copy.containsKey("limit")) {
            logger.debug("Dropping unsupported 'limit' param for organization models-json endpoint");
            copy.remove("limit");
        }
        String baseQuery = buildQueryString(copy);
        String url = BASE + "/organizations/" + encodedOrg + "/models-json" + baseQuery;
        if (baseQuery == null || baseQuery.isEmpty()) {
            url = url + "?p=" + page;
        } else {
            url = url + "&p=" + page;
        }

        logger.info("Fetching org models-json page {} from {}", page, url);
        HttpResponse<String> resp = sendGetResponse(url);
            if (resp.statusCode() / 100 != 2) {
                String body = resp.body();
                logger.warn("Non-2xx response from HF API: status={} url={} body-trim={}", resp.statusCode(), url,
                        body == null ? "<null>" : (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                throw new IOException("HF API HTTP " + resp.statusCode() + ": " + body);
            }
            JsonNode root;
            try {
                root = mapper.readTree(resp.body());
            } catch (IOException e) {
                logger.warn("Failed to parse JSON org list response from {}: {}", url, e.toString());
                return new ArrayList<>();
            }
            JsonNode models = root.path("models");
            if (models == null || !models.isArray() || models.size() == 0) {
                return new ArrayList<>();
            }
            int fetched = 0;
            for (JsonNode n : models) {
                String id = n.path("id").asText(null);
                if (id == null || id.isEmpty()) {
                    id = n.path("modelId").asText(null);
                }
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                    fetched++;
                }
            }
            logger.info("Org page {} returned {} entries", page, fetched);
        // done (single page)

        logger.info("Total org model entries collected: {}", ids.size());

        ExecutorService ex = Executors.newFixedThreadPool(Math.min(8, Math.max(2, ids.size())));
        try {
            List<CompletableFuture<HfModelInfo>> futures = ids.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> {
                        try {
                            logger.debug("Fetching details for model {}", id);
                            return fetchModelInfo(id);
                        } catch (Exception e) {
                            logger.warn("Failed to fetch details for model {}: {}", id, e.toString());
                            return null;
                        }
                    }, ex))
                    .collect(Collectors.toList());
            List<HfModelInfo> results = new ArrayList<>();
            for (CompletableFuture<HfModelInfo> f : futures) {
                try {
                    HfModelInfo info = f.get();
                    if (info != null) {
                        results.add(info);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // skip failed
                }
            }
            return results;
        } finally {
            ex.shutdown();
        }
    }

    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String parseNextFromLinkHeader(String header) {
        // Link: <https://huggingface.co/api/models?limit=100&offset=100>; rel="next", <...>; rel="last"
        String[] parts = header.split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.endsWith("rel=\"next\"") || p.contains("rel=\"next\"")) {
                int lt = p.indexOf('<');
                int gt = p.indexOf('>');
                if (lt >= 0 && gt > lt) {
                    return p.substring(lt + 1, gt);
                }
            }
        }
        return null;
    }

    private HttpResponse<String> sendGetResponse(String url) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30));
        if (token != null && !token.isEmpty()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpRequest req = b.build();
        logger.debug("Sending GET (resp) {}", url);
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HfModelInfo fetchModelInfo(String id) throws IOException, InterruptedException {
        String url = BASE + "/models/" + encodePathSegments(id);
        logger.info("Fetching model detail URL: {}", url);
        JsonNode details = sendGetJson(url);
        if (details == null) {
            return null;
        }

        HfModelInfo info = new HfModelInfo();
        info.setId(details.path("id").asText(id));
        info.setAuthor(details.path("author").asText(null));

        // tags / pipeline tags
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = details.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode t : tagsNode) {
                tags.add(t.asText());
            }
        }
        info.setTags(tags);

        List<String> pipelineTags = new ArrayList<>();
        JsonNode pnode = details.path("pipeline_tag");
        if (pnode.isTextual()) {
            pipelineTags.add(pnode.asText());
        }
        JsonNode pnodes = details.path("pipeline_tags");
        if (pnodes.isArray()) {
            for (JsonNode p : pnodes) {
                pipelineTags.add(p.asText());
            }
        }
        info.setPipelineTags(pipelineTags);

        // siblings/files
        List<HfModelFile> files = new ArrayList<>();
        long total = 0;
        boolean hasOnnx = false;
        JsonNode siblings = details.path("siblings");
        if (siblings.isArray()) {
            logger.debug("Raw siblings JSON for {}: {}", id, siblings.toString());
            for (JsonNode f : siblings) {
                String filename = f.path("rfilename").asText(null);
                if (filename == null || filename.isEmpty()) {
                    filename = f.path("filename").asText("");
                }

                // size can be directly under 'size' or inside 'lfs.size' for Git LFS objects
                long size = 0;
                if (f.hasNonNull("size")) {
                    size = f.path("size").asLong(0);
                } else if (f.has("lfs") && f.path("lfs").hasNonNull("size")) {
                    size = f.path("lfs").path("size").asLong(0);
                } else if (f.hasNonNull("bytes")) {
                    size = f.path("bytes").asLong(0);
                }

                // URL may be in several fields depending on API version
                String fileUrl = f.path("url").asText(null);
                if (fileUrl == null || fileUrl.isEmpty()) fileUrl = f.path("blob_url").asText(null);
                if (fileUrl == null || fileUrl.isEmpty()) fileUrl = f.path("raw_url").asText(null);
                if (fileUrl == null || fileUrl.isEmpty()) fileUrl = f.path("download_url").asText(null);
                if (fileUrl == null || fileUrl.isEmpty()) {
                    JsonNode lfs = f.path("lfs");
                    if (lfs.isObject() && lfs.hasNonNull("oid")) {
                        logger.debug("Sibling {} appears to be an LFS object with oid={}", filename, lfs.path("oid").asText(null));
                    }
                }

                HfModelFile mf = new HfModelFile();
                mf.setFilename(filename);
                mf.setUrl(fileUrl);
                files.add(mf);

                String lower = filename.toLowerCase();
                if (lower.endsWith(".onnx")) {
                    hasOnnx = true;
                    total += size;
                } else if (lower.endsWith(".safetensors") || lower.endsWith(".bin") || lower.endsWith(".pt")) {
                    total += size;
                }
            }
        }

        // If siblings did not expose per-file sizes, fall back to repository-wide usedStorage if available
        if (total <= 0) {
            long usedStorage = details.path("usedStorage").asLong(0);
            if (usedStorage > 0) {
                logger.info("No per-file sizes; using details.usedStorage={} for model {}", usedStorage, id);
                total = usedStorage;
            }
        }

        // If sibling entries didn't include direct URLs, construct a raw resolve URL for each file
        String encodedIdPath = encodePathSegments(id);
        for (HfModelFile mf : files) {
            if (mf.getUrl() == null || mf.getUrl().isEmpty()) {
                String filename = mf.getFilename() == null ? "" : mf.getFilename();
                if (!filename.isEmpty()) {
                    String rawUrl = "https://huggingface.co/" + encodedIdPath + "/resolve/main/" + encodePathSegments(filename);
                    mf.setUrl(rawUrl);
                    logger.debug("Constructed raw URL for {} -> {}", filename, rawUrl);
                }
            }
        }

        // No per-file HEAD checks performed (we avoid downloading or probing files)

        info.setFiles(files);
        info.setTotalWeightBytes(total);
        info.setHasOnnx(hasOnnx);
        info.setTotalWeightMB(total / (1024.0 * 1024.0));

        logger.debug("Model {} weight bytes={} hasOnnx={}", id, info.getTotalWeightBytes(), info.isHasOnnx());

        return info;
    }

    private String encodePathSegments(String id) {
        return Arrays.stream(id.split("/"))
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

    private JsonNode sendGetJson(String url) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30));
        if (token != null && !token.isEmpty()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpRequest req = b.build();
        logger.debug("Sending GET {}", url);
        HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
        logger.debug("Received response: status={} for {}", r.statusCode(), url);
        if (r.statusCode() / 100 != 2) {
            String body = r.body();
            logger.warn("Non-2xx response from HF API: status={} url={} body-trim={}", r.statusCode(), url, body == null ? "<null>" : (body.length() > 200 ? body.substring(0, 200) + "..." : body));
            throw new IOException("HF API HTTP " + r.statusCode() + ": " + body);
        }
        try {
            return mapper.readTree(r.body());
        } catch (IOException e) {
            logger.warn("Failed to parse JSON response from {}: {}", url, e.toString());
            return null;
        }
    }

    
}
