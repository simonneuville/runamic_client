/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent.statTrackerTests;

import android.location.Location;
import android.util.Log;

import com.dp16.runamicghent.Activities.RunningScreen.RunningActivity;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.StatTracker.RouteEngine;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by hendrikdepauw on 12/04/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RouteEngineTests {
    private EventBroker broker;
    private RouteEngine underTest;
    private RunRoute runRoute;
    private static final long waitingTime = 100; // ms

    @Before
    public void init(){
        try {
            String json = new String("{\"coordinates\": [ { \"lat\": 51.0386722, \"c\": \"none\", \"lon\": 3.730139 }, { \"lat\": 51.0386317, \"c\": \"left\", \"lon\": 3.7301503 }, { \"lat\": 51.038596, \"c\": \"none\", \"lon\": 3.7301377 } ] }");
            runRoute = new RunRoute(new JSONObject(json));
        } catch (JSONException e) {
            Log.e("RunRouteTests", e.getMessage());
        }

        broker = EventBroker.getInstance();
        underTest = new RouteEngine(Robolectric.buildActivity(RunningActivity.class).create().get());
    }

    /**
     * Util method to make a location from a lat, long coordinate
     *
     * @param latitude  in degrees (positive is E, negative is W)
     * @param longitude in degrees ( positive is N, negative is S)
     * @return a Location objects a generated by the android framework with the time set to now and the accuracy to perfect.
     */
    private Location makeLocation(double latitude, double longitude) {
        Location location = new Location("Mock provider");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(System.currentTimeMillis());
        location.setAccuracy(0);
        return location;
    }

    @Ignore("Did not have time to fix")
    @Test
    public void routeEngine_publishEvent_locationInRadius(){
        final AtomicInteger received = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, listener);
        underTest.start();
        broker.start();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386318, 3.7301504));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            // no actions needed
        }

        broker.stop();
        underTest.stop();

        assertEquals("RouteEngine did not publish Navigation Direction Event", 1, received.get());
    }

    @Ignore("Did not have time to fix")
    @Test
    public void routeEngine_publishEvent_locationInRadiusTwice(){
        final AtomicInteger received = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, listener);
        underTest.start();
        broker.start();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386318, 3.7301504));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            // no actions needed
        }
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386316, 3.7301502));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            // no actions needed
        }

        broker.stop();
        underTest.stop();

        assertEquals("RouteEngine published the same Navigation Direction event twice", 1, received.get());
    }

    @Ignore("Did not have time to fix")
    @Test
    public void routeEngine_publishEvent_locationNotInRadius(){
        final AtomicInteger received = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, listener);
        underTest.start();
        broker.start();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(0, 0));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            // no actions needed
        }

        broker.stop();
        underTest.stop();

        assertEquals("RouteEngine did publish Navigation Direction Event while not in radius", 0, received.get());
    }

}