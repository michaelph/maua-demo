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

import br.maua.demo.MetaLink;
import org.onosproject.event.AbstractEvent;

public class TMEvent extends AbstractEvent<TMEvent.Type, MetaLink> {

    public TMEvent(TMEvent.Type type, MetaLink metaLink, long time) {
        super(type, metaLink, time);
    }

    public TMEvent(TMEvent.Type type, MetaLink metaLink) {
        super(type, metaLink);
    }

    public enum Type {
        ADDED,
        REMOVED;

        Type() {
        }
    }
}

