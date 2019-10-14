/*
 * Copyright 2014 Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.maua.demo;

import java.util.List;
import java.util.Optional;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.Link;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkWeight implements LinkWeigher {

    List<MetaLink> links;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public LinkWeight(List<MetaLink> links) {
        this.links = links;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        Link localLink = edge.link();
        Optional<MetaLink> optionalMetaLink = links.stream().filter(myLink -> {
            boolean flag = localLink.src().toString().equals(myLink.getLink().src().toString()) && localLink.dst()
              .toString().equals(myLink.getLink().dst().toString());
            return flag;
        }).findFirst();
        if (optionalMetaLink.isPresent()) {
            log.info("Link found with delay: " + optionalMetaLink.get().getDelay());
            return ScalarWeight.toWeight(optionalMetaLink.get().getDelay());
        }

        return ScalarWeight.toWeight(1);
    }

    @Override
    public Weight getInitialWeight() {
        return ScalarWeight.toWeight(0);
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;

    }

    public enum WeightType {
        BW, DELAY;
    }
}
