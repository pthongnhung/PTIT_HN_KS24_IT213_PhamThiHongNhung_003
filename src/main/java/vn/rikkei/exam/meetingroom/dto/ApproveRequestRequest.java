package vn.rikkei.exam.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveRequestRequest(
        @NotBlank(message = "requestId không được để trống") String requestId,
        @NotBlank(message = "decision phải là APPROVE hoặc REJECT") String decision,
        @Size(max = 500, message = "note tối đa 500 ký tự") String note
) { }
