/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.routing.weighting;

import com.graphhopper.config.TurnCostsConfig;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.EdgeIterator;

import static com.graphhopper.config.TurnCostsConfig.INFINITE_U_TURN_COSTS;

public class DefaultTurnCostProvider implements TurnCostProvider {
    private final BooleanEncodedValue turnRestrictionEnc;
    private final TurnCostStorage turnCostStorage;
    private final int uTurnCostsInt;
    private final double uTurnCosts;

    public DefaultTurnCostProvider(BooleanEncodedValue turnRestrictionEnc, TurnCostStorage turnCostStorage) {
        this(turnRestrictionEnc, turnCostStorage, TurnCostsConfig.INFINITE_U_TURN_COSTS);
    }

    /**
     * @param uTurnCosts the costs of a u-turn in seconds, for {@link TurnCostsConfig#INFINITE_U_TURN_COSTS} the u-turn costs
     *                   will be infinite
     */
    public DefaultTurnCostProvider(BooleanEncodedValue turnRestrictionEnc, TurnCostStorage turnCostStorage, int uTurnCosts) {
        if (uTurnCosts < 0 && uTurnCosts != INFINITE_U_TURN_COSTS) {
            throw new IllegalArgumentException("u-turn costs must be positive, or equal to " + INFINITE_U_TURN_COSTS + " (=infinite costs)");
        }
        this.uTurnCostsInt = uTurnCosts;
        this.uTurnCosts = uTurnCosts < 0 ? Double.POSITIVE_INFINITY : uTurnCosts;
        if (turnCostStorage == null) {
            throw new IllegalArgumentException("No storage set to calculate turn weight");
        }
        // if null the TurnCostProvider can be still useful for edge-based routing
        this.turnRestrictionEnc = turnRestrictionEnc;
        this.turnCostStorage = turnCostStorage;
    }

    public BooleanEncodedValue getTurnRestrictionEnc() {
        return turnRestrictionEnc;
    }

    @Override
    public double calcTurnWeight(int edgeFrom, int nodeVia, int edgeTo) {
        if (!EdgeIterator.Edge.isValid(edgeFrom) || !EdgeIterator.Edge.isValid(edgeTo)) {
            return 0;
        }
        double tCost = 0;
        if (edgeFrom == edgeTo) {
            // note that the u-turn costs overwrite any turn costs set in TurnCostStorage
            tCost = uTurnCosts;
        } else {
            if (turnRestrictionEnc != null)
                tCost = turnCostStorage.get(turnRestrictionEnc, edgeFrom, nodeVia, edgeTo) ? Double.POSITIVE_INFINITY : 0;
        }
        return tCost;
    }

    @Override
    public long calcTurnMillis(int inEdge, int viaNode, int outEdge) {
        // Making a proper assumption about the turn time is very hard. Assuming zero is the
        // simplest way to deal with this. This also means the u-turn time is zero. Provided that
        // the u-turn weight is large enough, u-turns only occur in special situations like curbsides
        // pointing to the end of dead-end streets where it is unclear if a finite u-turn time would
        // be a good choice.
        return 0;
    }

    @Override
    public String toString() {
        return "default_tcp_" + uTurnCostsInt;
    }
}
