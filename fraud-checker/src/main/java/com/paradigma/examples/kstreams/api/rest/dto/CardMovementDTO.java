package com.paradigma.examples.kstreams.api.rest.dto;

import lombok.Data;

@Data
public class CardMovementDTO {

    private String id;
    private String card;
    private int origin;
    private float amount;
    private String device;
    private String site;
    private String createdAt;
}
