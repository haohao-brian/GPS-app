package com.example.myapplication;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static Document doc;
    // 監聽
    public final String LM_GPS = LocationManager.GPS_PROVIDER;
    public final String LM_NETWORK = LocationManager.NETWORK_PROVIDER;
    // 定位管理器
    private LocationManager mLocationManager;
    // 定位監聽器
    private LocationListener mLocationListener;

    public GoogleMap mMap;
    private Context context;
    double userLat,userLong;
    ArrayList<LatLng> points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        context = this;

        //建立監聽事件並開始監聽
        mLocationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();

        // GPS開啟
        openGPS(context);
/*
        try {
            main();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

 */
    }
    public void getMyLocation(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        // I suppressed the missing-permission warning because this wouldn't be executed in my
        // case without location services being enabled
        @SuppressLint("MissingPermission") android.location.Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        userLat = lastKnownLocation.getLatitude();
        userLong = lastKnownLocation.getLongitude();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        getMyLocation();
        LatLng usrLoction = new LatLng(24.99003820138407,121.3114136219668);//(userLat,userLong);
        mMap.addMarker(new MarkerOptions().position(usrLoction).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(usrLoction));
        playAnimateCamera(usrLoction,3000);

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setBuildingsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        UiSettings ui = mMap.getUiSettings();
        ui.setZoomControlsEnabled(true);
        ui.setZoomGesturesEnabled(true);
        ui.setScrollGesturesEnabled(true);
        ui.setRotateGesturesEnabled(true);

        //畫路徑
        drawPolyLineKML(googleMap);
    }
    private void playAnimateCamera(LatLng latlng, int durationMs){
        //bearing 旋轉方位;tilit 傾斜角度
        CameraPosition cameraPos = new CameraPosition.Builder().target(latlng)
                .zoom(17.0f).bearing(0).tilt(0).build();
        CameraUpdate cameraUpt = CameraUpdateFactory.newCameraPosition(cameraPos);
        mMap.animateCamera(cameraUpt,durationMs,null);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    // 在 resume 階段設定 mLocationListener 監聽器，以獲得地理位置的更新資料
    @Override
    protected void onResume() {
        if (mLocationManager == null) {
            mLocationManager =
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLocationListener = new MyLocationListener();
        }
        // 獲得地理位置的更新資料 (GPS 與 NETWORK都註冊)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager
                .requestLocationUpdates(LM_GPS, 1000, 3, mLocationListener);
        mLocationManager.requestLocationUpdates(LM_NETWORK, 1000, 3,
                mLocationListener);
        setTitle("onResume ...");
        super.onResume();
    }
    // 開啟 GPS
    public void openGPS(Context context) {
        boolean gps = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Toast.makeText(context, "GPS : " + gps + ", Network : " + network,
                Toast.LENGTH_SHORT).show();
        if (gps || network) {
            return;
        } else {
            // 開啟手動GPS設定畫面
            Intent gpsOptionsIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(gpsOptionsIntent);
        }
    }

    // 定位監聽器實作
    private class MyLocationListener implements LocationListener {
        // GPS位置資訊已更新
        public void onLocationChanged(Location location) {
            //LatLng newLoction = new LatLng(location.getLatitude(), location.getLongitude());
            points.add(new LatLng(location.getLatitude(),location.getLongitude()));
            PolyStyle(mMap,points);
            //setTitle("GPS位置資訊已更新");
        }
        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }
        // GPS位置資訊的狀態被更新
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    setTitle("服務中");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    setTitle("沒有服務");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    setTitle("暫時不可使用");
                    break;
            }
        }
    }

    // 畫連續線段-KML
    private void drawPolyLineKML(GoogleMap mMap) {
        points = new ArrayList<LatLng>();
        try {
            // 取得 res\router.kml 資源
            InputStream inStream = getResources().openRawResource(R.raw.router);
            // 建立 DOM 實例
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            // 取得 DOM 文件
            Document doc = db.parse(inStream);
            // 找到 "coordinates" XML 節點元素
            NodeList nl = doc.getElementsByTagName("coordinates");
            if(nl.getLength() == 0) {
                return;
            }
            int i = 0;
            while(nl.getLength() != 0){
                // 取得節點
                Node node = nl.item(i);
                // 分析節點元素內容
                String[] routers = node.getTextContent().trim().split(" ");
                for(String router : routers) {
                    String[] r = router.split(",");
                    // 將經緯度物件加入到 points 集合中
                    points.add(new LatLng(Double.parseDouble(r[1]),
                            Double.parseDouble(r[0])));
                }
                //確認style然後畫出來
                PolyStyle(mMap,points);
                points = new ArrayList<LatLng>();
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return;
        }
    }
    public void PolyStyle(GoogleMap mMap, List<LatLng> points){
        PolylineOptions options = new PolylineOptions();
        options.addAll(points);
        options.width(5);
        options.color(Color.CYAN);
        options.zIndex(1); // 疊層id(數字越高圖層越上層)
        mMap.addPolyline(options);

        //清空儲存的所有座標，保留最後一個，讓線段連起來
        for (int i = 0; i < points.size()-1; i++){
            points.remove(0);
        }
    }

    //紀錄GPS的移動路徑
    public static void recordeDocCreate(LatLng latLng) throws ParserConfigurationException,
            TransformerException {
        // 建立Builder基本函數 doc
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();

        Element root = doc.createElementNS("zetcode.com", "Placemark");
        doc.appendChild(root);
        root.appendChild(createLinString(doc, "1", latLng.toString()+" "));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transf = transformerFactory.newTransformer();

        transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transf.setOutputProperty(OutputKeys.INDENT, "yes");
        transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);

        File myFile = new File("src/main/res/raw/router.xml");

        StreamResult console = new StreamResult(System.out);
        StreamResult file = new StreamResult(myFile);

        transf.transform(source, console);
        transf.transform(source, file);
    }
    private static Node createLinString(Document doc, String id, String latlngAll) {
        Element user = doc.createElement("LineString");

        user.appendChild(createCdElement(doc, "coordinates", latlngAll));
        return user;
    }

    private static Node createCdElement(Document doc, String name,
                                          String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));

        return node;
    }
    private double distanceBetween(double lat1, double long1,
                         double lat2, double long2)   {
        double R = 6371;
        double dlat = (lat2 - lat1)*Math.PI / 180;
        double dlong = (long2 - long1)*Math.PI / 180;
        double aDouble = Math.sin(dlat/2)*Math.sin(dlong/2);

        return 0;
    }
}