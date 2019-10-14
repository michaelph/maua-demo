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

import br.maua.demo.TrafficMatrixService;
import java.util.Optional;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

@Command(scope = "onos", name = "remove-cost",
  description = "Command for set the Link delay")
public class MetaLinkRemoveCommand extends AbstractShellCommand {


    TrafficMatrixService trafficMatrixService = get(TrafficMatrixService.class);


    @Override
    protected void execute() {
        trafficMatrixService.removeMetaLink(Optional.empty());
        print("Removed!");
        trafficMatrixService.getTrafficMatrixInfo().forEach(mt -> {
            print(mt.toString());
        });

    }

}