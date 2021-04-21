package edu.ufl.cise.bytetorrent.service;

import edu.ufl.cise.bytetorrent.config.CommonConfig;
import edu.ufl.cise.bytetorrent.model.Peer;
import edu.ufl.cise.bytetorrent.model.message.MessageGenerator;
import edu.ufl.cise.bytetorrent.util.LoggerUtil;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChokeManagementService {

    List<Peer> unchokeList = new ArrayList<Peer>();

    /**
     * k = prefered number of peers to unchoke
     * p = unchoking interval
     * m = optimally unchoke interval
     *
     * @param allPeers
     */
    public void choke(List<Peer> allPeers) {

        boolean isPreferredNeighborsChanged = false;
        int num_peer = allPeers.size();

        synchronized (allPeers) {
            // Randomly selecting neighbors when download of file completed.
            if (FileManagementService.hasCompleteFile()) {
//					shuffle the list
                Collections.shuffle(allPeers);
                int nP = 0; // numbers of peers selected

                for (int i = 0; i < num_peer; i++) {
                    if ((allPeers.get(i)).isInterested() && (allPeers.get(i)).getSocket() != null) {
                        Peer unChokedPeer = allPeers.get(i);
                        if (nP < CommonConfig.getNumberOfdPreferredNeighbors()) {
                            if (unChokedPeer.getChoked()) {
                                unChokedPeer.setChoked(false);
                                unChokedPeer.getConnectionHandler().sendMessage(MessageGenerator.unChoke());
                            }
                            if (!unchokeList.contains(unChokedPeer)) {
                                unchokeList.add(unChokedPeer);
                                isPreferredNeighborsChanged = true;
                            }
                        } else {
                            unChokedPeer.setChoked(true);
                            unChokedPeer.getConnectionHandler().sendMessage(MessageGenerator.choke());
                        }
                        nP++;
                    }
                }
            } else {
                // selecting based on the download speeds when file download is not complete.
                allPeers.sort(Comparator.comparing(Peer::getDlSpeed));
                // selecting the peers
                float nP = 0; // numbers of peers selected

                for (int i = 0; i < num_peer; i++) {
                    if ((allPeers.get(i)).isInterested() && (allPeers.get(i)).getAllPeerID() != null) {
                        Peer unChokedPeer = allPeers.get(i);
                        if (nP < CommonConfig.getNumberOfdPreferredNeighbors()) {
                            if (unChokedPeer.getChoked()) {
                                unChokedPeer.setChoked(false);
                                unChokedPeer.getConnectionHandler().sendMessage(MessageGenerator.unChoke());
                            }
                            if (!unchokeList.contains(unChokedPeer)) {
                                unchokeList.add(unChokedPeer);
                                isPreferredNeighborsChanged = true;
                            }
                        } else {
                            unChokedPeer.setChoked(true);
                            unChokedPeer.getConnectionHandler().sendMessage(MessageGenerator.choke());
                        }
                        nP++;
                    }
                }
            }
        }
        if (isPreferredNeighborsChanged) {
            LoggerUtil.LogChangeNeighbors((ArrayList<Peer>) unchokeList);
        }
    }


    public void chokeOpt(List<Peer> allPeers) {

        int num_peer = allPeers.size();

        synchronized (allPeers) {
            Collections.shuffle(allPeers);
            int nP = 0; // numbers of peers  to be selected selected
            for (int i = 0; i < num_peer && nP < 1; i++) {
                if ((allPeers.get(i)).isInterested() && (allPeers.get(i)).getSocket() != null) {
                    nP++;
                    Peer unChokedPeer = allPeers.get(i);
                    if (unChokedPeer.getChoked()) {
                        unChokedPeer.setChoked(false);
                        unChokedPeer.getConnectionHandler().sendMessage(MessageGenerator.unChoke());
                        LoggerUtil.LogOptUnchokeNeighbor(String.valueOf(unChokedPeer.getPeerId()));
                    }
                }
            }
        }
    }

}

