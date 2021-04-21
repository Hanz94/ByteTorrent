package edu.ufl.cise.bytetorrent.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;


import edu.ufl.cise.bytetorrent.config.CommonConfig;
import edu.ufl.cise.bytetorrent.util.FileUtil;
import edu.ufl.cise.bytetorrent.util.LoggerUtil;

public class FileManagementService {

    private static boolean[] filePiecesOwned;
	private static Hashtable<Integer, Integer> piecesNeeded = new Hashtable<Integer, Integer>();
	private static final int numFilePieces = (int) Math
			.ceil((double) CommonConfig.getFileSize() / CommonConfig.getPieceSize());
	private static int numPiecesIHave = 0;
	private static String path = null;
	private static String fName = CommonConfig.getFileName();
	private static int fSize = CommonConfig.getFileSize();
	private static File file = null;


	public FileManagementService(int peerId, boolean hasFile) {
		path = "peer_" + peerId + "/";

		filePiecesOwned = new boolean[numFilePieces];

		if (hasFile) {
			Arrays.fill(filePiecesOwned, true);
			numPiecesIHave = numFilePieces;
		}

		File folder = new File(path);

		if (!folder.exists()) {
			folder.mkdirs();
		}

		file = new File(path + fName);

		if (!file.exists()) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				LoggerUtil.logDebugMessage("Writing file.");
				fos.write(new byte[fSize]);
				fos.close();
			} catch (IOException e) {
				LoggerUtil.logErrorMessage(e.getMessage(), e);
			}
		}

	}


	public static synchronized boolean hasCompleteFile() {
		return numPiecesIHave == numFilePieces;
	}


	public static synchronized byte[] getBitField() throws Exception {
		int size = (int) Math.ceil((double) numFilePieces / 8);
		byte[] bitfield = new byte[size];
		int counter = 0;
		int indexI = 0;
		while (indexI < numFilePieces) {
			int temp;
			if (numFilePieces > indexI + 8) {
				temp = indexI + 8;
			} else {
				temp = numFilePieces;
			}
			bitfield[counter++] = FileUtil.toByte(Arrays.copyOfRange(filePiecesOwned, indexI, temp));
			indexI = indexI + 8;
		}
		return bitfield;
	}


	public static synchronized byte[] getFilePart(int index) {
		try {
			FileInputStream fis = new FileInputStream(file);
			int loc = CommonConfig.getPieceSize() * index;
			fis.skip(loc);
			int contentSize = CommonConfig.getPieceSize();
			if (fSize - loc < CommonConfig.getPieceSize())
				contentSize = fSize - loc;
			byte[] content = new byte[contentSize];
			fis.read(content);
			fis.close();
            LoggerUtil.logDebugMessage("reading chunk of file.");
			return content;
		} catch (FileNotFoundException e) {
			LoggerUtil.logErrorMessage(e.getMessage(), e);
			return null;
		} catch (IOException e) {
			LoggerUtil.logErrorMessage(e.getMessage(), e);
			return null;
		}
	}


	public static synchronized void store( byte[] content, int index) throws Exception {
		int loc = CommonConfig.getPieceSize() * index;
		RandomAccessFile fos = null;
		try {
			fos = new RandomAccessFile(file, "rw");
			fos.seek(loc);
			fos.write(content);
			fos.close();

			numPiecesIHave++;
			filePiecesOwned[index] = true;

		} catch (IOException e) {
			LoggerUtil.logErrorMessage(e.getMessage(), e);
		}
	}


	public static boolean compareBitfields(byte[] neighborBitfield, byte[] bitfield) {
		boolean flag = false;
		int size = (int) Math.ceil((double) numFilePieces / 8);
		byte[] interesting = new byte[size];
		if (neighborBitfield == null) {
			return flag;
		}
		int indexI = 0;
		while (indexI < bitfield.length) {
			interesting[indexI] = (byte) ((bitfield[indexI] ^ neighborBitfield[indexI]) & neighborBitfield[indexI]);
			if (interesting[indexI] != 0) {
				flag = true;
			}
			indexI++;
		}
		return flag;
	}



	public static int requestPiece(byte[] neighborBitfield, byte[] bitfield, int nPID) {
		int size = (int) Math.ceil((double) numFilePieces / 8);
		byte[] interesting = new byte[size];
		boolean[] interestingPieces = new boolean[numFilePieces];
		int finLength;

		finLength = size > 1 ? numFilePieces % (8) : numFilePieces;

		int start, end;

		int indexI = 0, indexJ = 0;
		while (indexI < bitfield.length) {
			interesting[indexI] = (byte) ((bitfield[indexI] ^ neighborBitfield[indexI]) & neighborBitfield[indexI]);
			start = indexI == size - 1 ? 8 - finLength : 0;
			end = indexI == size - 1 ? finLength : 8;
			boolean[] x = FileUtil.toBool(interesting[indexI]);
			System.arraycopy(x, start, interestingPieces, indexJ, end);
			indexJ = indexJ + 8 < numFilePieces ? indexJ + 8 : numFilePieces - finLength;
			indexI++;
		}
		int indexK = 0;
		ArrayList<Integer> temp_pieces_needed  =  new ArrayList<Integer>();
		while (indexK < numFilePieces) {
			if (interestingPieces[indexK] && !piecesNeeded.containsKey(indexK)) {
				temp_pieces_needed.add(indexK);
			}
			indexK++;
		}
		if (temp_pieces_needed.size() != 0) {
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(temp_pieces_needed.size());
			int piece = temp_pieces_needed.get(index);
			piecesNeeded.put(piece, piece);
			return piece;
		}
		return -1;
	}

	public static int getNumFilePieces() {
		return numFilePieces;
	}
}
