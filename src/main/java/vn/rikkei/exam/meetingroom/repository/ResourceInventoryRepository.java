package vn.rikkei.exam.meetingroom.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.rikkei.exam.meetingroom.model.ResourceInventory;

import java.time.LocalDate;
import java.util.List;

public interface ResourceInventoryRepository extends JpaRepository<ResourceInventory, Long> {

    @Query("""
            select i
            from ResourceInventory i
            join fetch i.resourceType r
            where r.resourceCode = :resourceCode
              and i.availableDate between :startDate and :endDate
            order by i.availableDate
            """)
    List<ResourceInventory> findAvailability(
            @Param("resourceCode") String resourceCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from ResourceInventory i
            join fetch i.resourceType r
            where r.resourceCode = :resourceCode
              and i.availableDate between :startDate and :endDate
            order by i.availableDate
            """)
    List<ResourceInventory> findAvailabilityForUpdate(
            @Param("resourceCode") String resourceCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
