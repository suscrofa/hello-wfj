/***
 * 1 统计线程执行时间
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
			System.out.println("输入参数[" + filePath + "]不是文件路径");
		}

	}

	public static void writeToTmpFile(String msg) {
		try {
			tmp_bw.write(msg);
			tmp_bw.newLine();
		} catch (IOException e) {
			System.out.println("分析结果文件写入异常");
		}
	}

	public static void init(String fileNameWr) {

		// 创建统计结果文件
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
			System.out.println("创建分析结果文件时 文件输出字符集转换异常");
			e1.printStackTrace();
		}

	}

	/**
	 * 将报文统一时刻打印的数据打在一行 实现
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
			System.out.println("文件读入字符集转换异常");
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
					if (sb.length() == 0) {// 读入文件的第一行
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
				break; // 文件读取结束
			}

		}// end of while

	}

	/*
	 * 关闭临时中间文件
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
	 * 将同一时刻打印的报文重新打在一行 调用
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

		// 创建统计结果文件
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
			System.out.println("创建分析结果文件时 文件输出字符集转换异常");
			e1.printStackTrace();
		}

		// 创建统计日志文件
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
			System.out.println("创建分析日志文件时 文件输出字符集转换异常");
			e1.printStackTrace();
		}
	}

	public static String findFieldValue(String input, String fldName) {

		Pattern pattern = Pattern.compile("<data[^>]*name=\"" + fldName
				+ "\"><field[^>]*>[^<]*");
		Matcher matcher = pattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		// MAC_VALUE报文中只有一个，所以不用循环取
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
			System.out.println("分析程序的日志文件写入异常");
		}
	}

	public void writeToResultFile(String msg) {
		try {
			this.AnaResult_bw.write(msg);
			this.AnaResult_bw.newLine();
		} catch (IOException e) {
			System.out.println("分析结果文件写入异常");
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
	 * 时间表示方式转换
	 */
	static public String changeTimeLong2String(long tinmeIn) {
		long day = tinmeIn / 1000 / 3600 / 24;
		long hour = ((tinmeIn / 1000) % (24 * 3600)) / 3600;
		long minute = ((tinmeIn / 1000) % 3600) / 60;
		long second = ((tinmeIn / 1000) % 60);
		long ms = tinmeIn % 1000;
		return (day + "天" + hour + "时" + minute + "分" + second + "秒" + ms + "毫秒");
	}

	/*
	 * 计算线程起止时间差
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
			System.out.println("日期格式转换异常");
		}

		long day = timeSpace / 1000 / 3600 / 24;
		long hour = ((timeSpace / 1000) % (24 * 3600)) / 3600;
		long minute = ((timeSpace / 1000) % 3600) / 60;
		long second = ((timeSpace / 1000) % 60);
		long ms = timeSpace % 1000;

		if (day > 0) {
			hour = 24; // 时间差异常，赋个极端值。
		}

		String result = Long.toString(hour) + ":" + Long.toString(minute) + ":"
				+ Long.toString(second) + "," + Long.toString(ms);
		return result;
	}

	/*
	 * 时间转换为毫秒
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
	 * 将统计结果记录到文件
	 */
	public void recordStaticResult(boolean islastFile) {
		String msg = null;
		Set<String> Key = this.threadMap.keySet();
		Iterator<String> it = Key.iterator();
		for (; it.hasNext();) {
			String id = it.next().toString();
			String val = this.threadMap.get(id).toString();

			if (!val.contains("|")) {
				// 字符串没有此符号说明没有找到该线程结束的报文。
				if (islastFile) {
					String logMsg = "线程("
							+ id
							+ ")没有找到结束报文,线程开始时间["
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
	 * 统计日志中出现的线程及每个线程的执行时间
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
			System.out.println("文件读入字符集转换异常");
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

					// System.out.print(tmp.substring(0,23)); //报文打印时间。
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
						// 线程号已经出现过
						logMsg = "("
								+ threadName
								+ ")"
								+ svrInfo
								+ "重复出现，重复时间["
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
						// 线程结束报文出现前没有该线程启动的报文
						logMsg = "["
								+ threadName
								+ "]没有找到 开始时间，结束时间["
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
				break; // 文件读取结束
			}
		} // end of while

	} // end of threadStatistic()

	/*
	 * 判断线程是否同时存在
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
			System.out.println("统计并发线程个数时 日期格式转换异常" + e);
		}

		if (((start_l_ori >= start_l_cmp) && (start_l_ori <= end_l_cmp))
				|| ((end_l_ori >= start_l_cmp) && (end_l_ori <= end_l_cmp))) {
			// 开始或终止时间落在参与对比的线程的区间里
			return true;
		} else {
			return false;
		}
	}

	/*
	 * 并发线程个数统计 统计每一个线程存活期间同时存在的线程个数
	 */
	private void cocorrentThreadStatistic(String fileNameIn, String fileNameOut) {
		FileInputStream fis = null;
		Map<String, String> threadMap_ori = new ConcurrentHashMap<String, String>(); // 用于主循环
		Map<String, String> threadMap_cmp = new ConcurrentHashMap<String, String>(); // 用于重复遍历查找并发线程

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
			System.out.println("并发数统计文件读入字符集转换异常");
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
			System.out.println("文件输出字符集转换异常");
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
			int num = 0; // 并发的线程个数计数 //
			String writeMsg = null;
			for (; it_cmp.hasNext();) {
				String id_cmp = it_cmp.next().toString();
				String val_cmp = threadMap_cmp.get(id_cmp).toString();
				String start_cmp = val_cmp.substring(0, val_cmp.indexOf("|"));
				String end_cmp = val_cmp.substring((val_cmp.indexOf("|") + 1),
						val_cmp.length());

				if ((id_ori.equals(id_cmp)) && (start_ori.endsWith(start_cmp))) {
					// 遍历时同样的线程会自己和自己比较，剔除这种情况。
					continue;
				} else if (isConcurrence(start_ori, end_ori, start_cmp, end_cmp)) {
					num++;
					writeMsg += "[" + id_cmp + "]";
				}
			}// end of for(;it_cmp.hasNext();)

			writeMsg = "与开始时间在[" + start_ori + "]的线程[" + id_ori + "]同时存在的线程共有["
					+ String.valueOf(num) + "]个 线程号为 " + writeMsg;

			if (num > 0) {
				try {
					bw.write(writeMsg);
					bw.newLine();
				} catch (Exception e) {
					System.out.println("并发数统计写文件异常" + e);
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
			System.out.println("并发数统计 file close error");
		}

		System.out.print("并发数统计 calculate finished\n");
	}

	/*
	 * 判断当前时间在时间区间中的哪个片段中 返回的值为 片段序号加1 ，若返回0 说明当前时间不在总时间区间内
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
			System.out.println("时间格式转换异常" + e);
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
	 * 统计某时间间隔内出现的线程数目，时间窗全窗滑动，以线程起始统计。对AnalyseLog中记录的没有找到起始时间的线程则用终止时间统计
	 */
	private void threadCountInTimeSpace(String threadInfoFileName,
			String analyseLogFileName, String cntInTimespaceResultFname,
			String startTime, String endTime, long timeSpaceInSecond) {

		// 初始化 map
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

		long start_init = 0;
		long end_init = 0;
		try {
			start_init = fmt.parse(startTime).getTime();
			end_init = fmt.parse(endTime).getTime();
		} catch (Exception e) {
			System.out.println("map 初始化 时日期格式转换错误");
		}
		long space = timeSpaceInSecond * 1000;
		int numOfSpace = (int) ((end_init - start_init) / space + 1);

		int cnt[] = new int[numOfSpace];

		// 初始化结束

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
			System.out.println("文件读入字符集转换异常");
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
					cnt[(int) (index - 1)]++;// 计算index时返回值加了1，所以这里减去。
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
		// ////////线程起止正常文件统计完毕
		// ///////////////////////统计起止时间未匹配的线程
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
			System.out.println("文件读入字符集转换异常");
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
					cnt[(int) (index - 1)]++;// 计算index时返回值加了1，所以这里减去。
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

		// /////统计结束 将统计结果写入文件

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
			System.out.println("文件输出字符集转换异常");
			e1.printStackTrace();
		}

		String msg = null;
		try {
			msg = "统计开始时间[" + startTime + "]结束时间[" + endTime + "]时间间隔："
					+ String.valueOf(timeSpaceInSecond) + "秒";

			bw.write(msg);
			bw.newLine();

			// 轮询数组中的数据
			for (int i = 0; i < cnt.length; i++) {
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss,SSS");
				long start = df.parse(startTime).getTime();

				msg = String.valueOf(i)
						+ ","
						+ String.valueOf(cnt[i])
						+ ",开始时间为["
						+ df.format(new Date(start + i * timeSpaceInSecond
								* 1000)) + "]";

				bw.write(msg);
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(cntInTimespaceResultFname + "文件写入IO异常" + e);
		} catch (Exception e) {
			System.out.println(cntInTimespaceResultFname + "文件写入非IO其他异常" + e);
		}

		try {
			bw.flush();
			bw.close();
			fos.close();
		} catch (Exception e) {
			System.out.println("时间区间内线程统计中 file close error");
		}

	}

	/***
	 * 根据统计结果生成插表语句
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
			System.out.println("创建SQL文件时 文件输出字符集转换异常");
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
			System.out.println("文件读入字符集转换异常");
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
					System.out.println("SQL文件写入异常");
				}

			} else {
				
				try {
					bw.write("COMMIT;");
					bw.newLine();
				} catch (IOException e) {
					System.out.println("SQL文件Commit写入异常");
				}

				break; // 文件读取结束
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
		// "D:\\hejie\\work\\backup\\工作按照日期记录\\20131223网关\\生产清算报文接收异常\\1123网关\\log\\beps.log";
		// String fileNameRd = "D:\\log\\core1\\test\\tmp\\tmp_Ensemble1.log";

		String fileNameRd = filePath + tmpfileNameWr;
		String fileNameWr = "ThreadStatistic.csv";
		String analyseLogFilename = "AnalyseLog.dat";
		String conCurrentOriFileName = "ThreadStatistic.dat";
		String conCurrentResultFileName = "coCurrentCount.dat";

		String timeSpaceThreadCntFileName = "rhreadCntInTimeSpace.csv";

		String startTime = "2014-05-27 00:12:53,342"; // 统计时间段内交易数量的起始时间
		String endTime = "2014-05-27 23:18:36,731"; // 统计时间段内交易数量的终止时间
		long timeSpaceInSecond = 60; // 滑窗统计时间间隔 秒

		LogAnalyse loganals = new LogAnalyse();

		// ///////////////////////////////////////////////////////////////////

		// //////////////////// 从日志文件中提取线程信息
		// ///////////////////////////////////////////

		final int fileNum = 162; // 日志文件符号"."后最大的序号 统计该文件到最近的日志 //

		String matchString_start = "(TcpListenerCbsd.java:605)"; // 线程起始时刻
																	// 日志中的辨识信息
		String matchString_end = "(TcpListenerCbsd.java:643)"; // 线程终止时刻
																// 日志中的辨识信息

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
		System.out.println("从日志中提取文件完成 耗时:"
				+ LogAnalyse.changeTimeLong2String(end - start));

		// /////////////////////////////////////////////////////////////////////////////////////////////
		// 统计设定时间间隔内存在的线程数

		/*
		 * long start2 = System.currentTimeMillis();
		 * 
		 * loganals.threadCountInTimeSpace(conCurrentOriFileName,
		 * analyseLogFilename, timeSpaceThreadCntFileName, startTime, endTime,
		 * timeSpaceInSecond);
		 * 
		 * long end2 = System.currentTimeMillis();
		 * System.out.println("统计设定时间间隔内存在的线程数耗时： "
		 * +LogAnalyse.changeTimeLong2String(end2-start2));
		 */

		// //////////////////////////////////////////////////////////////////////////////////////////////////
		// 统计与每一个线程同时存在的线程个数 该步耗时较长，没有需要可以注释掉先不执行。

		/*
		 * long start3 = System.currentTimeMillis();
		 * loganals.cocorrentThreadStatistic(conCurrentOriFileName,
		 * conCurrentResultFileName); long end3 = System.currentTimeMillis();
		 * 
		 * System.out.println("统计与每一个线程同时存在的线程个数耗时："
		 * +LogAnalyse.changeTimeLong2String(end3-start3));
		 */

		// 生成inset sql 该部分根据需求定制
		
		LogAnalyse.sqlGen("LOGRESULT", fileNameWr, "LOGRESULT_insert.sql");
		
		System.out.println("finished ..");

	}

}
