package com.brendan.NSA_Demo_06Sep;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class elasticQuery {

	private static Date dateobj;
	private static DateFormat df;

	//should accept building name, in addition to a date format
	
	public elasticQuery() {
		df = new SimpleDateFormat("yyyy-MM-dd");
	}
	
	public static String getBuilding(String building) throws Exception {
		dateobj = new Date();
		
		String date = df.format(dateobj);
		
		String urlToRead = "http://10.10.200.50:9200/logstash-*/_"
				+ "search?q=@timestamp:[" + date + "+TO+" + date + "]"
				+ "type:snort" +"&sort=@timestamp:desc&size=5";
		
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}
}
