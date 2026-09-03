package vn.rikkei.exam.meetingroom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resource_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceType {

    @Id
    @Column(name = "resource_code", nullable = false, length = 20)
    private String resourceCode;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
