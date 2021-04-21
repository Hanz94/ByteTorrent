package edu.ufl.cise.bytetorrent.config;

import edu.ufl.cise.bytetorrent.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class CommonConfig {
    private static CommonConfig commonConfig = null;
    private static int NumberOfPreferredNeighbors;
    private static int UnchokingInterval;
    private static int OptimisticUnchokingInterval;
    private static String FileName ;
    private static int FileSize ;
    private static int PieceSize;

    private HashMap<String, String> info_list = new HashMap<>();
    private static final String FILE_NAME ="Common.cfg";

    private CommonConfig()
    {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME));
            String readLine = "";
            while ((readLine = reader.readLine()) != null) {
                String config[] = readLine.split(" ");
                info_list.put(config[0], config[1]);
            }
            reader.close();
            NumberOfPreferredNeighbors = Integer.parseInt(info_list.get("NumberOfPreferredNeighbors"));
            UnchokingInterval = Integer.parseInt(info_list.get("UnchokingInterval"));
            OptimisticUnchokingInterval = Integer.parseInt(info_list.get("OptimisticUnchokingInterval"));
            FileName = info_list.get("FileName");
            FileSize = Integer.parseInt(info_list.get("FileSize"));
            PieceSize = Integer.parseInt(info_list.get("PieceSize"));
        } catch (FileNotFoundException e) {
            LoggerUtil.LogErrorMessage("ERROR: Cannot find Common.cfg file", e);
        } catch (IOException e) {
            LoggerUtil.LogErrorMessage("Cannot open the file", e);
        }
    }

    public static CommonConfig initialize()
    {
        if (commonConfig == null)
            commonConfig = new CommonConfig();

        return commonConfig;
    }

    public static int getNumberOfdPreferredNeighbors() {
        return NumberOfPreferredNeighbors;
    }

    public static int getUnchokingInterval() {
        return UnchokingInterval;
    }

    public static int getOptimisticUnchokingInterval() {
        return OptimisticUnchokingInterval;
    }

    public static String getFileName() {
        return FileName;
    }

    public static int getFileSize() {
        return FileSize;
    }

    public static int getPieceSize() {
        return PieceSize;
    }

}
