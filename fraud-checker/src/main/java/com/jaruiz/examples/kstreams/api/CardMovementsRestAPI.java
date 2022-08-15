package com.jaruiz.examples.kstreams.api;

import com.jaruiz.examples.kstreams.api.rest.dto.CardMovementDTO;
import com.jaruiz.examples.kstreams.fraudchecker.FraudCheckerTopology;
import com.jaruiz.examples.kstreams.model.CardMovement;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/movements")
@Produces(MediaType.APPLICATION_JSON)
public class CardMovementsRestAPI {
    private static final Logger LOG = Logger.getLogger(CardMovementsRestAPI.class);

    @Inject
    @Channel("card-movements")
    Emitter<CardMovement> cardMovementEmitter;


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertMovement(CardMovementDTO cardMovementDTO) {
        CardMovement cardMovement = CardMovement.newBuilder()
                .setId(cardMovementDTO.getId())
                .setCard(cardMovementDTO.getCard())
                .setOrigin(cardMovementDTO.getOrigin())
                .setSite(cardMovementDTO.getSite())
                .setDevice(cardMovementDTO.getDevice())
                .setAmount(cardMovementDTO.getAmount())
                .setCreatedAt(cardMovementDTO.getCreatedAt())
                .build();

        cardMovementEmitter.send(cardMovement).whenComplete((success, failure) -> {
            if (failure != null) {
                LOG.error("Error: " + cardMovement.getId() + " // MSG: " + failure.getMessage());
            } else {
                LOG.info("Message processed successfully: " + cardMovement.getId());
            }
        });

        return Response.ok().status(201).build();
    }
}