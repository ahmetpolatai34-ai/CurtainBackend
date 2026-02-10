package com.curtain.backend.repository;

import com.curtain.backend.entity.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findByOrderIdOrderByTimestampAsc(Long orderId);
}
