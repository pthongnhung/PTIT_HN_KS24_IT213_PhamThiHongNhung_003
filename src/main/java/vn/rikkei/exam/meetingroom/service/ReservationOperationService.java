package vn.rikkei.exam.meetingroom.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rikkei.exam.meetingroom.dto.OperationResponse;
import vn.rikkei.exam.meetingroom.exception.BadRequestException;
import vn.rikkei.exam.meetingroom.exception.ConflictException;
import vn.rikkei.exam.meetingroom.exception.NotFoundException;
import vn.rikkei.exam.meetingroom.model.ReservationRequest;
import vn.rikkei.exam.meetingroom.model.ReservationStatus;
import vn.rikkei.exam.meetingroom.model.ResourceInventory;
import vn.rikkei.exam.meetingroom.repository.ReservationRequestRepository;
import vn.rikkei.exam.meetingroom.repository.ResourceInventoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReservationOperationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationOperationService.class);

    private final ReservationRequestRepository reservationRequestRepository;
    private final ResourceInventoryRepository resourceInventoryRepository;
    private final MeetingRoomService meetingRoomService;

    @Transactional
    public OperationResponse process(String requestId, String decision, String note) {
        ReservationRequest request = reservationRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy request: " + requestId));

        if (request.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Chỉ request PENDING mới được APPROVE/REJECT. Trạng thái hiện tại: "
                    + request.getStatus());
        }

        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!normalizedDecision.equals("APPROVE") && !normalizedDecision.equals("REJECT")) {
            throw new BadRequestException("decision chỉ nhận APPROVE hoặc REJECT");
        }

        if (normalizedDecision.equals("APPROVE")) {
            approve(request);
        } else {
            request.setStatus(ReservationStatus.REJECTED);
        }

        request.setDecisionNote(note == null ? null : note.trim());
        request.setUpdatedAt(Instant.now());
        reservationRequestRepository.save(request);

        log.info("event=reservation_decision requestId={} decision={} status={}",
                requestId, normalizedDecision, request.getStatus());

        return new OperationResponse(
                request.getRequestId(),
                request.getStatus().name(),
                "Đã xử lý request " + request.getRequestId() + " với quyết định " + normalizedDecision
        );
    }

    private void approve(ReservationRequest request) {
        meetingRoomService.validateReservationBusinessRules(request);

        List<ResourceInventory> inventories = resourceInventoryRepository.findAvailabilityForUpdate(
                request.getResourceType().getResourceCode(),
                request.getStartDate(),
                request.getEndDate()
        );

        var availability = meetingRoomService.buildAvailabilityResult(
                request.getResourceType(),
                request.getStartDate(),
                request.getEndDate(),
                inventories
        );

        if (!availability.availableForWholeRange()) {
            throw new ConflictException("Không thể APPROVE vì phòng không còn khả dụng trong toàn bộ khoảng ngày");
        }

        inventories.forEach(item -> item.setAvailableSlots(item.getAvailableSlots() - 1));
        resourceInventoryRepository.saveAll(inventories);
        request.setStatus(ReservationStatus.APPROVED);
    }
}
