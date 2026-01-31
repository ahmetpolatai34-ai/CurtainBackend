package com.curtain.backend.repository;

import com.curtain.backend.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {
    List<Station> findAllByOrderByStepAsc();
}
