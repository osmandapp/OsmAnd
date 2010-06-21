/*
 * Created on May 24, 2007
 *
 * This code belongs to Jonathan Fuerth
 */
package com.osmand.data.index;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;

import com.osmand.Base64;
import com.osmand.LogUtil;


/**
 * Initially was taken from GoogleCodeUploadTask
 */
public class GoogleCodeUploadIndex {
    
	private final static Log log = LogUtil.getLog(GoogleCodeUploadIndex.class);
	
    /**
     * Google user name to authenticate as (this is just the username part; 
     * don't include the @gmail.com part).
     */
    private String userName;
    
    /**
     * Coogle Code password (not the same as the gmail password) !!!!
     */
    private String password;
    
    /**
     * Google Code project name to upload to.
     */
    private String projectName;

    /**
     * The local path of the file to upload. 
     */
    private String fileName;
    
    /**
     * The file name that this file will be given on Google Code.
     */
    private String targetFileName;
    
    /**
     * Summary of the upload.
     */
    private String summary;
    
    
    /**
     * The labels that the download should have, separated by commas. Extra
     * whitespace before and after each label name will not be considered part
     * of the label name.
     */
    private String labels;
    
    private void log(String e){
    	log.info(e);
    }
    

    /**
     * Uploads the contents of the file {@link #fileName} to the project's
     * Google Code upload url. Performs the basic http authentication required
     * by Google Code.
     */
    public void upload() throws IOException {
        System.clearProperty("javax.net.ssl.trustStoreProvider"); // fixes open-jdk-issue //$NON-NLS-1$
        System.clearProperty("javax.net.ssl.trustStoreType"); //$NON-NLS-1$
        
        final String BOUNDARY = "CowMooCowMooCowCowCow"; //$NON-NLS-1$
        URL url = createUploadURL();
        
        log("The upload URL is " + url); //$NON-NLS-1$
        
        InputStream in = new BufferedInputStream(new FileInputStream(fileName));
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Basic " + createAuthToken(userName, password)); //$NON-NLS-1$ //$NON-NLS-2$
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY); //$NON-NLS-1$ //$NON-NLS-2$
        conn.setRequestProperty("User-Agent", "Google Code Upload Ant Task 0.1"); //$NON-NLS-1$ //$NON-NLS-2$
        
        log("Attempting to connect (username is " + userName + ")..."); //$NON-NLS-1$ //$NON-NLS-2$
        conn.connect();
        
        log("Sending request parameters..."); //$NON-NLS-1$
        OutputStream out = conn.getOutputStream();
        sendLine(out, "--" + BOUNDARY); //$NON-NLS-1$
        sendLine(out, "content-disposition: form-data; name=\"summary\""); //$NON-NLS-1$
        sendLine(out, ""); //$NON-NLS-1$
        sendLine(out, summary);
        
        if (labels != null) {
            String[] labelArray = labels.split("\\,"); //$NON-NLS-1$
            
            if (labelArray != null && labelArray.length > 0) {
                log("Setting "+labelArray.length+" label(s)"); //$NON-NLS-1$ //$NON-NLS-2$
                
                for (int n = 0, i = labelArray.length; n < i; n++) {
                    sendLine(out, "--" + BOUNDARY); //$NON-NLS-1$
                    sendLine(out, "content-disposition: form-data; name=\"label\""); //$NON-NLS-1$
                    sendLine(out, ""); //$NON-NLS-1$
                    sendLine(out, labelArray[n].trim());
                }
            }
        }
        
        log("Sending file... "+targetFileName); //$NON-NLS-1$
        sendLine(out, "--" + BOUNDARY); //$NON-NLS-1$
        sendLine(out, "content-disposition: form-data; name=\"filename\"; filename=\"" + targetFileName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        sendLine(out, "Content-Type: application/octet-stream"); //$NON-NLS-1$
        sendLine(out, ""); //$NON-NLS-1$
        int count;
        byte[] buf = new byte[8192];
        while ( (count = in.read(buf)) >= 0 ) {
            out.write(buf, 0, count);
        }
        in.close();
        sendLine(out, ""); //$NON-NLS-1$
        sendLine(out, "--" + BOUNDARY + "--"); //$NON-NLS-1$ //$NON-NLS-2$
        
        out.flush();
        out.close();
        
        // For whatever reason, you have to read from the input stream before
        // the url connection will start sending
        in = conn.getInputStream();
        
        log("Upload finished. Reading response."); //$NON-NLS-1$
        
        log("HTTP Response Headers: " + conn.getHeaderFields()); //$NON-NLS-1$
        StringBuilder responseBody = new StringBuilder();
        while ( (count = in.read(buf)) >= 0 ) {
            responseBody.append(new String(buf, 0, count, "ascii")); //$NON-NLS-1$
        }
        log(responseBody.toString());
        in.close();
        
        conn.disconnect();
    }

    /**
     * Just sends an ASCII version of the given string, followed by a CRLF line terminator,
     * to the given output stream.
     */
    private void sendLine(OutputStream out, String string) throws IOException {
        out.write(string.getBytes("ascii")); //$NON-NLS-1$
        out.write("\r\n".getBytes("ascii")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Creates a (base64-encoded) HTTP basic authentication token for the
     * given user name and password.
     */
    private static String createAuthToken(String userName, String password) {
        String string = (userName + ":" + password); //$NON-NLS-1$
        try {
            return Base64.encode(string.getBytes("UTF-8")); //$NON-NLS-1$
        }
        catch (java.io.UnsupportedEncodingException notreached){
            throw new InternalError(notreached.toString());
        }
    }

    /**
     * Creates the correct URL for uploading to the named google code project.
     * If uploadUrl is not set (this is the standard case), the correct URL will
     * be generated based on the {@link #projectName}.  Otherwise, if uploadUrl
     * is set, it will be used and the project name setting will be ignored.
     */
    private URL createUploadURL() throws MalformedURLException {
		if (projectName == null) {
			throw new NullPointerException("projectName must be set"); //$NON-NLS-1$
		}
		return new URL("https", projectName + ".googlecode.com", "/files"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

    
    // ============ Getters and Setters ==============

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }


    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }
    
    public static void main(String[] args) throws IOException {
		GoogleCodeUploadIndex uploadIndex = new GoogleCodeUploadIndex();
//    	uploadIndex.setLabels("");
//		uploadIndex.setProjectName("osmand");
//		uploadIndex.setFileName(_);
//		uploadIndex.setTargetFileName(_);
//		uploadIndex.setUserName(_);
//		uploadIndex.setSummary(_);
//		uploadIndex.setPassword(_);
		uploadIndex.upload();
	}
    
}