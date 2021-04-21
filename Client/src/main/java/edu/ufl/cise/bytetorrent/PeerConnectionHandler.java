package edu.ufl.cise.bytetorrent;

import edu.ufl.cise.bytetorrent.model.Peer;
import edu.ufl.cise.bytetorrent.service.FileManagementService;
import edu.ufl.cise.bytetorrent.util.FileUtil;
import edu.ufl.cise.bytetorrent.model.message.Handshake;
import edu.ufl.cise.bytetorrent.model.message.Message;
import edu.ufl.cise.bytetorrent.model.message.MessageGenerator;
import edu.ufl.cise.bytetorrent.model.message.payload.*;
import edu.ufl.cise.bytetorrent.util.LoggerUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.io.BufferedOutputStream;
/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for dealing with a single client's requests.
 */
public class PeerConnectionHandler extends Thread {
    private final Map<Integer, Peer> peers;
    private final Socket connection;
    private ObjectOutputStream out;
    private ObjectInputStream thisPeerInputStream;
    private Peer connectingPeer;
    private boolean isMeChocked;
    private Peer selfPeer;
    private float downloadSpeed = 0;

    public PeerConnectionHandler(Socket connection, Map<Integer, Peer> peers, Peer selfPeer) {
        this.connection = connection;
        this.peers = peers;
        this.selfPeer = selfPeer;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(connection.getOutputStream()));
            out.flush();
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

            Handshake handshake = new Handshake(-1);
            try {
                handshake = (Handshake) in.readObject();
            } catch (ClassNotFoundException e) {
                LoggerUtil.logErrorMessage(e.getMessage(), e);
            }

            Peer thisPeer = peers.get(handshake.getID());
            LoggerUtil.logConnectedMsg(String.valueOf(thisPeer.getPeerId()));
            thisPeer.setConnectionHandler(this);

            if (peers.get(handshake.getID()) == null) {
                LoggerUtil.logDebugMessage("Error performing Handshake : PeerId unknown");
            } else {
                LoggerUtil.logDebugMessage("Received Handshake Message : " + handshake.getID());
                this.connectingPeer = thisPeer;
                Message bitFieldMessage = MessageGenerator.bitfield(FileManagementService.getBitField());
                sendMessage(bitFieldMessage);
                LoggerUtil.logDebugMessage("Sent bit field message of length" + bitFieldMessage.getMessageLength());
                while (thisPeer.getSocket() == null) {
                    Thread.sleep(1000);
                }
                this.thisPeerInputStream = new ObjectInputStream(new BufferedInputStream(thisPeer.getSocket().getInputStream()));
                new Thread(this::listenToMessages).start();
            }

        } catch (IOException e) {
            LoggerUtil.logErrorMessage(e.getMessage(), e);
            LoggerUtil.logDebugMessage("Shutting down peer connection handler for " + connectingPeer.getPeerId());
        } catch (Exception e) {
            LoggerUtil.logErrorMessage(e.getMessage(), e);
            LoggerUtil.logDebugMessage("Shutting down peer connection handler for " + connectingPeer.getPeerId());
        }
    }

    public void sendMessage(Message msg) {
        try {
            if( msg != null){
                out.writeObject(msg);
                out.flush();
                LoggerUtil.logDebugMessage("Send message: " + msg.getMessageType() + " to " + connectingPeer.getPeerId());
            }
        } catch (IOException e) {
            LoggerUtil.logErrorMessage(e.getMessage(), e);
        }
    }

    private void listenToMessages() {
        while (!selfPeer.isCompletedDownloading()) {
            Message message = null;
            try {
                message = (Message) thisPeerInputStream.readObject();
                LoggerUtil.logDebugMessage("Receive message: " + message.getMessageType() + " from " + connectingPeer.getPeerId());

                switch (message.getMessageType()) {
                    case CHOKE:
                        isMeChocked = true;
                        LoggerUtil.logReceivedChokingMsg(String.valueOf(connectingPeer.getPeerId()));
                        break;
                    case UNCHOKE:
                        isMeChocked = false;
                        LoggerUtil.logReceivedUnchokingMsg(String.valueOf(connectingPeer.getPeerId()));
                        sendRequestMessage();
                        break;
                    case INTERESTED:
                        connectingPeer.setInterested(true);
                        LoggerUtil.logReceivedInterestedMsg(String.valueOf(connectingPeer.getPeerId()));
                        break;
                    case NOT_INTERESTED:
                        connectingPeer.setInterested(false);
                        LoggerUtil.logReceivedNotInterestedMsg(String.valueOf(connectingPeer.getPeerId()));
                        break;
                    case HAVE:
                        HavePayLoad haveIndex = (HavePayLoad) message.getPayload();
                        if (connectingPeer.getBitField() != null) {
                            connectingPeer.setBitField(FileUtil.updateBitfield(haveIndex.getIndex(), connectingPeer.getBitField()));
                            if (FileManagementService.getNumFilePieces() == connectingPeer.incrementAndGetNoOfPieces()) {
                                connectingPeer.setHasFile(true);
                                LoggerUtil.logCompleteDownload(connectingPeer);
                                checkAllDownloaded();
                            }
                            LoggerUtil.logReceivedHaveMsg(String.valueOf(connectingPeer.getPeerId()), haveIndex.getIndex());
                        }
                        break;
                    case BITFIELD:
                        BitFieldPayLoad bitFieldPayLoad = (BitFieldPayLoad) message.getPayload();
                        connectingPeer.setBitField(bitFieldPayLoad.getBitfield());
                        connectingPeer.setNoOfPiecesOwned(FileUtil.bitCount(bitFieldPayLoad.getBitfield()));
                        if (!FileManagementService.compareBitfields(bitFieldPayLoad.getBitfield(), selfPeer.getBitField())) {
                            LoggerUtil.logDebugMessage("Peer " + connectingPeer.getPeerId() + " have no any interesting pieces");
                            sendMessage(MessageGenerator.notInterested());
                        } else {
                            LoggerUtil.logDebugMessage("Peer " + connectingPeer.getPeerId() + " has interesting pieces");
                            sendMessage(MessageGenerator.interested());
                        }
                        break;
                    case REQUEST:
                        RequestPayLoad requestPayLoad = (RequestPayLoad) message.getPayload();
                        byte[] pieceContent = FileManagementService.getFilePart(requestPayLoad.getIndex());
                        sendMessage(MessageGenerator.piece(requestPayLoad.getIndex(), pieceContent));
                        connectingPeer.setDlSpeed(downloadSpeed++);
                        break;
                    case PIECE:
                        PiecePayLoad piece = (PiecePayLoad) message.getPayload();
                        try {
                            FileManagementService.store(piece.getContent(), piece.getIndex());
                            selfPeer.setBitField(FileManagementService.getBitField());
                        } catch (Exception e) {
                            LoggerUtil.logErrorMessage(e.getMessage(), e);
                        }
                        peers.values().stream().filter(peer -> peer.getConnectionHandler() != null).forEach(peer -> peer.getConnectionHandler().sendMessage(MessageGenerator.have(piece.getIndex())));
                        if (!isMeChocked)
                            sendRequestMessage();
                        LoggerUtil.logDownloadingPiece(String.valueOf(connectingPeer.getPeerId()), piece.getIndex(), selfPeer.incrementAndGetNoOfPieces());
                        if (FileManagementService.hasCompleteFile()) {
                            LoggerUtil.logDebugMessage("My self completed Downloading :" + selfPeer.getPeerId());
                            selfPeer.setHasFile(true);
                            checkAllDownloaded();
                        }
                        break;
                }

            } catch (SocketException e){
                LoggerUtil.logErrorMessage(e.getMessage(), e);
                break;
            }catch (IOException | ClassNotFoundException e) {
                LoggerUtil.logErrorMessage(e.getMessage(), e);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LoggerUtil.logErrorMessage(e.getMessage(), e);
            }
        }
        LoggerUtil.logDebugMessage("Shutting down peer connection handler for " + connectingPeer.getPeerId());
        cleanUp();
    }

    private void cleanUp(){
        try {
            out.close();
            thisPeerInputStream.close();
            connection.close();
        } catch (IOException e) {
            LoggerUtil.logErrorMessage(e.getMessage(), e);
        }
    }

    private void checkAllDownloaded() {
        if (selfPeer.isHasFile() && checkAllPeersDownloaded()) {
            selfPeer.setCompletedDownloading(true);
        }
    }

    private boolean checkAllPeersDownloaded() {
        return peers.values().stream().filter(Peer::isHasFile).count() == peers.size();
    }

    private void sendRequestMessage() {
        int pieceIdx = FileManagementService.requestPiece(connectingPeer.getBitField(), selfPeer.getBitField(), connectingPeer.getPeerId());
        if (pieceIdx == -1) {
            LoggerUtil.logDebugMessage("No more interesting pieces to request from peer " + connectingPeer.getPeerId());
            sendMessage(MessageGenerator.notInterested());
        } else {
            sendMessage(MessageGenerator.request(pieceIdx));
        }
    }

}
