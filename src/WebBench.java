import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

enum Method {
    GET, HEAD, OPTIONS, TRACE
}

public class WebBench {
    static final Logger logger = LoggerFactory.getLogger(WebBench.class);
    static String PROGRAM_VERSION = "1.5";
    int clients = 3;
    int benchtime = 5;
    int http10 = 1;
    int force = 0;
    int force_reload = 0;
    int proxyport = 80;
    String proxyhost = null;
    Method method = Method.GET;
    String url;
    String requests;
    boolean reply = false;

    class BenchCore implements Runnable {
        int failed = 0;
        int bytes = 0;
        int speed = 0;

        public void run() {
            Socket sock;
            InetAddress addr;
            long time1 = System.currentTimeMillis();
            while (true) {
                long time2 = System.currentTimeMillis();
                if (time2 - time1 >= benchtime * 1000) {
                    if (this.failed > 0) {
                        this.failed--;
                    }
                    break;
                }
                try {
                    sock = new Socket(proxyhost, proxyport);
                    addr = sock.getInetAddress();
                    logger.trace("连接到" + addr);
                } catch (Exception e) {
                    this.failed++;
                    continue;
                }
                try {
                    DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
                    dos.writeUTF(requests);
                } catch (Exception e) {
                    this.failed++;
                    try {
                        sock.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    continue;
                }
                if (reply) {
                    try {
                        BufferedReader buf = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        String info = null;
                        StringBuffer sbu = new StringBuffer();
                        while ((info = buf.readLine()) != null) {
                            sbu.append(info);
                        }
//                    logger.debug(sbu);
                        this.bytes += sbu.length();
                    } catch (Exception e) {
                        this.failed++;
                        try {
                            sock.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        continue;
                    }
                }
                try {
                    sock.close();
                    this.speed++;
                } catch (Exception e) {
                    this.failed++;
                    e.printStackTrace();
                }
            }
            String info = String.format("failed:%d, speed:%d, bytes:%d", this.failed, this.speed, this.bytes);
            logger.warn(info);
        }
    }

    public static void Copyright() {
        logger.warn("Webbench - Simple Web Benchmark " + PROGRAM_VERSION + "\n Copyright (c) Radim Kolar 1997-2004, GPL Open Source Software.\n");
    }

    public WebBench(String url) {
        this.requests = build_request(url);
    }

    public int getClients() {
        return this.clients;
    }

    public int getBenchtime() {
        return benchtime;
    }

    public int getProxyport() {
        return this.proxyport;
    }

    public String getProxyhost() {
        return this.proxyhost;
    }

    public String getRequest() {
        return this.requests;
    }

    public String build_request(String url) {
        this.url = url;
        String req = null;
        String host = null;
        if (!url.startsWith("http://") || url.length() > 1500) {
            logger.error("url illegal");
            return null;
        }
        String tmpUrl = url.replaceAll("http://", "").trim();
        int firstSlash = tmpUrl.indexOf('/');
        if (firstSlash == -1) {
            logger.error("url illegal, no end slash");
            return null;
        }
        int dotPosition = tmpUrl.indexOf(':');
        if (dotPosition != -1) {
            this.proxyport = Integer.parseInt(tmpUrl.substring(dotPosition, firstSlash));
            host = tmpUrl.substring(0, dotPosition);
        } else {
            host = tmpUrl.substring(0, firstSlash);
        }
        this.proxyhost = host;
        if (firstSlash == tmpUrl.length() - 1) {
            req = "/";
        } else {
            req = tmpUrl.substring(firstSlash);
        }

        StringBuffer request = new StringBuffer();
        if (this.force_reload > 0 && this.proxyhost != null && this.http10 < 1) {
            this.http10 = 1;
        }
        if (this.method == Method.HEAD && this.http10 < 1) {
            this.http10 = 1;
        }
        if (this.method == Method.OPTIONS && this.http10 < 2) {
            this.http10 = 2;
        }
        if (this.method == Method.TRACE && this.http10 < 2) {
            this.http10 = 2;
        }
        switch (method) {
            case GET:
                request.append("GET");
                break;
            case HEAD:
                request.append("HEAD");
                break;
            case OPTIONS:
                request.append("OPTIONS");
                break;
            case TRACE:
                request.append("TRACE");
                break;
            default:
                break;
        }
        request.append(" ");
        request.append(req);

        if (this.http10 == 1) {
            request.append(" HTTP/1.0");
        } else if (this.http10 == 2) {
            request.append(" HTTP/1.1");
        }
        request.append("\r\n");
        if (this.http10 > 0) {
            request.append("User-Agent: WebBench ");
            request.append(PROGRAM_VERSION);
            request.append("\r\n");
            request.append("Host:");
            request.append(host);
            request.append("\r\n");
        }
        if (this.force_reload > 0) {
            request.append("Pragma: no-cache\r\n");
        }
        if (this.http10 > 0) {
            request.append("Connection: close\r\n");
        }
        request.append("\r\n");
        return request.toString();
    }

    public void run() {
        ThreadPoolExecutor tp = new ThreadPoolExecutor(10, 20, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        logger.debug(this.getRequest());
        for (int i = 0; i < this.clients; i++) {
            BenchCore task = this.new BenchCore();
            tp.execute(task);
            logger.debug(String.valueOf(i));
        }
        logger.debug("finish");
        tp.shutdown();
        return;
    }

    public static void main(String[] args) {
        WebBench wb = new WebBench("http://www.baidu.com/");
        wb.run();
    }
}
