package com.example.androiddownload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements OnClickListener{

	private EditText ed_url;
	private EditText ed_thread;
	private Button btn_down;
	private LinearLayout linear;
	//线程数
	private int threadCount;
	private int blockSize;
	String path = "http://10.0.2.2:8080/3ASiCNanjing.mp3";
	private String fileAdress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ed_url = (EditText) findViewById(R.id.eddizhi);
		ed_thread = (EditText) findViewById(R.id.edthread);
		btn_down = (Button) findViewById(R.id.btndown);
		linear = (LinearLayout) findViewById(R.id.threadnumber);
		fileAdress = getCacheDir().getAbsolutePath()+File.separator+getFileName(path);
		btn_down.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		linear.removeAllViews();
		String number = ed_thread.getText().toString().trim();
		if(number.isEmpty()) {
			return;
		}
		threadCount = Integer.parseInt(number);
		for (int i = 0; i < threadCount; i++) {
			View.inflate(getApplicationContext(), R.layout.progressitem, linear);
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					URL url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(1000*10);
					int code = conn.getResponseCode();
					if(code == 200) {
						int filelength = conn.getContentLength();
						System.out.println(filelength);
						File file = new File(fileAdress);
						RandomAccessFile raf = new RandomAccessFile(file, "rw");
						raf.setLength(filelength);
						blockSize = filelength/threadCount;
						for(int i = 0;i<threadCount;i++) {
							//启动线程时计算每个线程下载的开始字节位置和技术字节位置
							int startindex = i*blockSize;
							int endindex = (i+1)*blockSize-1;
							if(i == threadCount-1) {
								endindex = filelength-1;
							}
							ProgressBar pb = (ProgressBar) linear.getChildAt(i);
							//设置进度条最大值
							pb.setMax(endindex-startindex);
							new Thread(new ThreadDown(startindex, endindex, i, pb)).start();
						}
						raf.close();
					}
					
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}).start();
		
	}
	
	private class ThreadDown implements Runnable{
		private int startbyte;
		private int endbyte;
		private int threadId;
		private int beganIndex = 0;
		private BufferedReader bfr;
		private RandomAccessFile log;
		private RandomAccessFile raf;
		private ProgressBar pb;
		
		public ThreadDown(int startindex, int endindex, int threadId, ProgressBar pb) {
			this.startbyte = startindex;
			this.endbyte = endindex;
			this.threadId = threadId;
			this.pb = pb;
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
				conn.setConnectTimeout(1000*10);
				//设置Range头用计算好的开始索引和结束索引到服务端请求数据
				conn.setRequestProperty("Range", "bytes="+(startbyte+beganIndex)+"-"+endbyte);
				if(conn.getResponseCode() == 206) { 
					InputStream inputStream = conn.getInputStream();
					int len = -1;
					int count = 0;
					byte[] buffer = new byte[1024*1000];
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
						//设置进度条当前值
						pb.setProgress((startbyte+beganIndex)-threadId*blockSize+count);
					}
					if(temp!=null) {
						temp.delete();
					}
					
				}
			} catch (Exception e) {
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
