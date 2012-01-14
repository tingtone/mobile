/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kylinworks;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.CRC32;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;

/**
 * 
 * @author Rex
 */

public class PNGConverter extends JFrame {
	public JLabel m_lblInfo;

	public PNGConverter() {
		super("Apple PNG file converter");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		// Set size and position of window
		int nWindowWidth = 400;
		int nWindowHeight = 200;

		setSize(nWindowWidth, nWindowHeight);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int nSystemWindowWidth = screenSize.width;
		int nSystemWindowHeight = screenSize.height;

		int windowX = Math.max(0, (nSystemWindowWidth - nWindowWidth) / 2);
		int windowY = Math.max(0, (nSystemWindowHeight - nWindowHeight) / 2);

		int nWindowLeft = windowX;
		int nWindowTop = windowY;

		if (nWindowLeft >= nSystemWindowWidth) {
			nWindowLeft = (nSystemWindowWidth - nWindowWidth) / 2;
		}
		if (nWindowTop >= nSystemWindowHeight) {
			nWindowTop = (nSystemWindowHeight - nWindowHeight) / 2;
		}

		setLocation(nWindowLeft, nWindowTop);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(null);

		JLabel lblDirectoryText = new JLabel(
				"<html><b>Select directory or PNG file to be converted.</b></html>");
		lblDirectoryText.setBounds(20, 10, 360, 20);
		mainPanel.add(lblDirectoryText);
		m_lblInfo = new JLabel();
		m_lblInfo.setBounds(20, 50, 360, 60);
		mainPanel.add(m_lblInfo);

		JButton btnSelect = new JButton("Select");
		btnSelect.setBounds(150, 120, 100, 40);
		btnSelect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser
						.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

				int returnVal = fileChooser.showDialog(PNGConverter.this,
						"Select PNG file or directory"); // .showOpenDialog(PNGHandler.this);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();

					new ConvertHandler(file).start();

				}
			}
		});
		mainPanel.add(btnSelect);

		getContentPane().add(mainPanel);
	}

	public static void main(String args[]) {

		PNGConverter converter = new PNGConverter();
		converter.setVisible(true);
	}

	class ConvertHandler extends Thread {
		File m_file;

		public ConvertHandler(File file) {
			m_file = file;
		}

		public void run() {
			if (m_file.isDirectory()) {
				convertDirectory(m_file);
			} else if (isPNGFile(m_file)) {
				convertPNGFile(m_file);
			}
			m_lblInfo.setText("Finished!");
		}

		protected ArrayList<PNGTrunk> trunks = null;

		protected boolean isPNGFile(File file) {
			String szFileName = file.getName();
			if (szFileName.length() < 5) {
				return false;
			}
			return szFileName.substring(szFileName.length() - 4)
					.equalsIgnoreCase(".png");
		}

		protected PNGTrunk getTrunk(String szName) {
			if (trunks == null) {
				return null;
			}
			PNGTrunk trunk;
			for (int n = 0; n < trunks.size(); n++) {
				trunk = trunks.get(n);
				if (trunk.getName().equalsIgnoreCase(szName)) {
					return trunk;
				}
			}
			return null;
		}

		public void convertPNGFile(File pngFile) {
			String szFullPath = pngFile.getAbsolutePath();
			String newFileName = szFullPath.substring(0,
					szFullPath.lastIndexOf(File.separator) + 1)
					+ "zzzz_" +
							pngFile.getName().substring(0,
							pngFile.getName().lastIndexOf(".")) + ".png";

			try {
				DataInputStream file = new DataInputStream(new FileInputStream(
						pngFile));
				FileOutputStream output = null;
				byte[] nPNGHeader = new byte[8];
				file.read(nPNGHeader);

				boolean bWithCgBI = false;

				trunks = new ArrayList<PNGTrunk>();
				PNGTrunk trunk;
				if ((nPNGHeader[0] == -119) && (nPNGHeader[1] == 0x50)
						&& (nPNGHeader[2] == 0x4e) && (nPNGHeader[3] == 0x47)
						&& (nPNGHeader[4] == 0x0d) && (nPNGHeader[5] == 0x0a)
						&& (nPNGHeader[6] == 0x1a) && (nPNGHeader[7] == 0x0a)) {

					do {
						trunk = PNGTrunk.generateTrunk(file);
						trunks.add(trunk);

						if (trunk.getName().equalsIgnoreCase("CgBI")) {
							bWithCgBI = true;
						}
					} while (!trunk.getName().equalsIgnoreCase("IEND"));
				}
				file.close();

				if (getTrunk("CgBI") != null) {
					String szInfo = "Dir:" + pngFile.getAbsolutePath() + "--->"
							+ newFileName;
					System.out.println("Dir:" + pngFile.getAbsolutePath()
							+ "--->" + newFileName);
					m_lblInfo.setText("<html>" + szInfo + "</html>");
					repaint();

					PNGIHDRTrunk ihdrTrunk = (PNGIHDRTrunk) getTrunk("IHDR");
					System.out.println("Width:" + ihdrTrunk.m_nWidth
							+ "  Height:" + ihdrTrunk.m_nHeight);

					int nMaxInflateBuffer = 4 * (ihdrTrunk.m_nWidth + 1)
							* ihdrTrunk.m_nHeight;
					// Convert data
					byte[] inputBuffer = new byte[nMaxInflateBuffer];
					byte[] outputBuffer = new byte[nMaxInflateBuffer];

					int allDataLength = 0;

					PNGTrunk dataTrunk = null;
					for (int i = 0; i < trunks.size(); i++) {
						dataTrunk = trunks.get(i);

						if (!dataTrunk.getName().equalsIgnoreCase("IDAT")) {
							continue;
						}

						System.arraycopy(dataTrunk.getData(), 0, inputBuffer,
								allDataLength, dataTrunk.getSize());
						allDataLength += dataTrunk.getSize();
					}

					/**
					 * 解压图片数据
					 */

					ZStream inStream = new ZStream();
					inStream.avail_in = allDataLength;
					inStream.next_in_index = 0;
					inStream.next_in = inputBuffer;
					inStream.next_out_index = 0;
					inStream.next_out = outputBuffer;
					inStream.avail_out = outputBuffer.length;

					if (inStream.inflateInit(-15) != JZlib.Z_OK) {
						System.out.println("PNGCONV_ERR_ZLIB");
						return;
					}

					int nResult = inStream.inflate(JZlib.Z_NO_FLUSH);
					switch (nResult) {
					case JZlib.Z_NEED_DICT:
						nResult = JZlib.Z_DATA_ERROR; /* and fall through */
					case JZlib.Z_DATA_ERROR:
					case JZlib.Z_MEM_ERROR:
						inStream.inflateEnd();
						System.out.println("PNGCONV_ERR_ZLIB");
						return;
					}

					nResult = inStream.inflateEnd();

					if (inStream.total_out > nMaxInflateBuffer) {
						System.out.println("PNGCONV_ERR_INFLATED_OVER");
					}

					/**
					 * 交换颜色
					 */
					int nIndex = 0;
					byte nTemp;
					for (int y = 0; y < ihdrTrunk.m_nHeight; y++) {
						nIndex++;
						for (int x = 0; x < ihdrTrunk.m_nWidth; x++) {
							nTemp = outputBuffer[nIndex];
							outputBuffer[nIndex] = outputBuffer[nIndex + 2];
							outputBuffer[nIndex + 2] = nTemp;
							nIndex += 4;
						}
					}

					/**
					 * 重新压缩
					 */

					ZStream deStream = new ZStream();
					int nMaxDeflateBuffer = nMaxInflateBuffer + 1024;
					byte[] deBuffer = new byte[nMaxDeflateBuffer];

					deStream.avail_in = (int) outputBuffer.length;
					deStream.next_in_index = 0;
					deStream.next_in = outputBuffer;
					deStream.next_out_index = 0;
					deStream.next_out = deBuffer;
					deStream.avail_out = deBuffer.length;
					deStream.deflateInit(9);
					nResult = deStream.deflate(JZlib.Z_FINISH);

					if (deStream.total_out > nMaxDeflateBuffer) {
						System.out.println("PNGCONV_ERR_DEFLATED_OVER");
					}

					long remainDataLength = deStream.total_out;
					int offset = 0;
					byte[] buffer = null;

					for (int i = 0; i < trunks.size(); i++) {
						dataTrunk = trunks.get(i);

						if (!dataTrunk.getName().equalsIgnoreCase("IDAT")) {
							continue;
						}

						int length = 0;
						if (remainDataLength > 0x80000) {
							length = 0x80000;
						} else {
							length = (int) remainDataLength;
						}

						buffer = new byte[length];
						for (int n = 0; n < length; n++) {
							buffer[n] = deBuffer[n + offset];
						}

						remainDataLength -= length;
						offset += length;

						CRC32 crc32 = new CRC32();
						crc32.update(dataTrunk.getName().getBytes());
						crc32.update(buffer);
						long lCRCValue = crc32.getValue();

						dataTrunk.m_nData = buffer;
						dataTrunk.m_nCRC[0] = (byte) ((lCRCValue & 0xFF000000) >> 24);
						dataTrunk.m_nCRC[1] = (byte) ((lCRCValue & 0xFF0000) >> 16);
						dataTrunk.m_nCRC[2] = (byte) ((lCRCValue & 0xFF00) >> 8);
						dataTrunk.m_nCRC[3] = (byte) (lCRCValue & 0xFF);
						dataTrunk.m_nSize = buffer.length;
					}

					FileOutputStream outStream = new FileOutputStream(
							newFileName);
					byte[] pngHeader = { -119, 80, 78, 71, 13, 10, 26, 10 };
					outStream.write(pngHeader);
					for (int n = 0; n < trunks.size(); n++) {
						trunk = trunks.get(n);
						if (trunk.getName().equalsIgnoreCase("CgBI")) {
							continue;
						}
						trunk.writeToStream(outStream);
					}
					outStream.close();
				}
			} catch (IOException e) {
				System.out.println("Error --" + e.toString());
			}

			try {
				sleep(100);
			} catch (Exception e) {
				// No nothing now.
			}
		}

		private void convertDirectory(File dir) {
			File[] files = dir.listFiles();

			for (int n = 0; n < files.length; n++) {
				if (files[n].isDirectory()) {
					convertDirectory(files[n]);
				} else if (isPNGFile(files[n])) {
					convertPNGFile(files[n]);
				}
			}
		}
	}
}
