package com.example.adithyaajith.osmnavigation_v11;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.traffic.TrafficPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.view.View;
import android.widget.Button;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.services.Constants.PRECISION_6;

public class MainActivity extends AppCompatActivity implements LocationEngineListener,PermissionsListener {

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location lastLocation;

    private TrafficPlugin trafficPlugin;

    private static final String TAG = "DirectionsActivity";
    private DirectionsRoute currentRoute;
    private MapboxDirections client;


    private MapboxMap mapboxMap;
    private MapView mapView;

    private Button button;
    private Position origin;
    private Position destination;

    private com.google.android.gms.maps.model.LatLng dest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mapbox Access token
        Mapbox.getInstance(this, getString(R.string.mapbox_key));

        setContentView(R.layout.activity_main);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                mapboxMap.clear();

                // Set the origin waypoint to the devices location
                origin = Position.fromCoordinates(lastLocation.getLongitude(),lastLocation.getLatitude());

                // Set the destination waypoint to the location point long clicked by the user
               // destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());

                destination = Position.fromCoordinates(place.getLatLng().longitude, place.getLatLng().latitude);
                // Add marker to the destination waypoint
                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(place.getLatLng().latitude , place.getLatLng().longitude))
                        .title("Destination Marker")
                        .snippet("My destination"));

                // Get route from API
                getRoute(origin, destination);
                button.setEnabled(true);
                button.setBackgroundResource(R.color.mapboxBlue);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });



//        final Position origin = Position.fromCoordinates(-96.691991,30.538346);
//        final Position destination = Position.fromCoordinates(-96.372954,30.687838);
        button = findViewById(R.id.startButton);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                Log.i("MapAsync", " is called");
                MainActivity.this.mapboxMap = mapboxMap;
                enableLocationPlugin();

                trafficPlugin = new TrafficPlugin(mapView, mapboxMap);
                MainActivity.this.trafficPlugin.setVisibility(true); // Enable the traffic view by default


                // Add origin and destination to the map
//                mapboxMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(origin.getLatitude(), origin.getLongitude()))
//                        .title("Origin")
//                        .snippet("Add your Geocode msg here."));
//                mapboxMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(destination.getLatitude(), destination.getLongitude()))
//                        .title("Destination")
//                        .snippet("Add your Geocode msg here."));

                // Get route from API
//                getRoute(origin, destination);

                mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng point) {

                        //Remove previously added markers
                        //Marker is an annotation that shows an icon image at a geographical location
                        //so all markers can be removed with the removeAllAnnotations() method.
                        mapboxMap.clear();

                        // Set the origin waypoint to the devices location
                        origin = Position.fromCoordinates(lastLocation.getLongitude(),lastLocation.getLatitude());

                        // Set the destination waypoint to the location point long clicked by the user
                        destination = Position.fromCoordinates(point.getLongitude(), point.getLatitude());

                        // Add marker to the destination waypoint
                        mapboxMap.addMarker(new MarkerOptions()
                                .position(new LatLng(point))
                                .title("Destination Marker")
                                .snippet("My destination"));

                        // Get route from API
                        getRoute(origin, destination);
                        button.setEnabled(true);
                        button.setBackgroundResource(R.color.mapboxBlue);

                    }
                });

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /*Position origin = originPosition;
                Position destination = destinationPosition;
*/
                // Pass in your Amazon Polly pool id for speech synthesis using Amazon Polly
                // Set to null to use the default Android speech synthesizer
                String awsPoolId = null;

                boolean simulateRoute = false;

                // Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(MainActivity.this, origin, destination,
                        awsPoolId, simulateRoute);
            }
        });

        findViewById(R.id.traffic_toggle_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    trafficPlugin.setVisibility(!trafficPlugin.isVisible());
                }
            }
        });


        findViewById(R.id.locate_me_toggle_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    initializeLocationEngine();
                }
            }
        });


    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private void getRoute(Position origin, Position destination) {

        client = new MapboxDirections.Builder()
                .setOrigin(origin)
                .setDestination(destination)
                .setOverview(DirectionsCriteria.OVERVIEW_FULL)
                .setProfile(DirectionsCriteria.PROFILE_DRIVING)
                .setAccessToken(Mapbox.getAccessToken())
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                System.out.println(call.request().url().toString());

                // You can get the generic HTTP info about the response
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, "Distance: " + currentRoute.getDistance());
                Toast.makeText(MainActivity.this, String.format(getString(R.string.directions_activity_toast_message),
                        currentRoute.getDistance()), Toast.LENGTH_SHORT).show();

                // Draw the route on the map
                drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(DirectionsRoute route) {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.getGeometry(), PRECISION_6);
        List<Position> coordinates = lineString.getCoordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude());
        }

        // Draw Points on MapView
        mapboxMap.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#32c5ff"))
                .width(4));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);

            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LostLocationEngine(MainActivity.this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
