package com.incident.resolver.entity;



import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;  // e.g., "Appalasuri Badithaboni"

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DailyIncident> dailyIncidents = new ArrayList<>();

    // Default Constructor
    public Person() {}

    // Parameterized Constructor
    public Person(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DailyIncident> getDailyIncidents() {
        return dailyIncidents;
    }

    public void setDailyIncidents(List<DailyIncident> dailyIncidents) {
        this.dailyIncidents = dailyIncidents;
    }
}