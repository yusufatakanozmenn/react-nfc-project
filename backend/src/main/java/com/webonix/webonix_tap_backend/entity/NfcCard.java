package com.webonix.webonix_tap_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "nfc_cards")
public class NfcCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(name = "destination_url", nullable = false)
    private String destinationUrl;

    private Integer scans;

    private Boolean active;

    public NfcCard() {
    }

    public NfcCard(
            String name,
            String type,
            String code,
            String destinationUrl,
            Integer scans,
            Boolean active
    ) {
        this.name = name;
        this.type = type;
        this.code = code;
        this.destinationUrl = destinationUrl;
        this.scans = scans;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    public Integer getScans() {
        return scans;
    }

    public void setScans(Integer scans) {
        this.scans = scans;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}