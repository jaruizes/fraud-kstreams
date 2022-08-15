package com.jaruiz.examples.kstreams.fraudchecker.model;

import com.jaruiz.examples.kstreams.fraudchecker.config.FraudCheckerConfig;
import com.jaruiz.examples.kstreams.model.CardMovement;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PotentialOnlineFraudCase extends PotentialFraudCase {

    private Set<String> sites;

    public PotentialOnlineFraudCase() {
        super();
        this.sites = new HashSet<>();
        setType(FraudCheckerConfig.MULTIPLE_ONLINE_MOVEMENTS_IN_SHORT_PERIOD);
    }

    public void addMovement(CardMovement cardMovement) {
        super.addMovement(cardMovement);
        this.sites.add(cardMovement.getSite());
    }

    @Override
    public boolean isFraud() {
        if (getMovements().size() > FraudCheckerConfig.ALLOWED_ONLINE_MOVS_IN_SHORT_PERIOD
                && getTotalAmount() > FraudCheckerConfig.ALLOWED_ONLINE_AMOUNT_IN_SHORT_PERIOD) {
            return true;
        }

        return false;
    }
}
