package com.download.test;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlDown {

	private static String path = "http://127.0.0.1:8080/3ASiCNanjing.mp3";
	private static int threadCount = 3;

	public static void main(String[] args) {
		int startbyte = 0;
		int endbyte = 0;
		try {
			URL url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(1000*10);
//			conn.setRequestProperty("Range", "byte="+startbyte+"-"+endbyte);
//			if(conn.getResponseCode() == 206) {
//				
//			}
			int code = conn.getResponseCode();
			if(code == 200) {
				int filelength = (int) conn.getContentLengthLong();
				System.out.println(filelength);
				File file = new File("E:"+File.separator+"3ASiCNanjing.mp3");
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				raf.setLength(filelength);
				int blockSize = filelength/threadCount;
				for(int i = 0;i<threadCount;i++) {
					int startindex = i*blockSize;
					int endindex = (i+1)*blockSize-1;
					if(i == threadCount-1) {
						endindex = filelength-1;
					}
					new ThreadDown(startindex, endindex, i).start();
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private static class ThreadDown extends Thread{
		private int startbyte;
		private int endbyte;
		private int threadId;
		public ThreadDown(int startindex, int endindex, int threadId) {
			this.startbyte = (int) startindex;
			this.endbyte = (int) endindex;
			this.threadId = threadId;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(1000*10);
				//设置Range头用计算好的开始索引和结束索引到服务端请求数据
				conn.setRequestProperty("Range", "bytes="+startbyte+"-"+endbyte);
				if(conn.getResponseCode() == 206) {
					InputStream inputStream = conn.getInputStream();
					int len = -1;
					byte[] buffer = new byte[1024];
					File file = new File("E:"+File.separator+"3ASiCNanjing.mp3");
					RandomAccessFile raf = new RandomAccessFile(file, "rw");
					raf.seek(startbyte);
					while((len=inputStream.read(buffer))!=-1) {
						raf.write(buffer, 0, len);
					}
					raf.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}


