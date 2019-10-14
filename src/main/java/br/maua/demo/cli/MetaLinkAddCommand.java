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
package br.maua.demo.cli;

import br.maua.demo.MetaLink;
import br.maua.demo.TrafficMatrixService;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;

@Command(scope = "onos", name = "set-link-cost",
  description = "Command for set the Link cost")
public class MetaLinkAddCommand extends AbstractShellCommand {

    LinkService linkService = get(LinkService.class);
    TrafficMatrixService trafficMatrixService = get(TrafficMatrixService.class);
    @Argument(index = 2, name = "cost", description = "Command and arguments",
      required = true, multiValued = false)
    private String cost;
    @Argument(index = 0, name = "link src", description = "Command and arguments",
      required = true, multiValued = false)
    private String src;
    @Argument(index = 1, name = "link dst", description = "Command and arguments",
      required = true, multiValued = false)
    private String dst;

    @Override
    protected void execute() {
        Link localLink = null;
        for (Link l : linkService.getLinks()) {
            if (l.src().toString().equals(src) && l.dst().toString().equals(dst)) {
                localLink = l;
            }
        }
        if (localLink == null) {
            print("Error: no link found");
            return;
        }
        MetaLink metaLink = new MetaLink();
        metaLink.setLink(localLink);
        metaLink.setDelay(Double.valueOf(cost));
        trafficMatrixService.addLinkWeight(metaLink);
        print("Added!");
        trafficMatrixService.getTrafficMatrixInfo().forEach(mt -> {
            print(mt.toString());
        });

    }

}