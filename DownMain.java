package com.down.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownMain{
	private static String path = "http://127.0.0.1:8080/jdk8_x64.exe";
	private static String fileAdress = "F:"+File.separator+getFileName(path);
	private static int threadCount = 3;
	
	public static void main(String[] args) {
		DownMain dm = new DownMain();
		try {
			URL url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(1000*10);
			int code = conn.getResponseCode();
			if(code == 200) {
				int filelength = (int) conn.getContentLengthLong();
				System.out.println(filelength);
				File file = new File(fileAdress);
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				raf.setLength(filelength);
				int blockSize = filelength/threadCount;
				for(int i = 0;i<threadCount;i++) {
					//启动线程时计算每个线程下载的开始字节位置和技术字节位置
					int startindex = i*blockSize;
					int endindex = (i+1)*blockSize-1;
					if(i == threadCount-1) {
						endindex = filelength-1;
					}
					new Thread(dm.new ThreadDown(startindex, endindex, i)).start();
				}
				raf.close();
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private  class ThreadDown implements Runnable{
		private int startbyte;
		private int endbyte;
		private int threadId;
		private int beganIndex = 0;
		private BufferedReader bfr;
		private RandomAccessFile log;
		private RandomAccessFile raf;
		public ThreadDown(int startindex, int endindex, int threadId) {
			this.startbyte = startindex;
			this.endbyte = endindex;
			this.threadId = threadId;
		}
		@Override
		public void run() {
			try {
				//用一个.log文件记录当前每个线程下载了多少字节
				File temp = new File(fileAdress+threadId+".log");
				if(temp!=null && temp.length()>0) {
					FileInputStream fis = new FileInputStream(temp);
					bfr = new BufferedReader(new InputStreamReader(fis));
					String readLine = bfr.readLine();
					beganIndex = Integer.parseInt(readLine);
					
				}
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(1000*100);
				//设置Range头用计算好的开始索引和结束索引到服务端请求数据
				conn.setRequestProperty("Range", "bytes="+(startbyte+beganIndex)+"-"+endbyte);
				if(conn.getResponseCode() == 206) { 
					InputStream inputStream = conn.getInputStream();
					int len = -1;
					int count = 0;
					byte[] buffer = new byte[1024*100];
					File file = new File(fileAdress);
					raf = new RandomAccessFile(file, "rw");
					//移动seek位置,继续上次下载位置开始下载
					raf.seek((beganIndex+startbyte));
					while((len=inputStream.read(buffer))!=-1) {
						raf.write(buffer, 0, len);
						//计算当前线程下载了多少
						count = len + count;
						log = new RandomAccessFile(fileAdress+threadId+".log", "rwd");
						log.write(String.valueOf(count).getBytes());
					}
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				try {
					if(bfr!=null) {
						bfr.close();
					}
					if(log!=null) {
						log.close();
					}
					if(raf!=null) {
						raf.close();
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
		
	}
	
	static String getFileName(String string) {
		String path = string;
		String[] split = path.split("/");
		String filename = split[split.length-1];
		return filename;
	}
}