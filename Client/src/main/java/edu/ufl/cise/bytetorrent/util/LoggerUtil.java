package edu.ufl.cise.bytetorrent.util;

import edu.ufl.cise.bytetorrent.model.Peer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.util.ArrayList;

public class LoggerUtil {

    private static Peer myPeer;

    private LoggerUtil(){ }

    public static void initialize(int peerID){
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setStatusLevel(Level.INFO);
        builder.setConfigurationName("DefaultLogger");

        AppenderComponentBuilder appenderBuilder = builder.newAppender("Console", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d %p %c [%t] %m%n"));

        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
        ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
        AppenderComponentBuilder rollingAppenderBuilder = builder.newAppender("rolling", "RollingFile")
                .addAttribute("fileName", "log_peer_"+peerID+".log")
                .addAttribute("filePattern", "log_peer_"+peerID+".log-%d{MM-dd-yy}.log.gz")
                .addAttribute("Append", false)
                .add(layoutBuilder)
                .addComponent(triggeringPolicy);

        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
        rootLogger.add(builder.newAppenderRef("Console").addAttribute("level", Level.INFO));
        rootLogger.add(builder.newAppenderRef("rolling").addAttribute("level", Level.DEBUG));

        builder.add(appenderBuilder);
        builder.add(rootLogger);
        builder.add(rollingAppenderBuilder);

        Configurator.initialize(builder.build());
    }

    public static void logInfoMessage(String message){
        LogManager.getLogger().info(message);
    }

    public static void logDebugMessage(String message){
        LogManager.getLogger().debug(message);
    }

    public static void logErrorMessage(String message, Throwable throwable){
        LogManager.getLogger().error(message, throwable);
    }

    public static void logMakeTcpConnection(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " makes a connection to Peer "+peerID+".";
        logInfoMessage(message);
    }

    public static void logConnectedMsg(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " is connected from Peer "+peerID+".";
        logInfoMessage(message);
    }

    public static void logChangeNeighbors(ArrayList<Peer> neighbors) {
        String message = "Peer "+ myPeer.getPeerId()+ " has the preferred neighbors ";
        for (Peer p : neighbors) {
            message += p.getPeerId()+",";
        }
        logInfoMessage(message.substring(0, message.length()-1));
    }

    public static void logOptUnchokeNeighbor(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " has optimistically unchoked neighbor "+peerID+".";
        logInfoMessage(message);
    }

    public static void logReceivedUnchokingMsg(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " is unchoked by "+peerID+".";
        logInfoMessage(message);
    }

    public static void logReceivedChokingMsg(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " is choked by "+peerID+".";
        logInfoMessage(message);
    }

    public static void logReceivedHaveMsg(String peerID, int piece_index) {
        String message = "Peer "+ myPeer.getPeerId()+ " received the ‘have’ message from "+peerID+" for the piece "+piece_index+".";
        logInfoMessage(message);
    }

    public static void logReceivedInterestedMsg(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " received the ‘interested’message from "+peerID+".";
        logInfoMessage(message);
    }

    public static void logReceivedNotInterestedMsg(String peerID) {
        String message = "Peer "+ myPeer.getPeerId()+ " received the ‘not interested’message from "+peerID+".";
        logInfoMessage(message);
    }

    public static void logDownloadingPiece(String peerID, int piece_index, int number_of_piece) {
        String message = "Peer "+ myPeer.getPeerId()+ " has downloaded the piece "+piece_index+" from "+peerID+". " +
                "Now the number of pieces it has is "+number_of_piece+".";
        logInfoMessage(message);
    }

    public static void logCompleteDownload(Peer connectedPeer) {
        String message = "Peer "+ connectedPeer.getPeerId()+ " has downloaded the complete file.";
        logInfoMessage(message);
    }

    public static void setMyPeer(Peer myPeer) {
        LoggerUtil.myPeer = myPeer;
    }
}
