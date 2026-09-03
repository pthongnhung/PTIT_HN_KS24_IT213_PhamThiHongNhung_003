package vn.rikkei.exam.meetingroom.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.rikkei.exam.meetingroom.model.AppUser;
public interface AppUserRepository extends JpaRepository<AppUser, String> { }
