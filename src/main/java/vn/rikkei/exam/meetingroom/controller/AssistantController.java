package vn.rikkei.exam.meetingroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.meetingroom.dto.AssistantAskRequest;
import vn.rikkei.exam.meetingroom.dto.AssistantAskResponse;
import vn.rikkei.exam.meetingroom.service.chat.MeetingRoomAssistantService;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final MeetingRoomAssistantService assistantService;

    @PostMapping("/ask")
    public ResponseEntity<AssistantAskResponse> ask(@Valid @RequestBody AssistantAskRequest request) {
        return ResponseEntity.ok(assistantService.ask(request));
    }
}
