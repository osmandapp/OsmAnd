/*
 * The MIT License
 *
 * Copyright 2018 Sonu Auti http://sonuauti.com twitter @SonuAuti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package androidhttpweb;

import android.net.TrafficStats;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 *
 * @author Sonu Auti @cis
 */
public class TinyWebServer extends Thread {

    /**
     * @param args the command line arguments
     */
    private static ServerSocket serverSocket;
    private final Map<String, String> lowerCaseHeader = new HashMap<>();

    public static String CONTENT_TYPE = "text/html";
    private String CONTENT_DATE = "";
    private String CONN_TYPE = "";
    private String Content_Encoding = "";
    private String content_length = "";
    private String STATUS = "200";
    private boolean keepAlive = true;
    private String SERVER_NAME = "Firefly http server v0.1";
    private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";
    private static final String ASCII_ENCODING = "US-ASCII";
    private String REQUEST_TYPE = "GET";
    private String HTTP_VER = "HTTP/1.1";

    //all status
    public static String PAGE_NOT_FOUND = "404";
    public static String OKAY = "200";
    public static String CREATED = "201";
    public static String ACCEPTED = "202";
    public static String NO_CONTENT = "204";
    public static String PARTIAL_NO_CONTENT = "206";
    public static String MULTI_STATUS = "207";
    public static String MOVED_PERMANENTLY = "301";
    public static String SEE_OTHER = "303";
    public static String NOT_MODIFIED = "304";
    public static String TEMP_REDIRECT = "307";
    public static String BAD_REQUEST = "400";
    public static String UNAUTHORIZED_REQUEST = "401";
    public static String FORBIDDEN = "403";
    public static String NOT_FOUND = "404";
    public static String METHOD_NOT_ALLOWED = "405";
    public static String NOT_ACCEPTABLE = "406";
    public static String REQUEST_TIMEOUT = "408";
    public static String CONFLICT = "409";
    public static String GONE = "410";
    public static String LENGTH_REQUIRED = "411";
    public static String PRECONDITION_FAILED = "412";

    public static String PAYLOAD_TOO_LARGE = "413";
    public static String UNSUPPORTED_MEDIA_TYPE = "415";
    public static String RANGE_NOT_SATISFIABLE = "416";
    public static String EXPECTATION_FAILED = "417";
    public static String TOO_MANY_REQUESTS = "429";

    public static String INTERNAL_ERROR = "500";
    public static String NOT_IMPLEMENTED = "501";
    public static String SERVICE_UNAVAILABLE = "503";
    public static String UNSUPPORTED_HTTP_VERSION = "505";

    public static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";

    public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";

    public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";

    public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

    public static final String CONTENT_LENGTH_REGEX = "Content-Length:";
    public static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile(CONTENT_LENGTH_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String USER_AGENT = "User-Agent:";
    public static final Pattern USER_AGENT_PATTERN = Pattern.compile(USER_AGENT, Pattern.CASE_INSENSITIVE);

    public static final String HOST_REGEX = "Host:";
    public static final Pattern CLIENT_HOST_PATTERN = Pattern.compile(HOST_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONNECTION_TYPE_REGEX = "Connection:";
    public static final Pattern CONNECTION_TYPE_PATTERN = Pattern.compile(CONNECTION_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String ACCEPT_ENCODING_REGEX = "Accept-Encoding:";
    public static final Pattern ACCEPT_ENCODING_PATTERN = Pattern.compile(ACCEPT_ENCODING_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";

    private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

    private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);
    
    
    public static String WEB_DIR_PATH="/";
    public static String SERVER_IP="localhost";
    public static int SERVER_PORT=9000;
    public static boolean isStart=true;
    public static String INDEX_FILE_NAME="index.html";
    public static Object object;

    public TinyWebServer(final String ip, final int port) throws IOException {
        
        InetAddress addr = InetAddress.getByName(ip); ////"172.31.0.186");
        serverSocket = new ServerSocket(port, 100, addr);
        serverSocket.setSoTimeout(5000);  //set timeout for listner

    }

    @Override
    public void run() {

        while (isStart) {
            try {
                TrafficStats.setThreadStatsTag(1);
                Socket newSocket = serverSocket.accept();
                Thread newClient = new EchoThread(newSocket);
                newClient.start();
            } catch (SocketTimeoutException s) {
            } catch (IOException e) {
            }

        }//endof Never Ending while loop

    }

    public class EchoThread extends Thread {

        protected Socket socket;
        protected boolean nb_open;

        public EchoThread(Socket clientSocket) {
            this.socket = clientSocket;
            this.nb_open = true;
        }

        @Override
        public void run() {

            try {
                DataInputStream in = null;
                DataOutputStream out = null;

                if (socket.isConnected()) {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                }

                byte[] data = new byte[1500];
                //socket.setSoTimeout(60 * 1000 * 5);

                while (in.read(data) != -1) {
                    String recData = new String(data).trim();
                    //System.out.println("received data: \n" + recData);
                    //System.out.println("------------------------------");
                    String[] header = recData.split("\\r?\\n");

                    String contentLen = "0";
                    String contentType = "text/html";
                    String connectionType = "keep-alive";
                    String hostname = "";
                    String userAgent = "";
                    String encoding = "";

                    String[] h1 = header[0].split(" ");
                    if (h1.length == 3) {
                        setRequestType(h1[0]);
                        setHttpVer(h1[2]);
                    }

                    for (int h = 0; h < header.length; h++) {
                        String value = header[h].trim();

                        //System.out.println(header[h]+" -> "+CONTENT_LENGTH_PATTERN.matcher(header[h]).find());
                        if (CONTENT_LENGTH_PATTERN.matcher(value).find()) {
                            contentLen = value.split(":")[1].trim();
                        } else if (CONTENT_TYPE_PATTERN.matcher(value).find()) {
                            contentType = value.split(":")[1].trim();
                        } else if (CONNECTION_TYPE_PATTERN.matcher(value).find()) {
                            connectionType = value.split(":")[1].trim();
                        } else if (CLIENT_HOST_PATTERN.matcher(value).find()) {
                            hostname = value.split(":")[1].trim();
                        } else if (USER_AGENT_PATTERN.matcher(value).find()) {
                            for (String ua : value.split(":")) {
                                if (!ua.equalsIgnoreCase("User-Agent:")) {
                                    userAgent += ua.trim();
                                }
                            }
                        } else if (ACCEPT_ENCODING_PATTERN.matcher(value).find()) {
                            encoding = value.split(":")[1].trim();
                        }

                    }

                    if (!REQUEST_TYPE.equals("")) {
                        String postData = "";
                        if (REQUEST_TYPE.equalsIgnoreCase("POST") && !contentLen.equals("0")) {
                            postData = header[header.length - 1];
                            if (postData.length() > 0 && contentLen.length() > 0) {
                                int len = Integer.valueOf(contentLen);
                                postData = postData.substring(0, len);
                               // System.out.println("Post data -> " + contentLen + " ->" + postData);
                            }
                        }

                       // System.out.println("contentLen ->" + contentLen + "\ncontentType ->" + contentType + "\nhostname ->" + hostname + "\nconnectionType-> " + connectionType + "\nhostname ->" + hostname + "\nuserAgent -> " + userAgent);
                        final String requestLocation = h1[1];
                        if (requestLocation != null) {
                            processLocation(out, requestLocation, postData);
                        }
                        //System.out.println("requestLocation "+requestLocation);
                    }

                }
            } catch (Exception er) {
                er.printStackTrace();
            }

        }

    }

    public void processLocation(DataOutputStream out, String location, String postData) {

        String data = "";
        switch (location) {
            case "/":
                //root location, server index file
                CONTENT_TYPE = "text/html";
                data=readFile(WEB_DIR_PATH+"/"+INDEX_FILE_NAME);
                constructHeader(out, data.length() + "", data);
                break;
            default:

                System.out.println("url location -> " + location);
                URL geturl = getDecodedUrl("http://localhost" + location);
                String[] dirPath = geturl.getPath().split("/");
                String fullFilePath=geturl.getPath();
                if (dirPath.length > 1) {
                    String fileName = dirPath[dirPath.length - 1];
                    HashMap qparms = (HashMap) splitQuery(geturl.getQuery());
                    if(REQUEST_TYPE.equals("POST")){
                        if (qparms==null){ qparms=new HashMap<String, String>();}
                        qparms.put("_POST", postData);
                    }
                    //System.out.println("File name " + fileName);
                    //System.out.println("url parms " + qparms);
                    CONTENT_TYPE = getContentType(fileName);
                    if(!CONTENT_TYPE.equals("text/plain")){
                       // System.out.println("Full file path - >"+fullFilePath +" "+CONTENT_TYPE);

                        if(CONTENT_TYPE.equals("image/jpeg") || CONTENT_TYPE.equals("image/png") || CONTENT_TYPE.equals("video/mp4")){
                           byte[] bytdata=readImageFiles(WEB_DIR_PATH+fullFilePath,CONTENT_TYPE); 
                           //System.out.println(bytdata.length);
                           if(bytdata!=null){
                                constructHeaderImage(out, bytdata.length+"", bytdata);
                           }else{
                                 pageNotFound();
                           }
                        }else{
                            data=readFile(WEB_DIR_PATH+fullFilePath);
                            if(!data.equals("")){
                                constructHeader(out, data.length() + "", data);
                            }else{
                                pageNotFound();
                            }
                        }
                    }else{
                        data = getResultByName(fileName, qparms);
                        constructHeader(out, data.length() + "", data);
                    }
                    
                    
                }

        }

    }

    public URL getDecodedUrl(String parms) {
        try {
            //String decodedurl =URLDecoder.decode(parms,"UTF-8"); 
            URL aURL = new URL(parms);
            return aURL;
        } catch (Exception er) {
        }
        return null;
    }

    public static HashMap<String, String> splitQuery(String parms) {
        try {
            final HashMap<String, String> query_pairs = new HashMap<>();
            final String[] pairs = parms.split("&");
            for (String pair : pairs) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                if (!query_pairs.containsKey(key)) {
                    query_pairs.put(key, "");
                }
                final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                query_pairs.put(key, value);
            }
            return query_pairs;
        } catch (Exception er) {
        }
        return null;
    }

    public String getResultByName(String name, HashMap qparms) {
        try {
            String ClassName = "appapis.queryfiles.AppApis";
            Class<?> rClass = Class.forName(ClassName); // convert string classname to class
            Object obj = rClass.newInstance();          // invoke empty constructor
            Method getNameMethod = obj.getClass().getMethod(name, HashMap.class);
            STATUS = TinyWebServer.OKAY;
            return getNameMethod.invoke(obj, qparms).toString();
        } catch (Exception er) {
           // er.printStackTrace();
            return pageNotFound();
        }
    }

    public void setRequestType(String type) {
       // System.out.println("REQUEST TYPE " + type);
        this.REQUEST_TYPE = type;
    }

    public void setHttpVer(String httpver) {
       // System.out.println("REQUEST ver " + httpver);
        this.HTTP_VER = httpver;
    }

    public String getRequestType() {
        return this.REQUEST_TYPE;
    }

    public String getHttpVer() {
        return this.HTTP_VER;
    }

    public String pageNotFound() {
        STATUS = NOT_FOUND;
        CONTENT_TYPE = "text/html";
        //customize your page here
        return "<!DOCTYPE html>"
                + "<html><head><title>Page not found | Firefly web server</title>"
                + "</head><body><h3>Requested page not found</h3></body></html>";
    }

    //hashtable initilization for content types
    static Hashtable<String, String> mContentTypes = new Hashtable();

    {
        mContentTypes.put("js", "application/javascript");
        mContentTypes.put("php", "text/html");
        mContentTypes.put("java", "text/html");
        mContentTypes.put("json", "application/json");
        mContentTypes.put("png", "image/png");
        mContentTypes.put("jpg", "image/jpeg");
        mContentTypes.put("html", "text/html");
        mContentTypes.put("css", "text/css");
        mContentTypes.put("mp4", "video/mp4");
        mContentTypes.put("mov", "video/quicktime");
        mContentTypes.put("wmv", "video/x-ms-wmv");

    }

    //get request content type
    public static String getContentType(String path) {
        String type = tryGetContentType(path);
        if (type != null) {
            return type;
        }
        return "text/plain";
    }

    //get request content type from path
    public static String tryGetContentType(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String e = path.substring(index + 1);
            String ct = mContentTypes.get(e);
           // System.out.println("content type: " + ct);
            if (ct != null) {
                return ct;
            }
        }
        return null;
    }

    private void constructHeader(DataOutputStream output, String size, String data) {
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
        pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
        if (this.CONTENT_TYPE != null) {
            printHeader(pw, "Content-Type", this.CONTENT_TYPE);
        }
        printHeader(pw, "Date", gmtFrmt.format(new Date()));
        printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
        printHeader(pw, "Content-Length", size);
        printHeader(pw, "Server", SERVER_NAME);
        pw.append("\r\n");
        pw.append(data);
        pw.flush();
        //pw.close();
    }
    
    private void constructHeaderImage(DataOutputStream output, String size, byte[] data) {
        try{
        
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
            pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
            if (this.CONTENT_TYPE != null) {
                printHeader(pw, "Content-Type", this.CONTENT_TYPE);
            }
            printHeader(pw, "Date", gmtFrmt.format(new Date()));
            printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
            printHeader(pw, "Content-Length", size);
            printHeader(pw, "Server", SERVER_NAME);
            pw.append("\r\n");
            pw.flush();
            output.write(data);
            output.flush();
            //System.out.println("data sent success");
        
        //pw.close();
        }catch(Exception er){er.printStackTrace();}

    }


    @SuppressWarnings("static-method")
    protected void printHeader(PrintWriter pw, String key, String value) {
        pw.append(key).append(": ").append(value).append("\r\n");
    }
    
    public byte[] readImageFiles(String fileName, String filetype){
        try{
        File ifile=new File(fileName);
            if(ifile.exists()){
                if(filetype.equalsIgnoreCase("image/png") || filetype.equalsIgnoreCase("image/jpeg") || filetype.equalsIgnoreCase("image/gif") || filetype.equalsIgnoreCase("image/jpg")){
                           FileInputStream fis = new FileInputStream(fileName);
                           byte[] buffer = new byte[fis.available()];
                           while (fis.read(buffer) != -1) {}
                           fis.close();
                           return buffer; 
                }
            }else{
            
            }
          }catch(Exception er){}
        return null;
    }
    public String readFile(String fileName){
        String content="";
        try{
            File ifile=new File(fileName);
            if(ifile.exists()){
                FileInputStream fis = new FileInputStream(fileName);
                    byte[] buffer = new byte[10];
                    StringBuilder sb = new StringBuilder();
                    while (fis.read(buffer) != -1) {
                            sb.append(new String(buffer));
                            buffer = new byte[10];
                    }
                    fis.close();
                    content = sb.toString();
            }else{
                pageNotFound();
                return content;
            }
        }catch(Exception er){
            pageNotFound();
            return "";
        }
        return content;
    }
    
    
    public static void init(String ip, int port, String public_dir){
    
        SERVER_IP=ip;
        SERVER_PORT=port;
        WEB_DIR_PATH=public_dir;
        scanFileDirectory();
        
    }
    
    public static void startServer(String ip, int port, String public_dir){
        try {

            isStart=true;
            init(ip,port,public_dir);
            Thread t = new TinyWebServer(SERVER_IP, SERVER_PORT);
            t.start();
            System.out.println("Server Started !");
           
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }
    
    public static void stopServer(){
        if(isStart){
            try{
            isStart=false;
            serverSocket.close();
            System.out.println("Server stopped running !");
            }catch(IOException er){
                er.printStackTrace();
            }
        }
    }
    
    
    //scan for index file
    public static void scanFileDirectory(){
        boolean isIndexFound=false;
        try{
            File file=new File(WEB_DIR_PATH);
            if(file.isDirectory()){
                File[] allFiles=file.listFiles();
                for (File allFile : allFiles) {
                    //System.out.println(allFile.getName().split("\\.")[0]);
                    if(allFile.getName().split("\\.")[0].equalsIgnoreCase("index")){
                        TinyWebServer.INDEX_FILE_NAME=allFile.getName();
                        isIndexFound=true;
                    }
                }
            }
            
        }catch(Exception er){}
        
        if(!isIndexFound){
            System.out.println("Index file not found !");
        }
    }
    
   /* //use for testing
    public static void main(String[] args) {
        try {

            Thread t = new TinyWebServer(SERVER_IP, SERVER_PORT);
            t.start();
            System.out.println("Server Started !");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }*/

}
