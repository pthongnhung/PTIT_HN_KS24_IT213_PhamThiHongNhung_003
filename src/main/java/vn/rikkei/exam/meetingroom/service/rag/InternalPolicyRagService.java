package vn.rikkei.exam.meetingroom.service.rag;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import vn.rikkei.exam.meetingroom.dto.IngestResponse;
import vn.rikkei.exam.meetingroom.dto.SourceReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InternalPolicyRagService {

    private static final Logger log = LoggerFactory.getLogger(InternalPolicyRagService.class);
    private static final String SOURCE = "tai_lieu_noi_bo.md";
    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[A-Za-z0-9_]+");

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}")
    private String vectorTableName;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.similarity-threshold:0.65}")
    private double similarityThreshold;

    public IngestResponse ingestCorpus() {
        validateTableName();
        String markdown = readCorpus();
        List<PolicyChunk> chunks = splitByMeaningfulPolicySection(markdown);

        List<Document> toWrite = new ArrayList<>();
        int skipped = 0;

        for (PolicyChunk chunk : chunks) {
            String deterministicId = deterministicUuid(SOURCE + "::" + chunk.section());
            String contentHash = sha256(chunk.content());
            String existingHash = findExistingContentHash(deterministicId);

            if (contentHash.equals(existingHash)) {
                skipped++;
                continue;
            }

            if (existingHash != null) {
                deleteExistingDocument(deterministicId);
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", SOURCE);
            metadata.put("section", chunk.section());
            metadata.put("citation", SOURCE + "#" + slug(chunk.section()));
            metadata.put("content_hash", contentHash);
            metadata.put("chunk_strategy", "markdown_policy_section");

            toWrite.add(Document.builder()
                    .id(deterministicId)
                    .text(chunk.content())
                    .metadata(metadata)
                    .build());
        }

        if (!toWrite.isEmpty()) {
            vectorStore.add(toWrite);
        }

        log.info("event=rag_ingest source={} totalChunks={} writtenChunks={} skippedChunks={}",
                SOURCE, chunks.size(), toWrite.size(), skipped);

        return new IngestResponse(
                SOURCE,
                chunks.size(),
                toWrite.size(),
                skipped,
                "Chunk theo từng mục chính sách có ý nghĩa; ID ổn định theo source+section và content hash dùng để chống nạp trùng."
        );
    }

    public RagSearchResult retrieve(String question) {
        if (question == null || question.isBlank()) {
            return new RagSearchResult("", List.of());
        }

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            return new RagSearchResult("", List.of());
        }

        StringBuilder context = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        for (Document document : documents) {
            String source = Objects.toString(document.getMetadata().get("source"), SOURCE);
            String section = Objects.toString(document.getMetadata().get("section"), "Không rõ mục");
            String citation = Objects.toString(document.getMetadata().get("citation"), source);

            context.append("[Nguồn: ")
                    .append(citation)
                    .append("]\n")
                    .append(document.getText())
                    .append("\n\n");

            sources.add(new SourceReference(source, section, citation, document.getScore()));
        }

        log.info("event=rag_retrieve topK={} threshold={} returned={}",
                topK, similarityThreshold, sources.size());
        return new RagSearchResult(context.toString().trim(), List.copyOf(sources));
    }

    private String readCorpus() {
        try {
            ClassPathResource resource = new ClassPathResource(SOURCE);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Không đọc được corpus " + SOURCE, ex);
        }
    }

    private List<PolicyChunk> splitByMeaningfulPolicySection(String markdown) {
        List<PolicyChunk> chunks = new ArrayList<>();
        String currentSection = null;
        StringBuilder body = new StringBuilder();

        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("## ")) {
                if (currentSection != null && !body.toString().isBlank()) {
                    chunks.add(new PolicyChunk(currentSection,
                            "## " + currentSection + "\n" + body.toString().trim()));
                }
                currentSection = line.substring(3).trim();
                body.setLength(0);
            } else if (currentSection != null) {
                body.append(rawLine).append('\n');
            }
        }

        if (currentSection != null && !body.toString().isBlank()) {
            chunks.add(new PolicyChunk(currentSection,
                    "## " + currentSection + "\n" + body.toString().trim()));
        }
        return chunks;
    }

    private String findExistingContentHash(String documentId) {
        String sql = "SELECT metadata->>'content_hash' FROM " + vectorTableName
                + " WHERE id = CAST(? AS uuid) LIMIT 1";
        List<String> hashes = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getString(1), documentId);
        return hashes.isEmpty() ? null : hashes.get(0);
    }

    private void deleteExistingDocument(String documentId) {
        String sql = "DELETE FROM " + vectorTableName + " WHERE id = CAST(? AS uuid)";
        jdbcTemplate.update(sql, documentId);
    }

    private void validateTableName() {
        if (vectorTableName == null || !SAFE_TABLE_NAME.matcher(vectorTableName).matches()) {
            throw new IllegalStateException("PGVECTOR_TABLE_NAME không hợp lệ");
        }
    }

    private String deterministicUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private String slug(String value) {
        return value.toLowerCase()
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private record PolicyChunk(String section, String content) { }
}
