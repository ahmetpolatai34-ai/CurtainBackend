package com.curtain.backend.service;

import com.curtain.backend.entity.Station;
import com.curtain.backend.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {
    @Autowired
    private StationRepository stationRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAllByOrderByStepAsc();
    }

    public Station createStation(Station station) {
        return stationRepository.save(station);
    }
    
     public Station updateStation(Long id, Station stationDetails) {
        Station station = stationRepository.findById(id).orElseThrow();
        station.setName(stationDetails.getName());
        station.setStateCode(stationDetails.getStateCode());
        station.setStep(stationDetails.getStep());
        station.setStatus(stationDetails.getStatus());
        return stationRepository.save(station);
    }

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
