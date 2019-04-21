/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
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
package by.fdf.webrtc.javawebrtc;

import gov.nist.javax.sdp.fields.AttributeField;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.*;
import org.ice4j.ice.sdp.CandidateAttribute;
import org.ice4j.ice.sdp.IceSdpUtils;
import org.jitsi.service.neomedia.DtlsControl;
import org.opentelecoms.javax.sdp.NistSdpFactory;
import org.springframework.util.ResourceUtils;

import javax.sdp.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Utilities for manipulating SDP. Some of the utilities in this method <b>do
 * not</b> try to act smart and make a lot of assumptions (e.g. at least one
 * media stream with at least one component) that may not always be true in real
 * life and lead to exceptions. Therefore, make sure you reread the code if
 * reusing it in an application. It should be fine for the purposes of our ice4j
 * examples though.
 *
 * @author Emil Ivov
 */
public class SdpUtils {
    /**
     * Creates a session description containing the streams from the specified
     * <tt>agent</tt> using dummy codecs. This method is unlikely to be of use
     * to integrating applications as they would likely just want to feed a
     * {@link MediaDescription} and have it populated with all the necessary
     * attributes.
     *
     * @param agent the {@link Agent} we'd like to generate.
     * @return a {@link SessionDescription} representing <tt>agent</tt>'s
     * streams.
     * @throws Throwable on rainy days
     */
    public static String createSDPDescription(Agent agent) throws Throwable {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription();

        IceSdpUtils.initSessionDescription(sdess, agent);

        return sdess.toString();
    }

    /**
     * Configures <tt>localAgent</tt> the the remote peer streams, components,
     * and candidates specified in <tt>sdp</tt>
     *
     * @param localAgent  the {@link Agent} that we'd like to configure.
     * @param dtlsControl
     * @param sdp         the SDP string that the remote peer sent.
     * @throws Exception for all sorts of reasons.
     */
    @SuppressWarnings("unchecked") // jain-sdp legacy code.
    public static void parseSDP(Agent localAgent, DtlsControl dtlsControl, String sdp)
            throws Exception {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription(sdp);
//
//        for (IceMediaStream stream : localAgent.getStreams()) {
//            stream.setRemotePassword(sdess.getAttribute("ice-pwd"));
//            stream.setRemoteUfrag(sdess.getAttribute("ice-ufrag"));
//        }

        Connection globalConn = sdess.getConnection();
        String globalConnAddr = null;
        if (globalConn != null)
            globalConnAddr = globalConn.getAddress();

        Vector<MediaDescription> mdescs = sdess.getMediaDescriptions(true);

        for (MediaDescription desc : mdescs) {
            String streamName = desc.getMedia().getMediaType();

            IceMediaStream stream = localAgent.getStream(streamName);

            if (stream == null)
                continue;

            stream.setRemotePassword(desc.getAttribute("ice-pwd"));
            stream.setRemoteUfrag(desc.getAttribute("ice-ufrag"));

            Vector<Attribute> attributes = desc.getAttributes(true);
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals(CandidateAttribute.NAME))
                    parseCandidate(attribute, stream);
            }

            //set default candidates
            Connection streamConn = desc.getConnection();
            String streamConnAddr = null;
            if (streamConn != null)
                streamConnAddr = streamConn.getAddress();
            else
                streamConnAddr = globalConnAddr;

            int port = desc.getMedia().getMediaPort();

            TransportAddress defaultRtpAddress =
                    new TransportAddress(streamConnAddr, port, Transport.UDP);

//            int rtcpPort = port + 1;
//            String rtcpAttributeValue = desc.getAttribute("rtcp");

//            if (rtcpAttributeValue != null)
//                rtcpPort = Integer.parseInt(rtcpAttributeValue);

//            TransportAddress defaultRtcpAddress =
//                    new TransportAddress(streamConnAddr, rtcpPort, Transport.UDP);

            Component rtpComponent = stream.getComponent(Component.RTP);
//            Component rtcpComponent = stream.getComponent(Component.RTCP);

            Candidate<?> defaultRtpCandidate
                    = rtpComponent.findRemoteCandidate(defaultRtpAddress);
            rtpComponent.setDefaultRemoteCandidate(defaultRtpCandidate);

//            if (rtcpComponent != null) {
//                Candidate<?> defaultRtcpCandidate
//                        = rtcpComponent.findRemoteCandidate(defaultRtcpAddress);
//                rtcpComponent.setDefaultRemoteCandidate(defaultRtcpCandidate);
//            }
        }

        String fingerprint = sdess.getAttribute("fingerprint");
        if (fingerprint == null) {
            MediaDescription md = (MediaDescription) sdess.getMediaDescriptions(false).get(0);
            fingerprint = md.getAttribute("fingerprint");
        }
        String[] offerFingerprintSplit = fingerprint.split(" ");
        dtlsControl.setRemoteFingerprints(Collections.singletonMap(offerFingerprintSplit[0], offerFingerprintSplit[1]));
    }

    /**
     * Parses the <tt>attribute</tt>.
     *
     * @param attribute the attribute that we need to parse.
     * @param stream    the {@link IceMediaStream} that the candidate is supposed
     *                  to belong to.
     * @return a newly created {@link RemoteCandidate} matching the
     * content of the specified <tt>attribute</tt> or <tt>null</tt> if the
     * candidate belonged to a component we don't have.
     */
    private static RemoteCandidate parseCandidate(Attribute attribute,
                                                  IceMediaStream stream) {
        String value = null;

        try {
            value = attribute.getValue();
        } catch (Throwable t) {
        }//can't happen

        StringTokenizer tokenizer = new StringTokenizer(value);

        //XXX add exception handling.
        String foundation = tokenizer.nextToken();
        int componentID = Integer.parseInt(tokenizer.nextToken());
        Transport transport = Transport.parse(tokenizer.nextToken().toLowerCase());
        long priority = Long.parseLong(tokenizer.nextToken());
        String address = tokenizer.nextToken();
        int port = Integer.parseInt(tokenizer.nextToken());

        TransportAddress transAddr
                = new TransportAddress(address, port, transport);

        tokenizer.nextToken(); //skip the "typ" String
        CandidateType type = CandidateType.parse(tokenizer.nextToken());

        Component component = stream.getComponent(componentID);

        if (component == null)
            return null;

        // check if there's a related address property

        RemoteCandidate relatedCandidate = null;
        if (tokenizer.countTokens() >= 4) {
            tokenizer.nextToken(); // skip the raddr element
            String relatedAddr = tokenizer.nextToken();
            tokenizer.nextToken(); // skip the rport element
            int relatedPort = Integer.parseInt(tokenizer.nextToken());

            TransportAddress raddr = new TransportAddress(
                    relatedAddr, relatedPort, Transport.UDP);

            relatedCandidate = component.findRemoteCandidate(raddr);
        }

        RemoteCandidate cand = new RemoteCandidate(transAddr, component, type,
                foundation, priority, relatedCandidate);

        component.addRemoteCandidate(cand);

        return cand;
    }

    public static AttributeField attributeField(String name, String value) {
        AttributeField ssrc = new AttributeField();
        try {
            ssrc.setName(name);
            ssrc.setValue(value);
        } catch (SdpException e) {
            e.printStackTrace();
        }
        return ssrc;
    }

    public static String createAnswer(Agent agent, DtlsControl dtlsControl) throws IOException, SdpException {
        File file = ResourceUtils.getFile("classpath:answer.sdp");
        String answerSdpStr = new String(Files.readAllBytes(file.toPath()));
        SessionDescription answerSdp = new NistSdpFactory().createSessionDescription(answerSdpStr);
        MediaDescription md = (MediaDescription) answerSdp.getMediaDescriptions(false).get(0);
        Vector attributes = md.getAttributes(false);
        attributes.add(SdpUtils.attributeField("ice-ufrag", agent.getLocalUfrag()));
        attributes.add(SdpUtils.attributeField("ice-pwd", agent.getLocalPassword()));
        for (Candidate<?> candidate : agent.getStreams().get(0).getComponent(Component.RTP).getLocalCandidates()) {
            attributes.add(new CandidateAttribute(candidate));
        }
        attributes.add(SdpUtils.attributeField("fingerprint", dtlsControl.getLocalFingerprintHashFunction() + " " + dtlsControl.getLocalFingerprint()));
        return answerSdp.toString();
    }

    public static String createOffer(Agent agent, DtlsControl dtlsControl) throws IOException, SdpException {
        File file = ResourceUtils.getFile("classpath:offer.sdp");
        String offerStr = new String(Files.readAllBytes(file.toPath()));
        SessionDescription offerSdp = new NistSdpFactory().createSessionDescription(offerStr);
        MediaDescription md = (MediaDescription) offerSdp.getMediaDescriptions(false).get(0);
        Vector attributes = md.getAttributes(false);
        attributes.add(SdpUtils.attributeField("ice-ufrag", agent.getLocalUfrag()));
        attributes.add(SdpUtils.attributeField("ice-pwd", agent.getLocalPassword()));
        for (Candidate<?> candidate : agent.getStreams().get(0).getComponent(Component.RTP).getLocalCandidates()) {
            attributes.add(new CandidateAttribute(candidate));
        }
        attributes.add(SdpUtils.attributeField("fingerprint", dtlsControl.getLocalFingerprintHashFunction() + " " + dtlsControl.getLocalFingerprint()));
        return offerSdp.toString();
    }
}
