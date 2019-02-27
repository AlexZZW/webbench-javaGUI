import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

enum Method {
    GET, HEAD, OPTIONS, TRACE
}

class BenchCore implements Runnable {
    String host;
    int port;
    String req;
    int timeout;
    int failed = 0;
    int bytes = 0;
    int speed = 0;
    boolean reply;
    static Logger logger = Logger.getLogger(WebBench.class);

    public BenchCore(String host, int port, String req, int timeout, boolean reply) {
        this.host = host;
        this.port = port;
        this.req = req;
        this.timeout = timeout;
        this.reply = reply;
        BasicConfigurator.configure();
        logger.setLevel(Level.DEBUG);
    }

    public void run() {
        Socket sock;
        InetAddress addr;
        long time1 = System.currentTimeMillis();
        while (true) {
            long time2 = System.currentTimeMillis();
            if (time2 - time1 >= this.timeout * 1000) {
                if (this.failed > 0) {
                    this.failed--;
                }
                break;
            }
            try {
                sock = new Socket(this.host, this.port);
                addr = sock.getInetAddress();
//                logger.debug("连接到" + addr);
            } catch (Exception e) {
                this.failed++;
                continue;
            }
            try {
                DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
                dos.writeUTF(this.req);
            } catch (Exception e) {
                this.failed++;
                try {
                    sock.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                continue;
            }
            if (this.reply) {
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
        String info = String.format("failed:%d, speed:%d, bytes:%d",this.failed, this.speed, this.bytes);
        logger.warn(info);
//        System.out.println(info);
    }
}

public class WebBench {
    static Logger logger = Logger.getLogger(WebBench.class);
    static String PROGRAM_VERSION = "1.5";
    int clients = 1;
    int benchtime = 60;
    int http10 = 1;
    int force = 0;
    int force_reload = 0;
    int proxyport = 80;
    String proxyhost = null;
    Method method = Method.GET;
    String url;
    String requests;

    public static void Copyright() {
        logger.warn("Webbench - Simple Web Benchmark " + PROGRAM_VERSION + "\n Copyright (c) Radim Kolar 1997-2004, GPL Open Source Software.\n");
    }

    public WebBench() {
        BasicConfigurator.configure();
        logger.setLevel(Level.DEBUG);
    }

    public WebBench(String url) {
        this();
        this.url = url;
        this.requests = build_request();
    }

    public WebBench(String url, Level level) {
        this(url);
        logger.setLevel(level);
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

    public String build_request() {
        String url = this.url;
        String req = null;
        String host = null;
        if (!url.startsWith("http://") || url.length() > 1500) {
            logger.fatal("url illegal");
            return null;
        }
        String tmpUrl = url.replaceAll("http://", "").trim();
        int firstSlash = tmpUrl.indexOf('/');
        if (firstSlash == -1) {
            logger.fatal("url illegal, no end slash");
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

    public static void main(String[] args) {
        ThreadPoolExecutor tp = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        WebBench wb = new WebBench("http://www.baidu.com/");
        logger.debug(wb.getRequest());
        for(int i=0;i<3;i++) {
            BenchCore task = new BenchCore(wb.getProxyhost(), wb.getProxyport(), wb.getRequest(), 5, true);
            tp.execute(task);
            logger.debug(i);
        }
        logger.debug("finish");
    }
}
