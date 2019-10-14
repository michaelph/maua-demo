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


import br.maua.demo.LinkWeight.WeightType;
import br.maua.demo.store.TMEvent;
import br.maua.demo.store.TMListener;
import br.maua.demo.store.TMStore;
import br.maua.demo.store.TMStoreDelegate;
import java.util.List;
import java.util.Optional;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manager traffic matrix.
 */
@Component(immediate = true)
@Service
public class TrafficMatrixManager implements TrafficMatrixService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TMStoreDelegate tmStoreDelegate = new InternalStoreDelegate();
    private final ListenerRegistry<TMEvent, TMListener> listenerRegistry =
      new ListenerRegistry<>();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TMStore store;


    @Activate
    public void activate() {
        eventDispatcher.addSink(TMEvent.class, listenerRegistry);
        store.setDelegate(tmStoreDelegate);
        log.info("TrafficMatrixManager started");
    }

    @Deactivate
    protected void deactivate() {
        eventDispatcher.removeSink(TMEvent.class);
        store.unsetDelegate(tmStoreDelegate);
        log.info("TrafficMatrixManager stopped");
    }

    @Override
    public void addLinkWeight(Link link, double weigh, WeightType weightType) {
    }

    @Override
    public void addLinkWeight(MetaLink metaLink) {
        store.addLinkWeight(metaLink);
    }

    @Override
    public void addIntentMonitored(Key key) {

    }

    @Override
    public List<MetaLink> getTrafficMatrixInfo() {
        return store.getTrafficMatrixInfo();

    }

    @Override
    public void addListener(TMListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(TMListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public void removeMetaLink(Optional<Integer> id) {
        store.removeMetaLink(id);
    }

    private class InternalStoreDelegate implements TMStoreDelegate {

        @Override
        public void notify(TMEvent tmEvent) {
            if (tmEvent != null && eventDispatcher != null) {
                eventDispatcher.post(tmEvent);
            }
        }
    }


}
