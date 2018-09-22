package nweave.com.uberclient.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TrafficModel;
import com.google.maps.model.TravelMode;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nweave.com.uberclient.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    @BindView(R.id.rootFrame)
    FrameLayout rootFrame;

    @BindView(R.id.rootll)
    LinearLayout rootll;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.rlwhere)
    RelativeLayout rlWhere;

    @BindView(R.id.ivHome)
    ImageView ivHome;

    @BindView(R.id.tvWhere)
    TextView tvWhereto;

    ArgbEvaluator argbEvaluator;


    @BindView(R.id.lt_book_ride)
    LinearLayout bookLayout;
    private LatLng destinationLocation;
    private LatLng pickupLocation;



    @BindView(R.id.tv_pickup_location_text)
    TextView tv_pickup_location_text;

    @BindView(R.id.tv_dropoff_location_text)
    TextView tv_dropoff_location_text;


    private DirectionsResult mDirectionsResult = null;



//    private LatLng destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);

        argbEvaluator = new ArgbEvaluator();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int devHeight = displayMetrics.heightPixels;
        int devWidth = displayMetrics.widthPixels;

        setUpPagerAdapter();
        viewPager.setClipToPadding(false);
        viewPager.setPageMargin(-devWidth / 2);

        viewPager.addOnPageChangeListener(pageChangeListener);
        viewPager.setPageTransformer(true, pageTransformer);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                tvWhereto.setText(place.getAddress());
                destination = place.getLatLng();
                setUpPolyLine();

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Toast.makeText(this, "Error " + status, Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    ViewPager.PageTransformer pageTransformer = new ViewPager.PageTransformer() {
        @Override
        public void transformPage(View page, float position) {


            if (position < -1) { // [-Infinity,-1)


            } else if (position <= 1) { // [-1,1]

                if (position >= -1 && position < 0) {

                    LinearLayout uberEco = (LinearLayout) page.findViewById(R.id.lluberEconomy);
                    TextView uberEcoTv = (TextView) page.findViewById(R.id.tvuberEconomy);

                    if (uberEco != null && uberEcoTv != null) {

                        uberEcoTv.setTextColor((Integer) argbEvaluator.evaluate(-2 * position, getResources().getColor(R.color.primary_text)
                                , getResources().getColor(R.color.secondary_text)));

                        uberEcoTv.setTextSize(16 + 4 * position);
                        uberEco.setX((page.getWidth() * position));

                    }

                } else if (position >= 0 && position <= 1) {

                    TextView uberPreTv = (TextView) page.findViewById(R.id.tvuberPre);
                    LinearLayout uberPre = (LinearLayout) page.findViewById(R.id.llUberPre);

                    if (uberPreTv != null && uberPre != null) {

                        uberPreTv.setTextColor((Integer) new ArgbEvaluator().evaluate((1 - position), getResources().getColor(R.color.primary_text)
                                , getResources().getColor(R.color.secondary_text)));

                        uberPreTv.setTextSize(12 + 4 * (1 - position));
                        uberPre.setX(uberPre.getLeft() + (page.getWidth() * (position)));


                    }


                }

            }
        }
    };


    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


        }

        @Override
        public void onPageSelected(int position) {
            Toast.makeText(getApplicationContext(), "You selected .... " , Toast.LENGTH_LONG ).show();
            TransitionManager.beginDelayedTransition(rootFrame);
            bookLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.INVISIBLE);
            ivHome.setVisibility(View.INVISIBLE);
            rlWhere.setVisibility(View.INVISIBLE);



            //Get and set data about Distance & Time required between source --> destination
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedDistance = df.format(getDistanceInMiles());

            ((TextView) bookLayout.findViewById(R.id.tv_distance)).setText(formattedDistance + " miles");
            ((TextView) bookLayout.findViewById(R.id.tv_time)).setText(getTimeRequired());

            if(getDistanceInMiles() > 0) {
                float amount = getDistanceInMiles() * 1; //distance * rate/perMile
                String formattedAmount = df.format(amount);

                ((TextView) bookLayout.findViewById(R.id.tv_amount)).setText(String.format(getString(R.string.amount), formattedAmount));
            } else {
                ((TextView) bookLayout.findViewById(R.id.tv_amount)).setText("");
            }


            tv_pickup_location_text.setText("Current Location");
            tv_dropoff_location_text.setText(tvWhereto.getText());
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @OnClick(R.id.ivHome)
    void showViewPagerWithTransition() {

        TransitionManager.beginDelayedTransition(rootFrame);
        viewPager.setVisibility(View.VISIBLE);
        ivHome.setVisibility(View.INVISIBLE);
        rlWhere.setVisibility(View.INVISIBLE);
        bookLayout.setVisibility(View.INVISIBLE);
        mMap.setPadding(0, 0, 0, viewPager.getHeight());

    }

    @OnClick(R.id.rlwhere)
    void openPlacesView() {
        openPlaceAutoCompleteView();
    }


    void startRevealAnimation() {

        int cx = rootFrame.getMeasuredWidth() / 2;
        int cy = rootFrame.getMeasuredHeight() / 2;
        Animator anim =
                ViewAnimationUtils.createCircularReveal(rootll, cx, cy, 50, rootFrame.getWidth());

        anim.setDuration(500);
        anim.setInterpolator(new AccelerateInterpolator(2));
        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bookLayout.setVisibility(View.INVISIBLE);
                rlWhere.setVisibility(View.VISIBLE);
                ivHome.setVisibility(View.VISIBLE);
            }
        });

        anim.start();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        rootFrame.post(new Runnable() {
            @Override
            public void run() {
                //Do your animation work here.
                startRevealAnimation();
            }
        });




    }

    private GeoApiContext getGeoApiContext() {
        GeoApiContext.Builder builder = new GeoApiContext.Builder();
        builder.apiKey(getString(R.string.google_maps_key));
        builder.queryRateLimit(3);
        return builder.build();
    }
    

    //https://android.jlelse.eu/google-maps-directions-api-5b2e11dee9b0
    private void getDirectionsApiResult() {
        DateTime now = new DateTime();
        try {

            mDirectionsResult = DirectionsApi.newRequest(getGeoApiContext())
                    .mode(TravelMode.DRIVING)
                    .origin(pickupLocation)
                    .destination(destinationLocation)
                    .departureTime(now)
                    .trafficModel(TrafficModel.BEST_GUESS)
                    .await();

            try {
                Log.d("", "mDirectionsResult  " + (new JSONObject(new Gson().toJson(mDirectionsResult)).toString(3)));
            } catch(Exception e) {
                Log.e("", "mDirectionsResult  ", e);
            }

        } catch (ApiException e) {
            Log.e("", "ApiException" ,e);
        } catch (InterruptedException e) {
            Log.e("", "InterruptedException" ,e);
        } catch (IOException e) {
            Log.e("", "IOException" ,e);
        } catch (Exception e) {
            Log.e("", "Exception" ,e);
        }
    }

    private float getDistanceInMiles() {
        try {
            if (mDirectionsResult != null) {
                long meters = mDirectionsResult.routes[0].legs[0].distance.inMeters;
                double inches = (39.370078 * meters);
                float miles = (float) (inches / 63360);
                return miles;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e("", "getDistanceRequired : " + e.getMessage());
        }
        return -1;
    }

    private String getTimeRequired() {
        //Log.d("","getTimeRequired  " +mDirectionsResult);
        try {
            if(mDirectionsResult != null) {
                return mDirectionsResult.routes[0].legs[0].duration.humanReadable;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e("", "getTimeRequired : " + e.getMessage());
        }
        return null;
    }

    private long getTimeInSecs() {
        //Log.d("","getTimeRequired  " +mDirectionsResult);
        try {
            if(mDirectionsResult != null) {
                return mDirectionsResult.routes[0].legs[0].duration.inSeconds;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e("", "getTimeRequired : " + e.getMessage());
        }
        return -1;
    }

    @Override
    protected void setUpPolyLine() {

        pickupLocation = new LatLng(getUserLocation().getLatitude(), getUserLocation().getLongitude());
        destinationLocation = new LatLng(getDestinationLatLong().latitude, getDestinationLatLong().longitude); ;
        if (pickupLocation != null &&   destinationLocation != null) {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://maps.googleapis.com/maps/api/directions/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            getPolyline polyline = retrofit.create(getPolyline.class);

            polyline.getPolylineData(pickupLocation.lat + "," + pickupLocation.lng, destinationLocation.lat + "," + destinationLocation.lng)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {

                            JsonObject gson = new JsonParser().parse(response.body().toString()).getAsJsonObject();
                            try {

                                Single.just(parse(new JSONObject(gson.toString())))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<List<List<HashMap<String, String>>>>() {
                                            @Override
                                            public void accept(List<List<HashMap<String, String>>> lists) throws Exception {

                                                try{
                                                    drawPolyline(lists);
                                                    getDirectionsApiResult();
                                                } catch (Exception e){
                                                    Log.d("", e.getMessage());
                                                }

                                            }
                                        });

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, Throwable t) {

                        }
                    });
        } else
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    private void setUpPagerAdapter() {

        List<Integer> data = Arrays.asList(0, 1);
        CarsPagerAdapter adapter = new CarsPagerAdapter(data);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {

        if (viewPager.getVisibility() == View.VISIBLE) {

            TransitionManager.beginDelayedTransition(rootFrame);
            viewPager.setVisibility(View.INVISIBLE);
            mMap.setPadding(0, 0, 0, 0);
            ivHome.setVisibility(View.VISIBLE);
            rlWhere.setVisibility(View.VISIBLE);
            return;
        }


        if (bookLayout.getVisibility() == View.VISIBLE) {

            TransitionManager.beginDelayedTransition(rootFrame);
            bookLayout.setVisibility(View.INVISIBLE);
            mMap.setPadding(0, 0, 0, 0);
            ivHome.setVisibility(View.VISIBLE);
            rlWhere.setVisibility(View.VISIBLE);
            return;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        super.onBackPressed();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.pickup_location) {
            // Handle the camera action
        } else if (id == R.id.history) {

        } else if (id == R.id.user_profile) {


        } else if (id == R.id.nav_share) {
            // referral link
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
