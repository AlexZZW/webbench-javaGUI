import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class WorkBench {
    static Logger logger = Logger.getLogger(WorkBench.class);
    static String PROGRAM_VERSION = "1.5";

    public static String sendGet(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();
            Map<String, List<String>> map = connection.getHeaderFields();
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        logger.setLevel(Level.DEBUG);
        logger.warn("Webbench - Simple Web Benchmark " + PROGRAM_VERSION + "\n Copyright (c) Radim Kolar 1997-2004, GPL Open Source Software.\n");
        int clients = 1;
        int benchtime = 60;
        String request = "GET https://www.baidu.com/ HTTP/1.0";
        int proxyport = 80;
        String response = WorkBench.sendGet("https://www.baidu.com/ ");
        logger.debug(response);

    }
}
