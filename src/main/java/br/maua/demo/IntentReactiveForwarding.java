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

import static org.slf4j.LoggerFactory.getLogger;

import br.maua.demo.store.TMEvent;
import br.maua.demo.store.TMListener;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */
@Component(immediate = true)
public class IntentReactiveForwarding {

    private final Logger log = getLogger(getClass());
    private TMListener tmListener = new InternalTMListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TrafficMatrixService trafficMatrixService;


    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private ApplicationId appId;

    private static final int DROP_RULE_TIMEOUT = 300;

    private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
      IntentState.WITHDRAWING,
      IntentState.WITHDRAW_REQ);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("br.maua.demo");

        packetService.addProcessor(processor, PacketProcessor.director(2));

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
        trafficMatrixService.addListener(tmListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        trafficMatrixService.removeListener(tmListener);
        log.info("Stopped");
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            if (dst == null) {
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            setUpConnectivity(context, srcId, dstId);
            forwardPacketToDst(context, dst);
        }
    }

    // Floods the specified packet if permissible.
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
          context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    private void forwardPacketToDst(PacketContext context, Host dst) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
          treatment, context.inPacket().unparsed());
        packetService.emit(packet);
        log.info("sending packet: {}", packet);
    }

    // Install a rule forwarding the packet to the specified port.
    private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId) {
        TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        Key key;
        if (srcId.toString().compareTo(dstId.toString()) < 0) {
            key = Key.of(srcId.toString() + dstId.toString(), appId);
        } else {
            key = Key.of(dstId.toString() + srcId.toString(), appId);
        }
        Host srcHost = hostService.getHost(srcId);
        Host dstHost = hostService.getHost(dstId);

        LinkCollectionIntent intent = (LinkCollectionIntent) intentService.getIntent(key);
        // TODO handle the FAILED state
        if (intent != null) {
            if (WITHDRAWN_STATES.contains(intentService.getIntentState(key))) {
                LinkCollectionIntent linkCollectionIntent = LinkCollectionIntent.builder()
                  .appId(appId)
                  .key(key)
                  .selector(selector)
                  .treatment(treatment)
                  .filteredIngressPoints(intent.filteredIngressPoints())
                  .filteredIngressPoints(intent.filteredEgressPoints())
                  .links(intent.links())
                  .applyTreatmentOnEgress(false)
                  .build();

                intentService.submit(linkCollectionIntent);
                log.info("intent not null");
            }

        } else if (intent == null) {
            ConnectPoint connectPointSrc = new ConnectPoint(srcHost.location().elementId(), srcHost.location().port());
            ConnectPoint connectPointDst = new ConnectPoint(dstHost.location().elementId(), dstHost.location().port());
            FilteredConnectPoint filteredConnectPointSrc = new FilteredConnectPoint(connectPointSrc);
            FilteredConnectPoint filteredConnectPointDst = new FilteredConnectPoint(connectPointDst);
            Set<FilteredConnectPoint> inSet = new HashSet<>();
            Set<FilteredConnectPoint> outSet = new HashSet<>();
            inSet.add(filteredConnectPointSrc);
            outSet.add(filteredConnectPointDst);
            Set<Path> paths = topologyService
              .getPaths(topologyService.currentTopology(), connectPointSrc.deviceId(), connectPointDst.deviceId(),
                new LinkWeight(trafficMatrixService.getTrafficMatrixInfo()));

            Set<Link> links = new HashSet<>(paths.stream().max(Comparator.comparing(Path::weight)).get().links());

            LinkCollectionIntent linkCollectionIntent = LinkCollectionIntent.builder()
              .appId(appId)
              .key(key)
              .selector(selector)
              .treatment(treatment)
              .filteredIngressPoints(inSet)
              .filteredEgressPoints(outSet)
              .links(links)
              .applyTreatmentOnEgress(false)
              .build();

            intentService.submit(linkCollectionIntent);
            log.info("intent null");
        }

    }

    class InternalTMListener implements TMListener {

        @Override
        public void event(TMEvent event) {
            MetaLink metaLink = event.subject();
            log.info(metaLink.toString() + " was " + event.type().name());
            intentService.getIntentData().forEach(intentData -> {
                if (intentData.intent() instanceof LinkCollectionIntent && intentData.state()
                  .equals(IntentState.INSTALLED)) {
                    Optional<Link> optionalLink = ((LinkCollectionIntent) intentData.intent()).links().stream()
                      .filter(intentLink -> {
                          boolean flag =
                            intentLink.src().toString().equals(metaLink.getLink().src().toString()) && intentLink.dst()
                              .toString().equals(metaLink.getLink().dst().toString());
                          return flag;
                      }).findAny();
                    if (optionalLink.isPresent()) {
                        LinkCollectionIntent relevantIntent = ((LinkCollectionIntent) intentData.intent());

                        ConnectPoint connectPointSrc = ((FilteredConnectPoint) relevantIntent.filteredIngressPoints()
                          .toArray()[0]).connectPoint();
                        ConnectPoint connectPointDst = ((FilteredConnectPoint) relevantIntent.filteredEgressPoints()
                          .toArray()[0]).connectPoint();

                        Set<Path> paths = topologyService
                          .getPaths(topologyService.currentTopology(), connectPointSrc.deviceId(),
                            connectPointDst.deviceId(),
                            new LinkWeight(trafficMatrixService.getTrafficMatrixInfo()));

                        Set<Link> links = new HashSet<>(
                          paths.stream().max(Comparator.comparing(Path::weight)).get().links());

                        LinkCollectionIntent linkCollectionIntent = LinkCollectionIntent.builder()
                          .appId(appId)
                          .key(relevantIntent.key())
                          .selector(relevantIntent.selector())
                          .treatment(relevantIntent.treatment())
                          .filteredIngressPoints(relevantIntent.filteredIngressPoints())
                          .filteredEgressPoints(relevantIntent.filteredEgressPoints())
                          .links(links)
                          .applyTreatmentOnEgress(true)
                          .build();

                        intentService.submit(linkCollectionIntent);
                    }
                }
            });
        }
    }

}


