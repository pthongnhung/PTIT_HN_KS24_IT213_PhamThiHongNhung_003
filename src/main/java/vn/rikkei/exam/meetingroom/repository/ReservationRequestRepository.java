package vn.rikkei.exam.meetingroom.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.rikkei.exam.meetingroom.model.ReservationRequest;

import java.util.Optional;

public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from ReservationRequest r
            join fetch r.requester
            join fetch r.resourceType
            where r.requestId = :requestId
            """)
    Optional<ReservationRequest> findByIdForUpdate(@Param("requestId") String requestId);
}
