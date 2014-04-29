// GTFS Data Animation Generator, v1
// Written by Michael Schade, (c)2014 
import java.util.*;  // date stuff
import java.text.*;
float minLng = 180;  // left
float maxLng = -180;  // right
float minLat = 90;  // bottom
float maxLat = -90;   // top  
Date minDate;
Date maxDate;
Boolean firstDate = true;
int swidth;
int sheight; 
PImage bg;   
DateFormat zulu1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
DateFormat zulu2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
int gpxFiles;
timePoint[][] gpxData;

class timePoint { 
  float lat, lng; 
  Date date;
  timePoint (float latitude, float longitude, Date d) {  
    lat = latitude; 
    lng = longitude; 
    date = d; 
    }  
  } 

float toY(float lat) { 
  return sheight - sheight*(lat - minLat)/(maxLat - minLat);
  }

float toX(float lng) { 
  return swidth*(lng - minLng)/(maxLng - minLng);
  }  

timePoint[] loadGPX(String file) {  
  String dateString;
  Date result1;
  float lat, lng;
  XML xml;  
  xml = loadXML(file);  
  XML[] children = xml.getChild("trk").getChild("trkseg").getChildren("trkpt"); 
  timePoint[] point = new timePoint[children.length];
  for (int i = 0; i < children.length; i++) {
    lat = children[i].getFloat("lat");
    lng = children[i].getFloat("lon");
    dateString = children[i].getChild("time").getContent();
    minLat = min(minLat, lat);
    maxLat = max(maxLat, lat);
    minLng = min(minLng, lng);
    maxLng = max(maxLng, lng);
    try {
      result1 = zulu1.parse(dateString);  
      } 
    catch (Exception e) {
      try {
        result1 = zulu2.parse(dateString);  
        } 
      catch (Exception e2) { 
        result1 = new Date(); // really should just FAIL big time here
        println("Unable to parse date stamp " + dateString);
        }  
      } 
    if (firstDate) {
      minDate = result1;
      maxDate = result1;
      firstDate = false;
      }
    else {
      if (result1.before(minDate)) 
        minDate = result1;
      if (result1.after(maxDate)) 
        maxDate = result1; 
      }  
    point[i] = new timePoint(lat, lng, result1);
    }
  return point;  
  }

float getRatio(Date A, Date B, Date C) {  
  // assume C is between A and B
  // return a number 0 to 1
  // 0 means A = C, 1 means A = B
  return (C.getTime() - A.getTime())/(B.getTime() - A.getTime());
  }

void loadFolder(String gpxpath) {  
  java.io.File gpxfolder = new java.io.File(dataPath(gpxpath)); 
  java.io.FilenameFilter gpxFilter = new java.io.FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.toLowerCase().endsWith(".gpx");
      }
    };
  String[] filenames = gpxfolder.list(gpxFilter); 
  gpxFiles = filenames.length;
  gpxData = new timePoint[gpxFiles][];
  println(gpxFiles + " gpx files in specified directory");
  for (int i = 0; i < gpxFiles; i++) { 
    gpxData[i] = loadGPX(gpxpath + filenames[i]);
    }
  }

void loadFile(String gpxfile) {  
  gpxFiles = 1;
  gpxData = new timePoint[1][];
  gpxData[0] = loadGPX(gpxfile);
  }

void showLimits() {
  println(maxLat);
  println(minLng + "   " + maxLng);
  println(minLat); 
  println("minDate = " + minDate);
  println("maxDate = " + maxDate);
  }

void setup() {
  //loadFolder("apr2014bikeparty/");
  loadFile("bird.gpx");
  showLimits(); 
  bg = loadImage("bird.png"); 
  swidth = 640;
  sheight = 520;
  maxLat = 38.9083;   // top  
  minLng = -77.1676;  // left
  maxLng = -77.0528;  // right
  minLat = 38.8354;  // bottom   
  int[] fileIndex = new int[gpxFiles];
  for (int i = 0; i < gpxFiles; i++) {
    fileIndex[i] = 0;
    } 
  Date drawDate = minDate;
  float x, y;
  float ratio;
  int phase = 0;
  size(swidth, sheight);  
  int imageNo = 0;
  Calendar cal = Calendar.getInstance(); 
  String folder = "frames" + cal.get(Calendar.HOUR) + "-" + cal.get(Calendar.MINUTE) + "/";  
  noFill();
  strokeWeight(5); 
  strokeJoin(ROUND);
  colorMode(HSB, 100);
  int radius;
  while (drawDate.before(maxDate) || drawDate.equals(maxDate)) {
    background(bg);
    for (int i = 0; i < gpxFiles; i++) {
      while (fileIndex[i] < gpxData[i].length && gpxData[i][fileIndex[i]].date.before(drawDate))
        fileIndex[i]++; 
      stroke(0, 90, 100, 90);
      for (int j = 0; j < fileIndex[i] - 1; j++) { 
        line(toX(gpxData[i][j].lng), toY(gpxData[i][j].lat), 
          toX(gpxData[i][j + 1].lng), toY(gpxData[i][j + 1].lat));
        }
      if (fileIndex[i] < gpxData[i].length) { 
        radius = phase + 3;
        stroke(100*i/gpxFiles, 90, 90, 204);
        stroke(18, 80, 100, 90); 
        if (gpxData[i][fileIndex[i]].date.equals(drawDate)) {
          x = toX(gpxData[i][fileIndex[i]].lng);
          y = toY(gpxData[i][fileIndex[i]].lat);
          ellipse(x, y, radius, radius);
          } 
        else if (fileIndex[i] > 0) {
          ratio = getRatio(gpxData[i][fileIndex[i] - 1].date, gpxData[i][fileIndex[i]].date, drawDate);
          x = toX(gpxData[i][fileIndex[i] - 1].lng + ratio*(gpxData[i][fileIndex[i]].lng - gpxData[i][fileIndex[i] - 1].lng));
          y = toY(gpxData[i][fileIndex[i] - 1].lat + ratio*(gpxData[i][fileIndex[i]].lat - gpxData[i][fileIndex[i] - 1].lat)); 
          ellipse(x, y, radius, radius);
          }
        }
      } 
    drawDate = new Date(drawDate.getTime() + 30000);
    phase = (phase + 1) % 15;
    //println(drawDate);
    saveFrame(folder + "image-" + nf(imageNo++, 5) + ".png"); 
    }  
  println("images = " + imageNo); 
  println("!!!"); 
  }

void draw() {   
  } 
