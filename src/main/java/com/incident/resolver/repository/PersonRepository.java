package com.incident.resolver.repository;

import com.incident.resolver.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Person findByName(String name);  // Custom: Find person by name (for caching)
}