package com.incident.resolver.repository;

import com.incident.resolver.entity.DailyIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyIncidentRepository extends JpaRepository<DailyIncident, Long> {
    
    // Custom: Find all incidents for a specific person (by name, via join)
    @Query("SELECT di FROM DailyIncident di WHERE di.person.name = :personName ORDER BY di.date")
    List<DailyIncident> findByPersonName(@Param("personName") String personName);
    
    // Custom: For report - Get all daily resolutions (person name, date, count) sorted
    @Query("SELECT di.person.name, di.date, di.count FROM DailyIncident di ORDER BY di.person.name, di.date")
    List<Object[]> findAllDailyResolutions();
}