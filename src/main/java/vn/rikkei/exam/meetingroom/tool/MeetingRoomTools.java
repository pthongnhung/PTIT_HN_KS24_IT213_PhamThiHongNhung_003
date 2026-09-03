package vn.rikkei.exam.meetingroom.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vn.rikkei.exam.meetingroom.dto.CreateMeetingRoomRequestResult;
import vn.rikkei.exam.meetingroom.dto.MeetingRoomAvailabilityResult;
import vn.rikkei.exam.meetingroom.exception.BadRequestException;
import vn.rikkei.exam.meetingroom.service.MeetingRoomService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class MeetingRoomTools {

    private final MeetingRoomService meetingRoomService;
    private final ToolExecutionTracker toolExecutionTracker;

    @Tool(
            name = "getMeetingRoomAvailability",
            description = """
                    Kiểm tra dữ liệu THỰC TẾ về tình trạng phòng họp còn khả dụng theo loại phòng và khoảng ngày.
                    Bắt buộc dùng tool này khi người dùng hỏi phòng còn trống/khả dụng.
                    Không được tự suy đoán tình trạng phòng từ tài liệu RAG hoặc từ kiến thức của LLM.
                    startDate và endDate dùng định dạng ISO yyyy-MM-dd và startDate phải <= endDate.
                    """
    )
    public MeetingRoomAvailabilityResult getMeetingRoomAvailability(
            @ToolParam(description = "Loại phòng: STANDARD/STD hoặc PREMIUM/PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu theo yyyy-MM-dd") String startDate,
            @ToolParam(description = "Ngày kết thúc theo yyyy-MM-dd") String endDate) {
        toolExecutionTracker.record("getMeetingRoomAvailability");
        return meetingRoomService.getMeetingRoomAvailability(
                resourceType,
                parseDate(startDate, "startDate"),
                parseDate(endDate, "endDate")
        );
    }

    @Tool(
            name = "createMeetingRoomRequest",
            description = """
                    Tạo yêu cầu đặt phòng họp bằng Java Service và lưu vào database với trạng thái PENDING.
                    Bắt buộc dùng tool này khi người dùng muốn tạo/đặt phòng.
                    Tool tự kiểm tra user tồn tại, startDate <= endDate, thời lượng tối đa 14 ngày,
                    sức chứa phòng, PREMIUM tối thiểu 2 người, purpose dài 10-200 ký tự và availability thực tế.
                    Không được tự tạo requestId hoặc tự tuyên bố đã đặt phòng nếu tool chưa thực thi thành công.
                    """
    )
    public CreateMeetingRoomRequestResult createMeetingRoomRequest(
            @ToolParam(description = "Mã user có thật trong database, ví dụ USR-001") String userId,
            @ToolParam(description = "Loại phòng: STANDARD/STD hoặc PREMIUM/PRM") String resourceType,
            @ToolParam(description = "Ngày bắt đầu theo yyyy-MM-dd") String startDate,
            @ToolParam(description = "Ngày kết thúc theo yyyy-MM-dd") String endDate,
            @ToolParam(description = "Số người tham gia") Integer participantCount,
            @ToolParam(description = "Mục đích đặt phòng, từ 10 đến 200 ký tự") String purpose) {
        toolExecutionTracker.record("createMeetingRoomRequest");
        return meetingRoomService.createMeetingRoomRequest(
                userId,
                resourceType,
                parseDate(startDate, "startDate"),
                parseDate(endDate, "endDate"),
                participantCount,
                purpose
        );
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new BadRequestException(fieldName + " phải theo định dạng yyyy-MM-dd");
        }
    }
}
