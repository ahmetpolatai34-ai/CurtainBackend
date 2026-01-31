package com.curtain.backend.controller;

import com.curtain.backend.entity.Station;
import com.curtain.backend.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping
    public List<Station> getAllStations() {
        return stationService.getAllStations();
    }

    @PostMapping
    public Station createStation(@RequestBody Station station) {
        return stationService.createStation(station);
    }
    
     @PutMapping("/{id}")
    public Station updateStation(@PathVariable Long id, @RequestBody Station station) {
        return stationService.updateStation(id, station);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok().build();
    }
}
