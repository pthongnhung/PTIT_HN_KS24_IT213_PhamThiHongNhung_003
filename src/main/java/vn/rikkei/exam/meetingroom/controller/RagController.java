package vn.rikkei.exam.meetingroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.meetingroom.dto.IngestResponse;
import vn.rikkei.exam.meetingroom.service.rag.InternalPolicyRagService;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final InternalPolicyRagService ragService;

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest() {
        return ResponseEntity.ok(ragService.ingestCorpus());
    }
}
