package vn.rikkei.exam.meetingroom.service.rag;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagAutoIngestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagAutoIngestRunner.class);

    private final InternalPolicyRagService ragService;

    @Value("${rag.auto-ingest:true}")
    private boolean autoIngest;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoIngest) {
            return;
        }
        try {
            var result = ragService.ingestCorpus();
            log.info("event=rag_auto_ingest result={}", result);
        } catch (Exception ex) {
            log.error("event=rag_auto_ingest_failed type={} message={}",
                    ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
