package nweave.com.uberclient.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;




import nweave.com.uberclient.R;

import static com.google.android.gms.maps.model.JointType.ROUND;

public abstract class BaseActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 858;

    GoogleMap mMap;
    Location userLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    public static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 100;
    protected LatLng destination;
    private List<LatLng> listLatLng = new ArrayList<>();
    private Polyline blackPolyLine, greyPolyLine;


    private int mNoOfCars = 0;
    private RequestQueue mRequestQueue = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    protected void launchActivity(Class activityClass) {
        Intent i = new Intent(getApplicationContext(), activityClass);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    protected boolean isInternetConnected() {
            ConnectivityManager connectivityMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityMgr != null) {
                NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    return true;
                }
            }

        return false;
    }

    protected String getFormattedTime(long millis) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat("hh:mm:a");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMaxZoomPreference(20);

        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(
                this, R.raw.maps_style);
        googleMap.setMapStyle(style);

        if (checkPermission()) onLocationPermissionGranted();
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void onLocationPermissionGranted() {
        if (!checkPermission()) return;

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(true);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {

                            userLocation = location;

                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()))
                                    .zoom(15)
                                    .build();


                            addOverlay(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
                            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            displayCarsNearMyLocation();


                        } else {
                            userLocation = null;
                        }
                    }
                }).addOnCanceledListener(this, new OnCanceledListener() {
            @Override
            public void onCanceled() {
               Toast.makeText(getApplicationContext(), "OnCanceledListener >>> onCanceled ", Toast.LENGTH_LONG).show();
            }
        })
        ;
    }


    private void displayCarsNearMyLocation() {

       // Utils.showProgressDialog(this, false);

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + userLocation.getLatitude() + "," + userLocation.getLongitude()
                + "&radius=500&type=restaurant&key=" +getString(R.string.google_places_key);

//        Log.d(Constants.TAG, "NearBySearch url: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
//                        Utils.hideProgressDialog();
                        try {
                            if(response != null && response.has("results")) {
                                JSONArray resultsArray = response.getJSONArray("results");

                                mNoOfCars = resultsArray.length();

                                if(mNoOfCars > 5) {
                                    mNoOfCars = 5; //Limit to display only 5 cars max
                                }

                                for(int i = 0; i < mNoOfCars; i++) {
                                    JSONObject resultObject = resultsArray.getJSONObject(i);
                                    if(resultObject != null && resultObject.has("geometry")) {
                                        JSONObject locationObject = resultObject.getJSONObject("geometry").getJSONObject("location");
                                        Location location = new Location("");
                                        location.setLatitude(locationObject.getDouble("lat"));
                                        location.setLongitude(locationObject.getDouble("lng"));
                                        addMarker(new LatLng(location.getLatitude(), location.getLongitude()), true);
                                    }
                                }

//                                mLoadingView.setVisibility(View.GONE);
//                                if(mNoOfCars == 0) {
////                                    showSnackBar(getString(R.string.no_cars_available), false);
//                                }
                            }

                        } catch(Exception e) {
                            Log.e("", "NearBySearch Api error", e);
                        }

//                        showMarker(mCurrentLocation,true);//written after displayCars for NoOfCars
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Utils.hideProgressDialog();
                        Log.e("", "NearBySearch Api volley error", error);

//                        showMarker(mCurrentLocation,true);//written after displayCars for NoOfCars
                    }
                });

        // Access the RequestQueue through your singleton class.
        mRequestQueue.add(jsonObjectRequest);
    }


    public void openPlaceAutoCompleteView() {

        mMap.clear();
        this.listLatLng.clear();

        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }

    }

    private boolean checkPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Ask for the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Please give location permission", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public Location getUserLocation() {
        if (userLocation != null)
            return userLocation;

        return null;
    }


    public LatLng getDestinationLatLong() {

        if (destination != null)
            return destination;
        else
            return null;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (checkPermission()){
                onLocationPermissionGranted();
            }else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void addMarker(LatLng destination, boolean isCarType) {

        MarkerOptions options = new MarkerOptions();
        options.position(destination);

        if(isCarType){
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.fake_car));
        }else {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }
        mMap.addMarker(options);

    }

    protected abstract void setUpPolyLine();


    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void addOverlay(LatLng place) {

        GroundOverlay groundOverlay = mMap.addGroundOverlay(new
                GroundOverlayOptions()
                .position(place, 100)
                .transparency(0.5f)
                .zIndex(3)
                .image(BitmapDescriptorFactory.fromBitmap(drawableToBitmap(getDrawable(R.drawable.map_overlay)))));

        startOverlayAnimation(groundOverlay);
    }


    private void startOverlayAnimation(final GroundOverlay groundOverlay) {

        AnimatorSet animatorSet = new AnimatorSet();

        ValueAnimator vAnimator = ValueAnimator.ofInt(0, 100);
        vAnimator.setRepeatCount(ValueAnimator.INFINITE);
        vAnimator.setRepeatMode(ValueAnimator.RESTART);
        vAnimator.setInterpolator(new LinearInterpolator());
        vAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final Integer val = (Integer) valueAnimator.getAnimatedValue();
                groundOverlay.setDimensions(val);

            }
        });

        ValueAnimator tAnimator = ValueAnimator.ofFloat(0, 1);
        tAnimator.setRepeatCount(ValueAnimator.INFINITE);
        tAnimator.setRepeatMode(ValueAnimator.RESTART);
        tAnimator.setInterpolator(new LinearInterpolator());
        tAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Float val = (Float) valueAnimator.getAnimatedValue();
                groundOverlay.setTransparency(val);
            }
        });

        animatorSet.setDuration(3000);
        animatorSet.playTogether(vAnimator, tAnimator);
        animatorSet.start();
    }


    public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

        List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String, String>>();

                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        /** Traversing all points */
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                            hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
        }

        return routes;
    }


    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    void drawPolyline(List<List<HashMap<String, String>>> result) {

        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;

        // Traversing through all the routes
        for (int i = 0; i < result.size(); i++) {
            points = new ArrayList<LatLng>();
            lineOptions = new PolylineOptions();

            // Fetching i-th route
            List<HashMap<String, String>> path = result.get(i);

            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }


            this.listLatLng.addAll(points);
        }

        lineOptions.width(10);
        lineOptions.color(Color.BLACK);
        lineOptions.startCap(new SquareCap());
        lineOptions.endCap(new SquareCap());
        lineOptions.jointType(ROUND);
        blackPolyLine = mMap.addPolyline(lineOptions);

        PolylineOptions greyOptions = new PolylineOptions();
        greyOptions.width(10);
        greyOptions.color(Color.GRAY);
        greyOptions.startCap(new SquareCap());
        greyOptions.endCap(new SquareCap());
        greyOptions.jointType(ROUND);
        greyPolyLine = mMap.addPolyline(greyOptions);




        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng latLng : points){
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

        mMap.animateCamera(cu, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                animatePolyLine();
            }

            @Override
            public void onCancel() {

            }
        });

    }

    private void animatePolyLine() {

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {

                List<LatLng> latLngList = blackPolyLine.getPoints();
                int initialPointSize = latLngList.size();
                int animatedValue = (int) animator.getAnimatedValue();
                int newPoints = (animatedValue * listLatLng.size()) / 100;

                if (initialPointSize < newPoints ) {
                    latLngList.addAll(listLatLng.subList(initialPointSize, newPoints));
                    blackPolyLine.setPoints(latLngList);
                }


            }
        });

        animator.addListener(polyLineAnimationListener);
        animator.start();

    }

    Animator.AnimatorListener polyLineAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

            addMarker(listLatLng.get(listLatLng.size()-1), false);
        }

        @Override
        public void onAnimationEnd(Animator animator) {

            List<LatLng> blackLatLng = blackPolyLine.getPoints();
            List<LatLng> greyLatLng = greyPolyLine.getPoints();

            greyLatLng.clear();
            greyLatLng.addAll(blackLatLng);
            blackLatLng.clear();

            blackPolyLine.setPoints(blackLatLng);
            greyPolyLine.setPoints(greyLatLng);

            blackPolyLine.setZIndex(2);

            animator.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {


        }
    };


}