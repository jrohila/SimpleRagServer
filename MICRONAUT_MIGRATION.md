# Micronaut Migration Plan for SimpleRagServer

## Overview
Migrating from Spring Boot to Micronaut for better GraalVM native image support.

## Phase 1: Dependencies (pom.xml)

### Remove Spring Boot dependencies:
- `spring-boot-starter-web` → `micronaut-http-server-netty`
- `spring-boot-starter-test` → `micronaut-test-junit5`
- `springdoc-openapi-starter-webmvc-ui` → `micronaut-openapi`
- `spring-security-oauth2-client` → `micronaut-security-oauth2`
- `spring-restdocs-mockmvc` → Remove (use micronaut-test)

### Add Micronaut BOM and core dependencies:
- Micronaut BOM 4.7.6 (latest stable)
- `micronaut-http-server-netty`
- `micronaut-inject-java` (annotation processor)
- `micronaut-serde-jackson` (replaces Gson, GraalVM-optimized)
- `micronaut-runtime`
- `micronaut-validation`

### Keep unchanged:
- OpenSearch client
- OpenNLP
- Jsoup
- langchain4j
- commons-math3
- Lombok

## Phase 2: Application Class

**Before:**
```java
@SpringBootApplication
@EnableScheduling
public class SimpleRagServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleRagServerApplication.class, args);
    }
}
```

**After:**
```java
@MicronautApplication
public class SimpleRagServerApplication {
    public static void main(String[] args) {
        Micronaut.run(SimpleRagServerApplication.class, args);
    }
}
```

## Phase 3: Annotation Mapping

| Spring | Micronaut | Notes |
|--------|-----------|-------|
| `@Component` | `@Singleton` | Same lifecycle |
| `@Service` | `@Singleton` | Same lifecycle |
| `@Configuration` | `@Factory` | For bean definitions |
| `@Bean` | `@Singleton` (on method) | In @Factory class |
| `@Autowired` | Constructor injection | Remove annotation, use constructor |
| `@Value("${key}")` | `@Property(name="key")` | Property injection |
| `@RestController` | `@Controller` | HTTP controllers |
| `@GetMapping` | `@Get` | HTTP GET |
| `@PostMapping` | `@Post` | HTTP POST |
| `@RequestBody` | `@Body` | Request body |
| `@PathVariable` | `@PathVariable` | Same! |
| `@RequestParam` | `@QueryValue` | Query params |
| `@Scheduled` | `@Scheduled` | Same! |
| `@EventListener` | `@EventListener` | Same! |
| `ApplicationEventPublisher` | `ApplicationEventPublisher` | Same interface! |

## Phase 4: File Changes by Category

### Core Application (1 file)
- `SimpleRagServerApplication.java` - Change to Micronaut

### Configuration Files (~8 files)
- `AsyncConfig.java` - Micronaut has built-in async
- `ContentConfig.java` - Migrate to @Factory
- `GsonConfig.java` - DELETE (use Micronaut Serde)
- `OpenApiConfiguration.java` - Micronaut OpenAPI config
- `OpenSearchConfig.java` - Keep mostly same
- `OpenNlpRuntimeHints.java` - DELETE (Micronaut handles this)
- `SecurityConfig.java` - Rewrite for Micronaut Security
- `WebMvcConfig.java` - DELETE (Micronaut handles CORS differently)

### Services (~15 files)
All services: Change `@Service` → `@Singleton`, remove `@Autowired`, use constructor injection

### Repositories (~10 files)
All services: Change `@Service` → `@Singleton`, remove `@Autowired`, use constructor injection

### Controllers (~10 files)
- Change `@RestController` → `@Controller`
- Change `@GetMapping` → `@Get`
- Change `@PostMapping` → `@Post`
- Change `@RequestBody` → `@Body`
- Change `@RequestParam` → `@QueryValue`
- Change `MultipartFile` → `CompletedFileUpload`

### Clients (~2 files)
- `OllamaEmbeddingClient.java` - Change annotations
- `OllamaLlmClient.java` - Change annotations

### Utils (~5 files)
- Change `@Component` → `@Singleton`

### DTOs (~20 files)
- Keep Jackson annotations (Micronaut Serde supports them)
- No changes needed!

## Phase 5: Configuration Files

### application.properties → application.yml
Micronaut prefers YAML. Convert all properties.

### Key configuration differences:
```yaml
# Micronaut format
micronaut:
  application:
    name: simple-rag-server
  server:
    port: 8080
  security:
    oauth2:
      # OAuth2 config
```

## Phase 6: Testing

### Integration Tests
- Replace `@SpringBootTest` with `@MicronautTest`
- Replace `MockMvc` with Micronaut HTTP Client
- Update test dependencies

## Phase 7: Build Configuration

### Maven plugins:
- Remove `spring-boot-maven-plugin`
- Add `micronaut-maven-plugin`
- Update `maven-compiler-plugin` for Micronaut annotation processors

### GraalVM Native Image:
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

## Estimated Effort

- **Phase 1 (pom.xml)**: 2 hours
- **Phase 2-3 (Application + annotations)**: 4 hours  
- **Phase 4 (File migrations)**: 12-16 hours
- **Phase 5 (Configuration)**: 2 hours
- **Phase 6 (Testing)**: 4 hours
- **Phase 7 (Build + GraalVM)**: 2 hours

**Total: 26-30 hours** (~3-4 working days)

## Rollback Plan

Keep a git branch with Spring Boot version. If migration fails, revert.

## Success Criteria

1. ✅ Application compiles without errors
2. ✅ All REST endpoints work
3. ✅ OpenSearch integration works
4. ✅ Ollama connector works
5. ✅ File uploads work
6. ✅ OAuth2 security works
7. ✅ GraalVM native image builds successfully
8. ✅ Native image starts in < 1 second
9. ✅ Native image uses < 100MB RAM

---

**PROCEED WITH CAUTION**: This is a major architectural change. Test thoroughly at each phase.
