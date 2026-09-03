package vn.rikkei.exam.meetingroom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "resource_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resource_inventory_resource_date",
                columnNames = {"resource_code", "available_date"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_code", nullable = false)
    private ResourceType resourceType;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;
}
