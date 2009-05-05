/**
 * Copyright 2009 Olivier Bonal <olivier.bonal@gmail.com>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package cauchy.android.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

/**
 * @author obonal
 * 
 */
public class PicasaWSUtils extends DefaultHandler {
    
    private static final String LOG_TAG = "[CAUCHY_LOG]";
    
    private final static String ENTRY_TAG = "entry";
    private final static String TITLE_TAG = "title";
    private final static String ID_TAG = "id";
    
    private Map<String, String> albumsIdsByAlbumTitles = new HashMap<String, String>();
    
    private String currentElementName;
    private String currentEntryID;
    private String currentEntryTitle;
    
    private boolean inEntryTag = false;
    
    private String authString;
    
    private String userID;
    
    public void startElement(String uri,
                             String name,
                             String qName,
                             Attributes atts) {
        currentElementName = name.trim();
        if ( ENTRY_TAG.equals( name.trim())) {
            inEntryTag = true;
        }
    }
    
    public void endElement(String uri, String name, String qName) throws SAXException {
        currentElementName = null;
        if ( ENTRY_TAG.equals( name.trim())) {
            inEntryTag = false;
            if ( currentEntryID != null && currentEntryTitle != null) {
                albumsIdsByAlbumTitles.put( currentEntryTitle, currentEntryID);
            }
        }
    }
    
    public void characters(char ch[], int start, int length) {
        if ( currentElementName == null) {
            return;
        }
        String chars = ( new String( ch).substring( start, start + length));
        
        if ( inEntryTag && ID_TAG.equals( currentElementName)) {
            currentEntryID = chars;
        } else if ( inEntryTag && TITLE_TAG.equals( currentElementName)) {
            currentEntryTitle = chars;
        }
    }
    
    public Map<String, String> getAlbumsIdsByAlbumTitles() {
        String url_string = "http://picasaweb.google.com/data/feed/api/user/"
                + userID;
        try {
            URL url = new URL( url_string);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler( this);
            xr.parse( new InputSource( url.openStream()));
            return albumsIdsByAlbumTitles;
        } catch ( Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public PicasaWSUtils(String user, String passwd) {
        userID = user;
        String url = "https://www.google.com/accounts/ClientLogin";
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        try {
            HttpPost httpost = new HttpPost( new URI( url));
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add( new BasicNameValuePair( "accountType", "GOOGLE"));
            nvps.add( new BasicNameValuePair( "Email", userID));
            nvps.add( new BasicNameValuePair( "Passwd", passwd));
            nvps.add( new BasicNameValuePair( "service", "lh2"));
            nvps.add( new BasicNameValuePair( "source",
                                              "companyName-applicationName-1.0"));
            
            httpost.setEntity( new UrlEncodedFormEntity( nvps, HTTP.UTF_8));
            
            // Post, check and show the result (not really spectacular, but
            // works):
            response = httpclient.execute( httpost);
            HttpEntity entity = response.getEntity();
            
            Log.d( LOG_TAG, "Google Login auth result = " + response.getStatusLine());
            
            if ( entity != null) {
                InputStream toto = entity.getContent();
                long content_length = entity.getContentLength();
                StringBuffer content_buf = new StringBuffer();
                for ( long i = 0; i < content_length; i++) {
                    content_buf.append( ( (char) toto.read()));
                }
                
                int index = content_buf.toString().indexOf( "Auth=");
                if ( index != -1) {
                    authString = content_buf.toString()
                                            .substring( index
                                                                + "Auth=".length(),
                                                        content_buf.toString()
                                                                   .length() - 1);
                }
                entity.consumeContent();
            } else {
                Log.d( LOG_TAG, "Entity is null!");
            }
            
        } catch ( URISyntaxException e) {
            e.printStackTrace();
        } catch ( UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch ( ClientProtocolException e) {
            e.printStackTrace();
        } catch ( IOException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().closeExpiredConnections();
        }
    }
    
    public void createAlbum(String album_name) {
        
        if ( authString == null) {
            return;
        }
        String url = "http://picasaweb.google.com/data/feed/api/user/" + userID;
        // HttpParams params = new BasicHttpParams();
        // HttpProtocolParams.setVersion( params, HttpVersion.HTTP_1_1);
        // HttpProtocolParams.setContentCharset( params, "UTF-8");
        // HttpProtocolParams.setUseExpectContinue( params, false);
        //        
        // HttpClient httpclient = new DefaultHttpClient( params);
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        HttpPost httpPost = new HttpPost( url);
        try {
            
            Header[] headers = new BasicHeader[2];
            headers[0] = new BasicHeader( "Content-Type",
                                          "application/atom+xml");
            headers[1] = new BasicHeader( "Authorization", "GoogleLogin auth="
                    + authString);
            httpPost.addHeader( headers[0]);
            httpPost.addHeader( headers[1]);
            
            String content_string = "<entry xmlns='http://www.w3.org/2005/Atom'"
                    + " xmlns:media='http://search.yahoo.com/mrss/'"
                    + " xmlns:gphoto='http://schemas.google.com/photos/2007'>"
                    + " <title type='text'>"
                    + album_name
                    + "</title>"
                    + " <gphoto:access>public</gphoto:access>"
                    + "<category scheme='http://schemas.google.com/g/2005#kind'"
                    + " term='http://schemas.google.com/photos/2007#album'>"
                    + "</category>" + "</entry>";
            
            httpPost.setEntity( new StringEntity( content_string));
            
            // Post, check and show the result (not really spectacular, but
            // works):
            response = httpclient.execute( httpPost);
            HttpEntity entity = response.getEntity();
            Log.d( LOG_TAG, "Create Album Result: " + response.getStatusLine());
            
            if ( entity != null) {
                InputStream toto = entity.getContent();
                long content_length = entity.getContentLength();
                StringBuffer content_buf = new StringBuffer();
                for ( long i = 0; i < content_length; i++) {
                    content_buf.append( ( (char) toto.read()));
                }
                Log.d( LOG_TAG, "Content = [" + content_buf.toString() + "]");
                
                entity.consumeContent();
            } else {
                Log.d( LOG_TAG, "Entity is null!");
            }
            
        } catch ( UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch ( ClientProtocolException e) {
            e.printStackTrace();
        } catch ( IOException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().closeExpiredConnections();
        }
    }
    
    public void addPicture(byte[] picture_data, String album_name) {
        
        Map<String, String> lookup = getAlbumsIdsByAlbumTitles();
        String album_id = lookup.get( album_name);
        
        if ( album_id == null) {
            Log.d( LOG_TAG, album_name + "album_id not found, create album!");
            createAlbum( album_name);
            lookup = getAlbumsIdsByAlbumTitles();
            try {
                // Wait 1s to make sure album is created
                Thread.sleep( 1000l);
            } catch ( InterruptedException e) {
                e.printStackTrace();
            }
            album_id = lookup.get( album_name);
            if ( album_id == null) {
                Log.d( LOG_TAG, "album id " + album_id + "  not found! ABORT!");
                return;
            }
        }
        
        String url = "http://picasaweb.google.com/data/feed/api/user/" + userID
                + "/albumid/" + album_id;
        
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion( params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset( params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue( params, false);
        
        HttpClient httpclient = new DefaultHttpClient( params);
        HttpPost httppost = new HttpPost( url);
        try {
            Header[] headers = new BasicHeader[2];
            headers[0] = new BasicHeader( "Content-Type", "image/jpeg");
            headers[1] = new BasicHeader( "Authorization", "GoogleLogin auth="
                    + authString);
            httppost.addHeader( headers[0]);
            httppost.addHeader( headers[1]);
            
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity( picture_data);
            byteArrayEntity.setContentType( headers[1]);
            httppost.setEntity( byteArrayEntity);
            
            HttpResponse response;
            response = httpclient.execute( httppost);
            
            HttpEntity entity = response.getEntity();
            if ( entity != null) {
                Log.d( LOG_TAG, "Add Picture Result: "
                        + response.getStatusLine());
                entity.consumeContent();
            }
        } catch ( ClientProtocolException e) {
            e.printStackTrace();
        } catch ( IOException e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().closeExpiredConnections();
        }
        
    }
    
}
