package edu.ufl.cise.bytetorrent;

import edu.ufl.cise.bytetorrent.config.CommonConfig;
import edu.ufl.cise.bytetorrent.config.PeerInfoConfig;
import edu.ufl.cise.bytetorrent.model.Peer;
import edu.ufl.cise.bytetorrent.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

public class PeerProcess {
    public static void main(String[] args) {

        final int id = Integer.parseInt(args[0]);
        LoggerUtil.initialize(id);
        LoggerUtil.logInfoMessage("Process Thread Started");
        PeerInfoConfig peerInfo = PeerInfoConfig.getInstance();
        CommonConfig.initialize();
        Peer selfPeer = null;

        Map<Integer,Peer> remotePeers = new HashMap<>();

        for (Peer peer : peerInfo.getPeerInfoList()) {
            if (id == peer.getPeerId()) {
                selfPeer = peer;
            }
            else {
                remotePeers.put(peer.getPeerId(), peer);
            }
        }

        if( selfPeer == null ){
            throw new RuntimeException( " Configuration for peer " + id + " is not found in PeerInfo.cfg" );
        }

        LoggerUtil.setMyPeer(selfPeer);
        PeerManagerPlatform peerProcess = new PeerManagerPlatform(selfPeer, remotePeers);
		peerProcess.init();
    }
}
