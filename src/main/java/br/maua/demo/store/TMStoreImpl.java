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
package br.maua.demo.store;

import static org.slf4j.LoggerFactory.getLogger;

import br.maua.demo.LinkWeight.WeightType;
import br.maua.demo.MetaLink;
import br.maua.demo.store.TMEvent.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

@Component(immediate = true)
@Service
public class TMStoreImpl extends AbstractStore<TMEvent, TMStoreDelegate> implements TMStore {

    private final Logger log = getLogger(getClass());

    private ConsistentMap<String, MetaLink> metaLinkConsistentMap;
    private Map<String, MetaLink> metaLinkMap;
    private ConsistentMap<Key, Intent> monitoredIntentsConsistentMap;
    private Map<Key, Intent> monitoredIntentsMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        metaLinkConsistentMap = storageService.<String, MetaLink>consistentMapBuilder()
          .withSerializer(Serializer.using(
            new KryoNamespace.Builder()
              .register(KryoNamespaces.API)
              .register(Link.class)
              .register(MetaLink.class)
              .build()))
          .withName("metaLinks")
          .build();
        metaLinkMap = metaLinkConsistentMap.asJavaMap();

        monitoredIntentsConsistentMap = storageService.<Key, Intent>consistentMapBuilder()
          .withSerializer(Serializer.using(
            new KryoNamespace.Builder()
              .register(KryoNamespaces.API)
              .register(Key.class)
              .register(Intent.class)
              .build()))
          .withName("monitoredIntents")
          .build();
        monitoredIntentsMap = monitoredIntentsConsistentMap.asJavaMap();


    }

    @Deactivate
    public void deactivate() {

    }

    @Override
    public void addLinkWeight(Link link, double weigh, WeightType weightType) {

    }

    @Override
    public void addLinkWeight(MetaLink metaLink) {
        Optional<MetaLink> optionalMetaLink = metaLinkMap.values().stream()
          .filter(mt -> mt.getLink().src().toString().equals(metaLink.getLink().src().toString()) && mt.getLink().dst()
            .toString().equals(metaLink.getLink().dst().toString())).findFirst()
          .map(mt -> {
              metaLink.setId(mt.getId());
              return metaLinkMap.put(mt.getId(), metaLink);
          });
        if (!optionalMetaLink.isPresent()) {
            metaLinkMap.put(metaLink.getId(), metaLink);
        }
        log.info("ID: " + metaLink.getId());
        sendEvent(Type.ADDED, metaLink);
    }

    @Override
    public void addIntentMonitored(Key key) {

    }

    @Override
    public List<MetaLink> getTrafficMatrixInfo() {
        List<MetaLink> links = new ArrayList<>(metaLinkMap.size());
        metaLinkMap.values().forEach(metaLink -> {
            links.add(metaLink);
        });
        return links;
    }

    @Override
    public void removeMetaLink(Optional<Integer> id) {
        if (id.isPresent()) {
            metaLinkMap.remove(id);
        } else {
            metaLinkMap.clear();
        }
    }

    private void sendEvent(TMEvent.Type type, MetaLink metaLink) {
        notifyDelegate(new TMEvent(type, metaLink, System.currentTimeMillis()));
    }
}
