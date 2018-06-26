package com.AR.Teaching;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class HttpPostAR {

	private static final String TAG = "HttpPost";

	public static String submitPostData(String mobilecmd, String mobilesql, int serverchoose ,String iptmp) {
		if(UnityPlayerActivity.datalogshow)
			Log.e(TAG, TAG);
		Map<String, String> params = new HashMap<String, String>();
		params.put("mobilecmd", mobilecmd);
		params.put("mobilesql", mobilesql);
		byte[] data = getRequestData(params, "utf-8").toString().getBytes();
		HttpURLConnection httpURLConnection = null;
		try {
			URL url = null;
			if (serverchoose == 1) {
				String urlstr = iptmp.substring(0, iptmp.lastIndexOf(".") + 1);
				url = new URL("http://" + urlstr + "28:30116/mobilebasecmd");
			}
			else if (serverchoose == 2)
				url = new URL("http://117.29.161.22:30116/mobilebasecmd");
			httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setConnectTimeout(1000);
			httpURLConnection.setReadTimeout(1000);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setUseCaches(false);
			httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
			httpURLConnection.connect();
			OutputStream outputStream = httpURLConnection.getOutputStream();
			outputStream.write(data);
			outputStream.flush();
			outputStream.close();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream inptStream = httpURLConnection.getInputStream();
				String retstr = dealResponseResult(inptStream);
				inptStream.close();
				httpURLConnection.disconnect();
				return retstr;
			} else {
				httpURLConnection.disconnect();
				JSONObject reterr = new JSONObject();
				try {
					reterr.put("err", "posterror");
					reterr.put("data", 0);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				String reterrstr = "[" + reterr.toString() + "]";
				return reterrstr;
			}
		} catch (IOException e) {
			JSONObject reterr = new JSONObject();
			try {
				reterr.put("err", "timeouterror");
				reterr.put("data", 0);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			String reterrstr = "[" + reterr.toString() + "]";
			return reterrstr;
		} finally {
			if (httpURLConnection != null)
				httpURLConnection.disconnect();
		}
	}

	public static StringBuffer getRequestData(Map<String, String> params, String encode) {
		StringBuffer stringBuffer = new StringBuffer();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				stringBuffer.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), encode)).append("&");
			}
			stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringBuffer;
	}

	public static String dealResponseResult(InputStream inputStream) {
		String resultData = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int len = 0;
		try {
			while ((len = inputStream.read(data)) != -1) {
				byteArrayOutputStream.write(data, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultData = new String(byteArrayOutputStream.toByteArray());
		return resultData;
	}
}