package edu.ufl.cise.bytetorrent.config;

import edu.ufl.cise.bytetorrent.model.Peer;
import edu.ufl.cise.bytetorrent.util.LoggerUtil;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;

public class PeerInfoConfig {
    public static final String FILE_NAME = "PeerInfo.cfg";
    private ArrayList<Peer> peerInfoList = new ArrayList<>();
    private static PeerInfoConfig peerInfoConfig = null;


    private PeerInfoConfig() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME));
            String readLine = "";
            while ((readLine = reader.readLine()) != null) {
                String config[] = readLine.split(" ");
                int id = Integer.parseInt(config[0]);
                String address = config[1];
                int port = Integer.parseInt(config[2]);
                boolean hasFile = false;
                if ("1".equals(config[3])) {
                    hasFile = true;
                }
                peerInfoList.add(new Peer(id, address, port, hasFile));

            }
            reader.close();
            }
        catch (Exception e){
            LoggerUtil.LogErrorMessage("ERROR: Cannot find PeerInfo.cfg file", e);
        }
    }

    public static PeerInfoConfig getInstance()
    {
        if (peerInfoConfig == null)
            peerInfoConfig = new PeerInfoConfig();

        return peerInfoConfig;
    }

    public ArrayList<Peer> getPeerInfoList() {
        return peerInfoList;
    }

    public int getNoOfPeers(){
        return peerInfoList.size();
    }
}