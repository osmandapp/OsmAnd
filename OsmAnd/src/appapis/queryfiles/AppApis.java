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
package appapis.queryfiles;


import android.widget.Toast;

import org.apache.commons.compress.utils.IOUtils;

import androidhttpweb.ServerActivity;
import androidhttpweb.TinyWebServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;


/**
 *
 * @author cis
 */
public class AppApis {
    
    public AppApis(){
    }
    
    public String helloworld(HashMap qparms){
        //demo of simple html webpage from controller method 
        TinyWebServer.CONTENT_TYPE="text/html";
        return "<html><head><title>Simple HTML and Javascript Demo</title>\n" +
                "  <script>\n" +
                "  \n" +
                "</script>\n" +
                "  \n" +
                "  </head><body style=\"text-align:center;margin-top: 5%;\" cz-shortcut-listen=\"true\" class=\"\">\n" +
                "    <h3>Say Hello !</h3>\n" +
                "<div style=\"text-align:center;margin-left: 29%;\">\n" +
                "<div id=\"c1\" style=\"width: 100px;height: 100px;color: gray;background: gray;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c2\" style=\"width: 100px;height: 100px;color: gray;background: yellow;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c3\" style=\"width: 100px;height: 100px;color: gray;background: skyblue;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c4\" style=\"width: 100px;height: 100px;color: gray;background: yellowgreen;border-radius: 50%;float: left;\"></div>\n" +
                "<div id=\"c5\" style=\"width: 100px;height: 100px;color: gray;background: red;border-radius: 50%;position: ;position: ;float: left;\" class=\"\"></div></div>\n" +
                "  </body></html>";
    }
    
    public String simplejson(HashMap qparms){
        //simple json output demo from controller method
        String json = "{\"name\":\"sonu\",\"age\":29}";
        return json.toString();
    }
    
    public String simplegetparm(HashMap qparms){
        /*
        qparms is hashmap of get and post parameter
        
        simply use qparms.get(key) to get parameter value
        user _POST as key for post data
        e.g to get post data use qparms.get("_POST"), return will be post method 
        data
        */
        
        System.out.println("output in simplehelloworld "+qparms);
        String p="";
        if(qparms!=null){
            p=qparms.get("age")+"";
        }
        String json = "{\"name\":\"sonu\",\"age\":"+p+",\"isp\":yes}";
        return json.toString();
    }
    
    public String main(HashMap qparams){
        TinyWebServer.CONTENT_TYPE="text/html";
        final ServerActivity act = ((ServerActivity)(TinyWebServer.object));
        InputStream stream = null;
        try {
            stream = act.getAssets().open("server/main.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return result;
    }

    public String button1Click(HashMap qparams){
        try {
            final ServerActivity act = ((ServerActivity)(TinyWebServer.object));
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(act,"Hello from btn1",Toast.LENGTH_LONG).show();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
        TinyWebServer.CONTENT_TYPE="text/html";
        return "<script>\n" +
                "function goBack() {\n" +
                "  window.history.back()\n" +
                "}\n" +
                "goBack();" +
                "</script>";
    }


    public String button2Click(HashMap qparams){
        return "cancel";
    }
    //implement web callback here and access them using method name
}
