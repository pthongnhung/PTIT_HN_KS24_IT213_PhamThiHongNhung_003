package vn.rikkei.exam.meetingroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rikkei.exam.meetingroom.dto.CreateMeetingRoomRequestResult;
import vn.rikkei.exam.meetingroom.dto.DailyAvailability;
import vn.rikkei.exam.meetingroom.dto.MeetingRoomAvailabilityResult;
import vn.rikkei.exam.meetingroom.exception.BadRequestException;
import vn.rikkei.exam.meetingroom.exception.ConflictException;
import vn.rikkei.exam.meetingroom.exception.NotFoundException;
import vn.rikkei.exam.meetingroom.model.AppUser;
import vn.rikkei.exam.meetingroom.model.ReservationRequest;
import vn.rikkei.exam.meetingroom.model.ReservationStatus;
import vn.rikkei.exam.meetingroom.model.ResourceInventory;
import vn.rikkei.exam.meetingroom.model.ResourceType;
import vn.rikkei.exam.meetingroom.repository.AppUserRepository;
import vn.rikkei.exam.meetingroom.repository.ReservationRequestRepository;
import vn.rikkei.exam.meetingroom.repository.ResourceInventoryRepository;
import vn.rikkei.exam.meetingroom.repository.ResourceTypeRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final AppUserRepository appUserRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceInventoryRepository resourceInventoryRepository;
    private final ReservationRequestRepository reservationRequestRepository;

    @Transactional(readOnly = true)
    public MeetingRoomAvailabilityResult getMeetingRoomAvailability(String resourceType,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate) {
        validateDateRange(startDate, endDate);
        ResourceType type = resolveResourceType(resourceType);
        List<ResourceInventory> inventories = resourceInventoryRepository.findAvailability(
                type.getResourceCode(), startDate, endDate);
        return buildAvailabilityResult(type, startDate, endDate, inventories);
    }

    @Transactional
    public CreateMeetingRoomRequestResult createMeetingRoomRequest(String userId,
                                                                    String resourceType,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Integer participantCount,
                                                                    String purpose) {
        validateDateRange(startDate, endDate);
        validateMaximumDuration(startDate, endDate);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user: " + userId));
        ResourceType type = resolveResourceType(resourceType);

        validateParticipantRules(type, participantCount);
        String normalizedPurpose = validatePurpose(purpose);

        List<ResourceInventory> inventories = resourceInventoryRepository.findAvailability(
                type.getResourceCode(), startDate, endDate);
        MeetingRoomAvailabilityResult availability = buildAvailabilityResult(type, startDate, endDate, inventories);
        if (!availability.availableForWholeRange()) {
            throw new ConflictException("Phòng " + type.getResourceCode()
                    + " không còn khả dụng trong toàn bộ khoảng ngày yêu cầu");
        }

        Instant now = Instant.now();
        ReservationRequest request = ReservationRequest.builder()
                .requestId(generateRequestId())
                .requester(user)
                .resourceType(type)
                .startDate(startDate)
                .endDate(endDate)
                .participantCount(participantCount)
                .purpose(normalizedPurpose)
                .status(ReservationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        reservationRequestRepository.save(request);

        String summary = "Đã tạo yêu cầu " + request.getRequestId()
                + " ở trạng thái PENDING cho phòng " + type.getResourceCode()
                + ", từ " + startDate + " đến " + endDate
                + ", " + participantCount + " người, mục đích: " + normalizedPurpose;

        return new CreateMeetingRoomRequestResult(
                request.getRequestId(),
                request.getStatus().name(),
                summary
        );
    }

    public ResourceType resolveResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new BadRequestException("resourceType không được để trống");
        }

        String normalized = resourceType.trim().toUpperCase(Locale.ROOT);
        String code = switch (normalized) {
            case "STANDARD", "STD", "STANDARD PHÒNG HỌP", "STANDARD PHONG HOP" -> "STD";
            case "PREMIUM", "PRM", "PREMIUM PHÒNG HỌP", "PREMIUM PHONG HOP" -> "PRM";
            default -> normalized;
        };

        ResourceType type = resourceTypeRepository.findById(code)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại phòng: " + resourceType));

        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new ConflictException("Loại phòng " + code + " hiện không hoạt động");
        }
        return type;
    }

    public void validateReservationBusinessRules(ReservationRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateMaximumDuration(request.getStartDate(), request.getEndDate());
        validateParticipantRules(request.getResourceType(), request.getParticipantCount());
        validatePurpose(request.getPurpose());
        if (!appUserRepository.existsById(request.getRequester().getUserId())) {
            throw new NotFoundException("User của yêu cầu không còn tồn tại: " + request.getRequester().getUserId());
        }
    }

    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate và endDate không được để trống");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate phải nhỏ hơn hoặc bằng endDate");
        }
    }

    public void validateMaximumDuration(LocalDate startDate, LocalDate endDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (inclusiveDays > 14) {
            throw new BadRequestException("Một yêu cầu đặt phòng tối đa 14 ngày");
        }
    }

    public void validateParticipantRules(ResourceType type, Integer participantCount) {
        if (participantCount == null || participantCount <= 0) {
            throw new BadRequestException("participantCount phải lớn hơn 0");
        }
        if (participantCount > type.getMaxParticipants()) {
            throw new BadRequestException("Số người vượt quá sức chứa tối đa của phòng "
                    + type.getResourceCode() + ": " + type.getMaxParticipants());
        }
        if ("PRM".equalsIgnoreCase(type.getResourceCode()) && participantCount < 2) {
            throw new BadRequestException("Nhóm PREMIUM yêu cầu tối thiểu 2 người");
        }
    }

    public String validatePurpose(String purpose) {
        if (purpose == null) {
            throw new BadRequestException("purpose không được để trống");
        }
        String normalized = purpose.trim();
        if (normalized.length() < 10 || normalized.length() > 200) {
            throw new BadRequestException("purpose phải có độ dài từ 10 đến 200 ký tự");
        }
        return normalized;
    }

    public MeetingRoomAvailabilityResult buildAvailabilityResult(ResourceType type,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  List<ResourceInventory> inventories) {
        long expectedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<DailyAvailability> daily = new ArrayList<>();
        int minimumSlots = Integer.MAX_VALUE;
        boolean allAvailable = inventories.size() == expectedDays;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate date = cursor;
            ResourceInventory inventory = inventories.stream()
                    .filter(item -> date.equals(item.getAvailableDate()))
                    .findFirst()
                    .orElse(null);
            int slots = inventory == null || inventory.getAvailableSlots() == null
                    ? 0 : inventory.getAvailableSlots();
            boolean available = slots > 0;
            daily.add(new DailyAvailability(date, slots, available));
            minimumSlots = Math.min(minimumSlots, slots);
            if (!available) {
                allAvailable = false;
            }
            cursor = cursor.plusDays(1);
        }

        if (minimumSlots == Integer.MAX_VALUE) {
            minimumSlots = 0;
        }

        String summary = allAvailable
                ? "Phòng " + type.getResourceCode() + " còn khả dụng trong toàn bộ khoảng ngày; số slot tối thiểu còn lại là " + minimumSlots
                : "Phòng " + type.getResourceCode() + " không khả dụng trong toàn bộ khoảng ngày yêu cầu";

        return new MeetingRoomAvailabilityResult(
                type.getResourceCode(),
                startDate,
                endDate,
                allAvailable,
                minimumSlots,
                List.copyOf(daily),
                summary
        );
    }

    private String generateRequestId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
