package vn.rikkei.exam.meetingroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.meetingroom.dto.ApproveRequestRequest;
import vn.rikkei.exam.meetingroom.dto.OperationResponse;
import vn.rikkei.exam.meetingroom.service.ReservationOperationService;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final ReservationOperationService operationService;

    @PostMapping("/approve-request")
    public ResponseEntity<OperationResponse> approveRequest(@Valid @RequestBody ApproveRequestRequest request) {
        return ResponseEntity.ok(operationService.process(
                request.requestId(), request.decision(), request.note()));
    }
}
