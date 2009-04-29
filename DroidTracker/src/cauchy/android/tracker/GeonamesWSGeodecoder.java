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

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;


/*
 SAMPLE GEODECODE XML:
  
  // Using findNearestAddress
  <geonames>
    <address>
      <street/>
      <streetNumber/>
      <lat>47.627995</lat>
      <lng>-122.241713</lng>
      <distance>0.04</distance>
      <postalcode>00</postalcode>
      <placename>Medina</placename>
      <adminName2>King</adminName2>
      <adminCode1>WA</adminCode1>
      <adminName1>Washington</adminName1>
      <countryCode>US</countryCode>
     </address>
  </geonames>
  
  // Using findNearbyPlaceName (for Europe for instance)
  <geonames>
        <geoname>
            <name>Marseille</name>
            <lat>43.3</lat>
            <lng>5.4</lng>
            <geonameId>2995469</geonameId>
            <countryCode>FR</countryCode>
            <countryName>France</countryName>
            <fcl>P</fcl>
            <fcode>PPLA</fcode>
            <distance>0</distance>
        </geoname>
    </geonames>
 */

/**
 * @author obonal
 *
 */
public class GeonamesWSGeodecoder extends DefaultHandler implements IGeoDecoder {

  //private final static String geodecode_url = "http://ws.geonames.org/findNearestAddress?";
    private final static String geodecode_url = "http://ws.geonames.org/findNearbyPlaceName?";
  // WITH PARAMS"lat=xxx&lng=xxx";
    
  private final static String STREET_TAG = "street";
  private final static String STREETNUMBER_TAG = "streetNumber";
  private final static String POSTALCODE_TAG = "postalcode";
  private final static String PLACENAME_TAG = "placename";
  private final static String ADMINNAME2_TAG = "adminName2";
  private final static String ADMINCODE1_TAG = "adminCode1";
  private final static String COUNTRYCODE_TAG = "countryCode";
  
  // For findNearbyPlaceName
  private final static String COUNTRYNAME_TAG = "countryName";
  private final static String NAME_TAG = "name";
  
  private String street;
  private String streetNumber;
  private String postalcode;
  private String placename;
  private String adminName2;
  private String adminCode1;
  private String countryCode;
  private String countryName;
  private String name; 
  
  private String currentElementName;

  
   public void startElement(String uri, String name, String qName,
           Attributes atts) {
       currentElementName = name.trim();
   }

   public void endElement(String uri, String name, String qName)
           throws SAXException {
     currentElementName = null;
   }

   public void characters(char ch[], int start, int length) {
	   if ( currentElementName == null) {
		   return;
	   }
       String chars = (new String(ch).substring(start, start + length));

       if ( currentElementName.equals( STREET_TAG))
         street = chars;
       else if (currentElementName.equals( STREETNUMBER_TAG))
         streetNumber = chars;
       else if (currentElementName.equals( POSTALCODE_TAG))
         postalcode = chars;
       else if (currentElementName.equals( PLACENAME_TAG))
         placename = chars;
       else if (currentElementName.equals( ADMINNAME2_TAG))
         adminName2 = chars;
       else if (currentElementName.equals( ADMINCODE1_TAG))
         adminCode1 = chars;
       else if (currentElementName.equals( COUNTRYCODE_TAG))
         countryCode = chars;
       else if (currentElementName.equals( COUNTRYNAME_TAG))
           countryName = chars;
       else if (currentElementName.equals( NAME_TAG))
           name = chars;
   }

   /* (non-Javadoc)
 * @see cauchy.android.tracker.IGeoDecoder#getAddressFromGeoCode(java.lang.String, java.lang.String)
 */
public String getAddressFromGeoCode( String lat, String lng) {
     String url_string = geodecode_url + "lat=" + lat + "&lng=" + lng;
     Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "url string: " + url_string);
     try {
      URL url = new URL( url_string);
      SAXParserFactory spf = SAXParserFactory.newInstance();
      SAXParser sp = spf.newSAXParser();
      XMLReader xr = sp.getXMLReader();
      xr.setContentHandler( this);
      xr.parse( new InputSource( url.openStream()));
      StringBuffer result = new StringBuffer();
      if ( streetNumber != null) {
	      result.append( streetNumber);
	      result.append( " ");
      }
      if ( street != null) {
    	  result.append( street);
      }
      if ( result.length() != 0) {
    	  result.append( "\n");
      }
      if ( postalcode != null) {
    	  result.append( postalcode);
    	  result.append( " ");
      }
      if ( placename != null) {
	      result.append( placename);
      } else if ( name != null) {
          result.append( name);
      }
      if ( result.length() != 0) {
    	  result.append( "\n");
      }
      if ( adminName2 != null) {
	      result.append( adminName2);
      }
      if ( adminCode1 != null) {
    	  result.append( " - ");
	      result.append( adminCode1);
      }
      if ( result.length() != 0) {
    	  result.append( "\n");
      }
      
      if ( countryName != null) {
          result.append( countryName);
      } else if ( countryCode != null) {
    	  result.append( countryCode);
      }
      
      if ( result.length() == 0) {
    	  return null;
      }
      
      result.append( getDebugAddOn( lat, lng));
      
      Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "result: " + result.toString());
      return result.toString();
      
    } catch ( Exception e) {
      Log.println( Log.ERROR, IDroidTrackerConstants.CAUCHY_LOG, "Exception while geodecoding using: " + url_string);
      Log.println( Log.ERROR, IDroidTrackerConstants.CAUCHY_LOG, e.getMessage());
      return "ERROR: " + e.getMessage();
    }
   }

    private String getDebugAddOn( String lat, String lng) {
        return "\n( lat:" + lat + ", lng:" + lng + ")";// + "\nhttp://maps.google.com/maps?f=q&hl=en&geocode=&q=" + lat + "," + lng + "&sll="+ lat + "," + lng + "&sspn=0.010821,0.027852&ie=UTF8&t=h&z=16&g=" + lat + "+" + lng + "&iwloc=addr";
    }
   
}
