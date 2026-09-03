package vn.rikkei.exam.meetingroom.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.meetingroom.model.ResourceType;
public interface ResourceTypeRepository extends JpaRepository<ResourceType, String> { }
