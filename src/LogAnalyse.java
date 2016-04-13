/***
 * 1 ͳ���߳�ִ��ʱ��
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAnalyse {
	private String matchString_start = null;
	private String matchString_end = null;
	private Map<String, String> threadMap = new ConcurrentHashMap<String, String>();

	private static String tmpFileNameRd = null;
	private String fileNameRd = null;

	FileOutputStream AnaResult_fos = null;
	BufferedWriter AnaResult_bw = null;

	FileOutputStream AnaLog_fos = null;
	BufferedWriter AnaLog_bw = null;

	static String fileName = null;
	static int maxNum = 0;
	static int minNum = 9999;
	static FileOutputStream tmp_fos = null;
	static BufferedWriter tmp_bw = null;

	public static void fetchLogName(String filePath) {
		File d = new File(filePath);
		String tmpFileNme = null;
		String tmpNumStr = null;
		int tmpNum = 0;
		if (d.isDirectory()) {
			File[] files = d.listFiles();
			for (int i = 0; i < files.length; i++) {

				if (files[i].isFile()) {
					tmpFileNme = files[i].getAbsolutePath();
					tmpNumStr = tmpFileNme.substring(
							tmpFileNme.lastIndexOf(".") + 1,
							tmpFileNme.length());

					if (tmpNumStr.equals("log")) {
						LogAnalyse.fileName = files[i].getName();
					} else {
						LogAnalyse.fileName = files[i].getName().substring(0,
								files[i].getName().lastIndexOf("."));
						if (tmpNumStr.matches("[0-9]+")) {
							tmpNum = Integer.parseInt(tmpNumStr);
							if (tmpNum > LogAnalyse.maxNum) {
								LogAnalyse.maxNum = tmpNum;
							}
							if (tmpNum < LogAnalyse.minNum) {
								LogAnalyse.minNum = tmpNum;
							}
						}
					}
				}
			} // end of for
		}// end of if(d.isDirectory())
		else {
			System.out.println("�������[" + filePath + "]�����ļ�·��");
		}

	}

	public static void writeToTmpFile(String msg) {
		try {
			tmp_bw.write(msg);
			tmp_bw.newLine();
		} catch (IOException e) {
			System.out.println("��������ļ�д���쳣");
		}
	}

	public static void init(String fileNameWr) {

		// ����ͳ�ƽ���ļ�
		try {
			tmp_fos = new FileOutputStream(fileNameWr);
		} catch (IOException e) {
			System.out.println(fileNameWr + "create failed");
			e.printStackTrace();
		}

		try {
			tmp_bw = new BufferedWriter(
					new OutputStreamWriter(tmp_fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("������������ļ�ʱ �ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}

	}

	/**
	 * ������ͳһʱ�̴�ӡ�����ݴ���һ�� ʵ��
	 * 
	 * @param fileRd
	 * @param keyWord
	 */
	public static void fileModify(String fileRd, String keyWord) {

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileRd);
		} catch (FileNotFoundException e) {
			System.out.println(fileRd + "  file not found");
			// e.printStackTrace();
			return;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		String lineOfFile = null;
		StringBuilder sb = new StringBuilder();
		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + fileRd + "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				if (lineOfFile.contains(keyWord)) {
					if (sb.length() == 0) {// �����ļ��ĵ�һ��
						sb.append(lineOfFile);
					} else {
						writeToTmpFile(sb.toString());
						sb.delete(0, sb.length());
						sb.append(lineOfFile);
					}

					/*
					 * if (lineOfFile.length() <= 80) {
					 * writeToTmpFile(lineOfFile); } else { int n =
					 * lineOfFile.length() / 80; for (int i = 0; i < n; i++) {
					 * writeToTmpFile(lineOfFile.substring((i * 80), (i + 1) *
					 * 80)); } writeToTmpFile(lineOfFile.substring((n * 80),
					 * lineOfFile.length())); }
					 */
				} else {
					sb.append(lineOfFile);
				}
			} else {
				writeToTmpFile(sb.toString());
				break; // �ļ���ȡ����
			}

		}// end of while

	}

	/*
	 * �ر���ʱ�м��ļ�
	 */
	public static void tmp_finish() {
		try {

			tmp_bw.flush();

			tmp_bw.close();
			tmp_fos.close();

		} catch (Exception e) {
			System.out.println("file close error");
		}
	}

	/**
	 * ��ͬһʱ�̴�ӡ�ı������´���һ�� ����
	 * 
	 * @param fileNameRd
	 * @param fileNameWr
	 * @param keyWord
	 */
	public static void switchLog(String fileNameRd, String fileNameWr,
			String keyWord) {
		if (new File(fileNameRd).exists()) {
			System.out.println(fileNameRd);
			LogAnalyse.init(fileNameWr);
			LogAnalyse.fileModify(fileNameRd, keyWord);
			LogAnalyse.tmp_finish();
		}

	}

	public void init(String fileNameWr, String analyseLogFilename,
			String matchString_start, String matchString_end) {
		this.threadMap.clear();
		this.matchString_start = matchString_start;
		this.matchString_end = matchString_end;

		// ����ͳ�ƽ���ļ�
		try {
			this.AnaResult_fos = new FileOutputStream(fileNameWr);
		} catch (IOException e) {
			System.out.println(fileNameWr + "create failed");
			e.printStackTrace();
		}

		try {
			this.AnaResult_bw = new BufferedWriter(new OutputStreamWriter(
					this.AnaResult_fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("������������ļ�ʱ �ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}

		// ����ͳ����־�ļ�
		try {
			this.AnaLog_fos = new FileOutputStream(analyseLogFilename);
		} catch (IOException e) {
			System.out.println(analyseLogFilename + "create failed");
			e.printStackTrace();
		}

		try {
			this.AnaLog_bw = new BufferedWriter(new OutputStreamWriter(
					this.AnaLog_fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("����������־�ļ�ʱ �ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}
	}

	public static String findFieldValue(String input, String fldName) {

		Pattern pattern = Pattern.compile("<data[^>]*name=\"" + fldName
				+ "\"><field[^>]*>[^<]*");
		Matcher matcher = pattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		// MAC_VALUE������ֻ��һ�������Բ���ѭ��ȡ
		if (matcher.find()) {
			String tmp = matcher.group();
			String src = tmp.substring(tmp.lastIndexOf(">") + 1, tmp.length());
			sb.append(src);
		}

		return sb.toString();
	}

	public void writeToAnalyseLogFile(String msg) {
		try {
			this.AnaLog_bw.write(msg);
			this.AnaLog_bw.newLine();
		} catch (IOException e) {
			System.out.println("�����������־�ļ�д���쳣");
		}
	}

	public void writeToResultFile(String msg) {
		try {
			this.AnaResult_bw.write(msg);
			this.AnaResult_bw.newLine();
		} catch (IOException e) {
			System.out.println("��������ļ�д���쳣");
		}
	}

	public void finish() {
		try {
			this.AnaLog_bw.flush();
			this.AnaResult_bw.flush();

			this.AnaLog_bw.close();
			this.AnaLog_fos.close();
			this.AnaResult_bw.close();
			this.AnaResult_fos.close();

		} catch (Exception e) {
			System.out.println("file close error");
		}
	}

	/*
	 * ʱ���ʾ��ʽת��
	 */
	static public String changeTimeLong2String(long tinmeIn) {
		long day = tinmeIn / 1000 / 3600 / 24;
		long hour = ((tinmeIn / 1000) % (24 * 3600)) / 3600;
		long minute = ((tinmeIn / 1000) % 3600) / 60;
		long second = ((tinmeIn / 1000) % 60);
		long ms = tinmeIn % 1000;
		return (day + "��" + hour + "ʱ" + minute + "��" + second + "��" + ms + "����");
	}

	/*
	 * �����߳���ֹʱ���
	 */
	public String calTimeSpace(String startTime, String endTime) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss,SSS");
		long timeSpace = 0;
		try {
			Date start = dateFormat.parse(startTime);
			Date end = dateFormat.parse(endTime);

			// System.out.println(end.getTime());
			// System.out.println(start.getTime());

			timeSpace = end.getTime() - start.getTime();
		} catch (Exception e) {
			System.out.println("���ڸ�ʽת���쳣");
		}

		long day = timeSpace / 1000 / 3600 / 24;
		long hour = ((timeSpace / 1000) % (24 * 3600)) / 3600;
		long minute = ((timeSpace / 1000) % 3600) / 60;
		long second = ((timeSpace / 1000) % 60);
		long ms = timeSpace % 1000;

		if (day > 0) {
			hour = 24; // ʱ����쳣����������ֵ��
		}

		String result = Long.toString(hour) + ":" + Long.toString(minute) + ":"
				+ Long.toString(second) + "," + Long.toString(ms);
		return result;
	}

	/*
	 * ʱ��ת��Ϊ����
	 */
	private String dateSwitchToMs(String timeIn) {
		String timeTmp = timeIn.replace(",", ":");
		String[] times = timeTmp.split(":");
		long ms = Long.parseLong(times[0]) * 3600 * 1000
				+ Long.parseLong(times[1]) * 60 * 100
				+ Long.parseLong(times[2]) * 1000 + Long.parseLong(times[3]);
		return String.valueOf(ms);
	}

	/*
	 * ��ͳ�ƽ����¼���ļ�
	 */
	public void recordStaticResult(boolean islastFile) {
		String msg = null;
		Set<String> Key = this.threadMap.keySet();
		Iterator<String> it = Key.iterator();
		for (; it.hasNext();) {
			String id = it.next().toString();
			String val = this.threadMap.get(id).toString();

			if (!val.contains("|")) {
				// �ַ���û�д˷���˵��û���ҵ����߳̽����ı��ġ�
				if (islastFile) {
					String logMsg = "�߳�("
							+ id
							+ ")û���ҵ���������,�߳̿�ʼʱ��["
							+ val
							+ "]"
							+ "   in file:"
							+ this.fileNameRd.substring(
									(this.fileNameRd.lastIndexOf("\\") + 1),
									this.fileNameRd.length());
					writeToAnalyseLogFile(logMsg);
				}
			} else {
				String start = val.substring(0, val.indexOf("|"));
				String end = val
						.substring((val.indexOf("|") + 1), val.length());
				String timeSpace = calTimeSpace(start, end);
				msg = "("
						+ id
						+ ")start on["
						+ start
						+ "]end on <"
						+ end
						+ "> cost {"
						+ timeSpace
						+ "}"
						+ "<"
						+ dateSwitchToMs(timeSpace)
						+ ">"
						+ "   in file:"
						+ this.tmpFileNameRd.substring(
								(this.tmpFileNameRd.lastIndexOf("\\") + 1),
								this.tmpFileNameRd.length());

				writeToResultFile(msg);
				this.threadMap.remove(id);
			}
		}

	}

	/*
	 * ͳ����־�г��ֵ��̼߳�ÿ���̵߳�ִ��ʱ��
	 */
	public void threadStatistic(String fileNameRd) {
		this.fileNameRd = fileNameRd;
		String logMsg = null;
		FileInputStream fis = null;

		String svrInfo = null;
		try {
			fis = new FileInputStream(fileNameRd);
		} catch (FileNotFoundException e) {
			System.out.println(fileNameRd + "  file not found");
			// e.printStackTrace();
			return;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		String lineOfFile = null;
		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + fileNameRd + "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				if (lineOfFile.contains(matchString_start)) {

					// System.out.print(tmp.substring(0,23)); //���Ĵ�ӡʱ�䡣
					// int firstIndex = tmp.indexOf("[");
					// int secendIndex = tmp.indexOf("]");
					// System.out.println(tmp.substring(firstIndex+1 ,
					// secendIndex));

					svrInfo = "<"
							+ findFieldValue(lineOfFile.replaceAll(" ", "")
									.replaceAll("\r\n", "")
									.replaceAll("\n", ""), "PROGRAM_ID")
							+ "="
							+ findFieldValue(lineOfFile.replaceAll(" ", "")
									.replaceAll("\r\n", "")
									.replaceAll("\n", ""), "SOURCE_TYPE")
							+ "="

							+ findFieldValue(lineOfFile.replaceAll(" ", "")
									.replaceAll("\r\n", "")
									.replaceAll("\n", ""), "SERVICE_CODE")
							+ "="
							+ findFieldValue(lineOfFile.replaceAll(" ", "")
									.replaceAll("\r\n", "")
									.replaceAll("\n", ""), "MESSAGE_TYPE")
							+ "="
							+ findFieldValue(lineOfFile.replaceAll(" ", "")
									.replaceAll("\r\n", "")
									.replaceAll("\n", ""), "MESSAGE_CODE")
							+ ">";

					String threadName = lineOfFile.substring(
							lineOfFile.indexOf("[") + 1,
							lineOfFile.indexOf("]"));
					String startTime = lineOfFile.substring(0, 23);

					// add for debug
					// if (threadName.equals("Thread-2165940")) {
					// System.out.println(threadName);
					// }
					// add end

					if (this.threadMap.containsKey(threadName)) {
						// �̺߳��Ѿ����ֹ�
						logMsg = "("
								+ threadName
								+ ")"
								+ svrInfo
								+ "�ظ����֣��ظ�ʱ��["
								+ startTime
								+ "]"
								+ "   in file:"
								+ this.fileNameRd
										.substring((this.fileNameRd
												.lastIndexOf("\\") + 1),
												this.fileNameRd.length());
						writeToAnalyseLogFile(logMsg);
					} else {
						this.threadMap.put(threadName, startTime + "%"
								+ svrInfo);
					}
				} // end of if(lineOfFile.contains(matchString_start))
				else if (lineOfFile.contains(matchString_end)) {

					String threadName = lineOfFile.substring(
							lineOfFile.indexOf("[") + 1,
							lineOfFile.indexOf("]"));
					String endTime = lineOfFile.substring(0, 23);

					// add for debug
					// if (threadName.equals("Thread-2165940")) {
					// System.out.println(threadName);
					// }
					// add end

					if (this.threadMap.containsKey(threadName)) {
						svrInfo = threadMap.get(threadName).split("%")[1];
						String startTime = threadMap.get(threadName).split("%")[0];
						// String mapValue = startTime + "|" + endTime;
						// this.threadMap.put(threadName, mapValue);

						// ///////////////////////////////

						String start = startTime;
						String end = endTime;
						String timeSpace = calTimeSpace(start, end);
						String msg = "["
								+ threadName
								+ "]"
								+ "start on["
								+ start
								+ "]end on <"
								+ end
								+ "> cost {"
								+ timeSpace
								+ "}"
								+ "<,"
								+ dateSwitchToMs(timeSpace)
								+ ",>"
								+ svrInfo
								+ "   in file:"
								+ this.fileNameRd
										.substring((this.fileNameRd
												.lastIndexOf("\\") + 1),
												this.fileNameRd.length());

						writeToResultFile(msg);
						this.threadMap.remove(threadName);

						// /////////////////////////////////
					} else {
						// �߳̽������ĳ���ǰû�и��߳������ı���
						logMsg = "["
								+ threadName
								+ "]û���ҵ� ��ʼʱ�䣬����ʱ��["
								+ endTime
								+ "]"
								+ "   in file:"
								+ this.fileNameRd
										.substring((this.fileNameRd
												.lastIndexOf("\\") + 1),
												this.fileNameRd.length());
						writeToAnalyseLogFile(logMsg);
					}
				}
			}// end of if (null != lineOfFile)
			else {
				break; // �ļ���ȡ����
			}
		} // end of while

	} // end of threadStatistic()

	/*
	 * �ж��߳��Ƿ�ͬʱ����
	 */
	private boolean isConcurrence(String start_ori, String end_ori,
			String start_cmp, String end_cmp) {

		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss,SSS");
		long start_l_ori = 0, start_l_cmp = 0, end_l_ori = 0, end_l_cmp = 0;
		try {
			start_l_ori = dateFormat.parse(start_ori).getTime();
			start_l_cmp = dateFormat.parse(start_cmp).getTime();
			end_l_ori = dateFormat.parse(end_ori).getTime();
			end_l_cmp = dateFormat.parse(end_cmp).getTime();
		} catch (Exception e) {
			System.out.println("ͳ�Ʋ����̸߳���ʱ ���ڸ�ʽת���쳣" + e);
		}

		if (((start_l_ori >= start_l_cmp) && (start_l_ori <= end_l_cmp))
				|| ((end_l_ori >= start_l_cmp) && (end_l_ori <= end_l_cmp))) {
			// ��ʼ����ֹʱ�����ڲ���Աȵ��̵߳�������
			return true;
		} else {
			return false;
		}
	}

	/*
	 * �����̸߳���ͳ�� ͳ��ÿһ���̴߳���ڼ�ͬʱ���ڵ��̸߳���
	 */
	private void cocorrentThreadStatistic(String fileNameIn, String fileNameOut) {
		FileInputStream fis = null;
		Map<String, String> threadMap_ori = new ConcurrentHashMap<String, String>(); // ������ѭ��
		Map<String, String> threadMap_cmp = new ConcurrentHashMap<String, String>(); // �����ظ��������Ҳ����߳�

		int numOfthreadFinished = 0;

		try {
			fis = new FileInputStream(fileNameIn);
		} catch (FileNotFoundException e) {
			System.out.println(fileNameIn + "  file not found");
			e.printStackTrace();
			return;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("������ͳ���ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(fileNameOut);
		} catch (IOException e) {
			System.out.println(fileNameOut + "create failed");
			e.printStackTrace();
		}

		try {
			bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}

		String lineOfFile = null;

		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + fileNameIn + "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				String threadName = lineOfFile.substring(
						(lineOfFile.indexOf("(") + 1), lineOfFile.indexOf(")"));
				String start = lineOfFile.substring(
						(lineOfFile.indexOf("[") + 1), lineOfFile.indexOf("]"));
				String end = lineOfFile.substring(
						(lineOfFile.indexOf("<") + 1), lineOfFile.indexOf(">"));
				threadMap_ori.put(threadName, start + "|" + end);
				threadMap_cmp.put(threadName, start + "|" + end);
			} else {
				break;
			}
		}// end of while

		Set<String> Key_ori = threadMap_ori.keySet();
		Iterator<String> it_ori = Key_ori.iterator();

		Set<String> Key_cmp = threadMap_cmp.keySet();

		for (; it_ori.hasNext();) {
			String id_ori = it_ori.next().toString();
			String val_ori = threadMap_ori.get(id_ori).toString();
			String start_ori = val_ori.substring(0, val_ori.indexOf("|"));
			String end_ori = val_ori.substring((val_ori.indexOf("|") + 1),
					val_ori.length());
			Iterator<String> it_cmp = Key_cmp.iterator();
			int num = 0; // �������̸߳������� //
			String writeMsg = null;
			for (; it_cmp.hasNext();) {
				String id_cmp = it_cmp.next().toString();
				String val_cmp = threadMap_cmp.get(id_cmp).toString();
				String start_cmp = val_cmp.substring(0, val_cmp.indexOf("|"));
				String end_cmp = val_cmp.substring((val_cmp.indexOf("|") + 1),
						val_cmp.length());

				if ((id_ori.equals(id_cmp)) && (start_ori.endsWith(start_cmp))) {
					// ����ʱͬ�����̻߳��Լ����Լ��Ƚϣ��޳����������
					continue;
				} else if (isConcurrence(start_ori, end_ori, start_cmp, end_cmp)) {
					num++;
					writeMsg += "[" + id_cmp + "]";
				}
			}// end of for(;it_cmp.hasNext();)

			writeMsg = "�뿪ʼʱ����[" + start_ori + "]���߳�[" + id_ori + "]ͬʱ���ڵ��̹߳���["
					+ String.valueOf(num) + "]�� �̺߳�Ϊ " + writeMsg;

			if (num > 0) {
				try {
					bw.write(writeMsg);
					bw.newLine();
				} catch (Exception e) {
					System.out.println("������ͳ��д�ļ��쳣" + e);
				}
			}

			numOfthreadFinished++;
			System.out.println("finished ["
					+ String.valueOf(numOfthreadFinished) + "] thread");
			// add for debug
			// if (numOfthreadFinished == 10) {
			// break;
			// }
			// end
		}// end of for(;it_ori.hasNext();)

		try {
			bw.flush();
			br.close();
			fis.close();
			bw.close();
			fos.close();
		} catch (Exception e) {
			System.out.println("������ͳ�� file close error");
		}

		System.out.print("������ͳ�� calculate finished\n");
	}

	/*
	 * �жϵ�ǰʱ����ʱ�������е��ĸ�Ƭ���� ���ص�ֵΪ Ƭ����ż�1 ��������0 ˵����ǰʱ�䲻����ʱ��������
	 */
	private long indexOftime(String startTime, String endTime,
			long timeSpaceInSecond, String timeIn) {
		long index = 0;
		long startTime_l = 0;
		long endTime_l = 0;
		long timeIn_l = 0;

		SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		try {
			startTime_l = dfmt.parse(startTime).getTime();
			endTime_l = dfmt.parse(endTime).getTime();
			timeIn_l = dfmt.parse(timeIn).getTime();
		} catch (Exception e) {
			System.out.println("ʱ���ʽת���쳣" + e);
		}

		if ((timeIn_l < startTime_l) || (timeIn_l > endTime_l)) {
			return 0;
		} else {
			index = (timeIn_l - startTime_l) / (timeSpaceInSecond * 1000);
			index++;
			return index;
		}
	}

	/*
	 * ͳ��ĳʱ�����ڳ��ֵ��߳���Ŀ��ʱ�䴰ȫ�����������߳���ʼͳ�ơ���AnalyseLog�м�¼��û���ҵ���ʼʱ����߳�������ֹʱ��ͳ��
	 */
	private void threadCountInTimeSpace(String threadInfoFileName,
			String analyseLogFileName, String cntInTimespaceResultFname,
			String startTime, String endTime, long timeSpaceInSecond) {

		// ��ʼ�� map
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

		long start_init = 0;
		long end_init = 0;
		try {
			start_init = fmt.parse(startTime).getTime();
			end_init = fmt.parse(endTime).getTime();
		} catch (Exception e) {
			System.out.println("map ��ʼ�� ʱ���ڸ�ʽת������");
		}
		long space = timeSpaceInSecond * 1000;
		int numOfSpace = (int) ((end_init - start_init) / space + 1);

		int cnt[] = new int[numOfSpace];

		// ��ʼ������

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(threadInfoFileName);
		} catch (FileNotFoundException e) {
			System.out.println(threadInfoFileName + "not found");
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		String lineOfFile = null;

		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + threadInfoFileName
						+ "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				String timeIn = lineOfFile.substring(
						(lineOfFile.indexOf("[") + 1), lineOfFile.indexOf("]"));
				long index = indexOftime(startTime, endTime, timeSpaceInSecond,
						timeIn);
				if (index > 0) {
					cnt[(int) (index - 1)]++;// ����indexʱ����ֵ����1�����������ȥ��
				}
			} else {
				break;
			}
		}// end of while true

		try {

			br.close();
			fis.close();
		} catch (Exception e) {
			System.out.println(threadInfoFileName + "file close error");
		}
		// ////////�߳���ֹ�����ļ�ͳ�����
		// ///////////////////////ͳ����ֹʱ��δƥ����߳�
		fis = null;
		try {
			fis = new FileInputStream(analyseLogFileName);
		} catch (FileNotFoundException e) {
			System.out.println(analyseLogFileName + "not found");
			e.printStackTrace();
		}

		br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		lineOfFile = null;
		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + threadInfoFileName
						+ "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				String timeIn = lineOfFile.substring(
						(lineOfFile.indexOf("[") + 1), lineOfFile.indexOf("]"));
				long index = indexOftime(startTime, endTime, timeSpaceInSecond,
						timeIn);
				if (index > 0) {
					cnt[(int) (index - 1)]++;// ����indexʱ����ֵ����1�����������ȥ��
				}
			} else {
				break;
			}
		}// end of while true

		try {

			br.close();
			fis.close();
		} catch (Exception e) {
			System.out.println(analyseLogFileName + "file close error");
		}

		// /////ͳ�ƽ��� ��ͳ�ƽ��д���ļ�

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(cntInTimespaceResultFname);
		} catch (IOException e) {
			System.out.println(cntInTimespaceResultFname + "create failed");
			e.printStackTrace();
		}

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}

		String msg = null;
		try {
			msg = "ͳ�ƿ�ʼʱ��[" + startTime + "]����ʱ��[" + endTime + "]ʱ������"
					+ String.valueOf(timeSpaceInSecond) + "��";

			bw.write(msg);
			bw.newLine();

			// ��ѯ�����е�����
			for (int i = 0; i < cnt.length; i++) {
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss,SSS");
				long start = df.parse(startTime).getTime();

				msg = String.valueOf(i)
						+ ","
						+ String.valueOf(cnt[i])
						+ ",��ʼʱ��Ϊ["
						+ df.format(new Date(start + i * timeSpaceInSecond
								* 1000)) + "]";

				bw.write(msg);
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(cntInTimespaceResultFname + "�ļ�д��IO�쳣" + e);
		} catch (Exception e) {
			System.out.println(cntInTimespaceResultFname + "�ļ�д���IO�����쳣" + e);
		}

		try {
			bw.flush();
			bw.close();
			fos.close();
		} catch (Exception e) {
			System.out.println("ʱ���������߳�ͳ���� file close error");
		}

	}

	/***
	 * ����ͳ�ƽ�����ɲ�����
	 */

	public static void sqlGen(String tbName, String inFileName,
			String sqlFileName) {
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(sqlFileName);
		} catch (FileNotFoundException e) {
			System.out.println(sqlFileName + "create failed");
			e.printStackTrace();
		}

		try {
			bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("����SQL�ļ�ʱ �ļ�����ַ���ת���쳣");
			e1.printStackTrace();
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(inFileName);
		} catch (FileNotFoundException e) {
			System.out.println(inFileName + "  file not found");
			// e.printStackTrace();
			return;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println("�ļ������ַ���ת���쳣");
			e1.printStackTrace();
		}

		String lineOfFile = null;
		StringBuilder sb = new StringBuilder();
		while (true) {
			try {
				lineOfFile = br.readLine();
			} catch (IOException e) {
				System.out.println("read line from" + inFileName + "failed");
				e.printStackTrace();
			}

			if (null != lineOfFile) {
				lineOfFile = lineOfFile.replaceAll("\\]start on\\[",
						"',TO_TIMESTAMP('");

				lineOfFile = lineOfFile.replaceAll("]end on <",
						"', 'yyyy-mm-dd hh24:mi:ss,ff3'),TO_TIMESTAMP('");

				lineOfFile = lineOfFile.replaceAll(">.*cost.*\\}",
						"', 'yyyy-mm-dd hh24:mi:ss,ff3'),'");

				lineOfFile = lineOfFile.replaceAll("'<,", "");

				lineOfFile = lineOfFile.replaceAll("><", "'");

				lineOfFile = lineOfFile.replaceAll("=", "','");

				lineOfFile = lineOfFile.replaceAll(">.*", "');");

				lineOfFile = lineOfFile.replaceAll("\\[", "INSERT INTO "
						+ tbName + " VALUES('");
				try {
					bw.write(lineOfFile);
					bw.newLine();
				} catch (IOException e) {
					System.out.println("SQL�ļ�д���쳣");
				}

			} else {
				
				try {
					bw.write("COMMIT;");
					bw.newLine();
				} catch (IOException e) {
					System.out.println("SQL�ļ�Commitд���쳣");
				}

				break; // �ļ���ȡ����
			}

		}// end of while
		
		try {

			bw.flush();

			bw.close();
			fos.close();

		} catch (Exception e) {
			System.out.println("file close error");
		}

	}

	public static void main(String[] args) {

		String filePath = "D:\\log\\20141103\\core4\\";
		String filePathTmp = filePath + "\\tmp\\";
		String tmpfileNameWr = null;
		File destFile = new File(filePathTmp.substring(0,
				(filePathTmp.lastIndexOf("\\"))));
		destFile.mkdirs();

		String modifyKeyWord = "2014-11-03";

		LogAnalyse.fetchLogName(filePath);

		System.out.println(LogAnalyse.fileName);
		System.out.println(LogAnalyse.maxNum);
		System.out.println(LogAnalyse.minNum);

		for (int i = LogAnalyse.maxNum; i >= LogAnalyse.minNum; i--) {
			String fileNameRdDyn = LogAnalyse.fileName + "."
					+ String.valueOf(i);

			tmpfileNameWr = "\\tmp\\tmp_" + fileNameRdDyn;
			LogAnalyse.switchLog(filePath + fileNameRdDyn, filePath
					+ tmpfileNameWr, modifyKeyWord);
		}

		tmpfileNameWr = "\\tmp\\tmp_" + LogAnalyse.fileName;
		LogAnalyse.switchLog(filePath + LogAnalyse.fileName, filePath
				+ tmpfileNameWr, modifyKeyWord);

		System.out.println("tmp file finish");

		// String fileNameRd =
		// "D:\\hejie\\work\\backup\\�����������ڼ�¼\\20131223����\\�������㱨�Ľ����쳣\\1123����\\log\\beps.log";
		// String fileNameRd = "D:\\log\\core1\\test\\tmp\\tmp_Ensemble1.log";

		String fileNameRd = filePath + tmpfileNameWr;
		String fileNameWr = "ThreadStatistic.csv";
		String analyseLogFilename = "AnalyseLog.dat";
		String conCurrentOriFileName = "ThreadStatistic.dat";
		String conCurrentResultFileName = "coCurrentCount.dat";

		String timeSpaceThreadCntFileName = "rhreadCntInTimeSpace.csv";

		String startTime = "2014-05-27 00:12:53,342"; // ͳ��ʱ����ڽ�����������ʼʱ��
		String endTime = "2014-05-27 23:18:36,731"; // ͳ��ʱ����ڽ�����������ֹʱ��
		long timeSpaceInSecond = 60; // ����ͳ��ʱ���� ��

		LogAnalyse loganals = new LogAnalyse();

		// ///////////////////////////////////////////////////////////////////

		// //////////////////// ����־�ļ�����ȡ�߳���Ϣ
		// ///////////////////////////////////////////

		final int fileNum = 162; // ��־�ļ�����"."��������� ͳ�Ƹ��ļ����������־ //

		String matchString_start = "(TcpListenerCbsd.java:605)"; // �߳���ʼʱ��
																	// ��־�еı�ʶ��Ϣ
		String matchString_end = "(TcpListenerCbsd.java:643)"; // �߳���ֹʱ��
																// ��־�еı�ʶ��Ϣ

		long start = System.currentTimeMillis();

		loganals.init(fileNameWr, analyseLogFilename, matchString_start,
				matchString_end);

		for (int i = LogAnalyse.maxNum; i >= LogAnalyse.minNum; i--) {
			String fileNameRdDyn = fileNameRd + "." + String.valueOf(i);
			System.out.println(fileNameRdDyn);
			loganals.threadStatistic(fileNameRdDyn);
			loganals.recordStaticResult(false);
		}

		loganals.threadStatistic(fileNameRd);

		loganals.recordStaticResult(true);

		loganals.finish();

		long end = System.currentTimeMillis();
		System.out.println("����־����ȡ�ļ���� ��ʱ:"
				+ LogAnalyse.changeTimeLong2String(end - start));

		// /////////////////////////////////////////////////////////////////////////////////////////////
		// ͳ���趨ʱ�����ڴ��ڵ��߳���

		/*
		 * long start2 = System.currentTimeMillis();
		 * 
		 * loganals.threadCountInTimeSpace(conCurrentOriFileName,
		 * analyseLogFilename, timeSpaceThreadCntFileName, startTime, endTime,
		 * timeSpaceInSecond);
		 * 
		 * long end2 = System.currentTimeMillis();
		 * System.out.println("ͳ���趨ʱ�����ڴ��ڵ��߳�����ʱ�� "
		 * +LogAnalyse.changeTimeLong2String(end2-start2));
		 */

		// //////////////////////////////////////////////////////////////////////////////////////////////////
		// ͳ����ÿһ���߳�ͬʱ���ڵ��̸߳��� �ò���ʱ�ϳ���û����Ҫ����ע�͵��Ȳ�ִ�С�

		/*
		 * long start3 = System.currentTimeMillis();
		 * loganals.cocorrentThreadStatistic(conCurrentOriFileName,
		 * conCurrentResultFileName); long end3 = System.currentTimeMillis();
		 * 
		 * System.out.println("ͳ����ÿһ���߳�ͬʱ���ڵ��̸߳�����ʱ��"
		 * +LogAnalyse.changeTimeLong2String(end3-start3));
		 */

		// ����inset sql �ò��ָ���������
		
		LogAnalyse.sqlGen("LOGRESULT", fileNameWr, "LOGRESULT_insert.sql");
		
		System.out.println("finished ..");

	}

}
