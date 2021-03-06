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

package com.dp16.runamicghent.DataProviderTests;

import android.location.Location;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.SpeedProvider;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.runamicghent.util.GpxReader;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for DataProvider.SpeedProvider
 * Created by Nick on 30-3-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SpeedProviderTests {
    private EventBroker broker;
    private SpeedProvider provider;
    private static final String gpxFilename = "plateau_zwijnaarde.gpx";
    private static final double accuracyRequired = 0.2; // 20% as described in the acceptance tests
    private static final int warmupPeriod = 1; // how many locations to send to the filter before reading from it

    /**
     * Util method to make a location from a lat, long and speed
     *
     * @param latitude  in degrees (positive is E, negative is W)
     * @param longitude in degrees ( positive is N, negative is S)
     * @param speed     in m/s
     * @return a Location objects a generated by the android framework with the time set to now and the accuracy to perfect.
     */
    private Location makeLocation(double latitude, double longitude, float speed) {
        Location location = new Location("Mock provider");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(System.currentTimeMillis());
        location.setAccuracy(0);
        location.setSpeed(speed);
        return location;
    }

    @Before
    public void init() {
        broker = EventBroker.getInstance();
        provider = new SpeedProvider();
    }

    @Test
    public void SpeedProvider_startPauseResumeStop_leavesEventBrokerStateUnchanged() {
        int amountListenersBefore = broker.getAmountOfListeners();
        broker.start();
        provider.start();
        int amountListenersAfterStart = broker.getAmountOfListeners();
        assertTrue("SpeedProvider.start() does not register an additional eventlistener", amountListenersAfterStart > amountListenersBefore);
        provider.pause();
        assertTrue("SpeedProvider.pause() does not unregister some eventlisteners", amountListenersAfterStart > broker.getAmountOfListeners());
        provider.resume();
        assertEquals("SpeedProvider.resume() does not register same amount as start()", amountListenersAfterStart, broker.getAmountOfListeners());
        provider.stop();
        assertEquals("SpeedProvider.stop() does not unregister all listeners", amountListenersBefore, broker.getAmountOfListeners());
        broker.stop();
    }

    @Test
    public void SpeedProvider_constantSpeedTrack_returnsSameSpeed() {
        GpxReader reader = new GpxReader(gpxFilename);
        List<Location> locations = reader.getLocations();
        final double speed = 12.0 / 3.6; // this prerecorded track is at 12 km/h

        assertTrue("Error in test definition: warmup period is longer than input length", warmupPeriod < locations.size());

        final AtomicInteger messagesReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                RunSpeed runSpeed = (RunSpeed) message;
                assertEquals("Runspeed reported is not within 20% of the real value", speed, runSpeed.getSpeed(), speed * accuracyRequired);
                messagesReceived.incrementAndGet();
            }
        };

        EventListener warmupListener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceived.incrementAndGet();
            }
        };
        broker.addEventListener(Constants.EventTypes.SPEED, warmupListener);

        provider.start();
        broker.start();

        EventPublisherClass eventPublisher = new EventPublisherClass();

        // do some warmup on the SpeedProvider as it starts from a speed of 0.0
        for (int i = 0; i < warmupPeriod; i++) {
            eventPublisher.publishEvent(Constants.EventTypes.RAW_LOCATION, locations.get(i));
        }

        ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue("Did not a speed report for each location in ", messagesReceived, warmupPeriod);

        // start with the real data and attach the listener that checks for accuracy
        broker.removeEventListener(warmupListener);
        broker.addEventListener(Constants.EventTypes.SPEED, listener);

        for (int i = warmupPeriod; i < locations.size(); i++) {
            eventPublisher.publishEvent(Constants.EventTypes.RAW_LOCATION, locations.get(i));
        }

        ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue("Did not a speed report for each location in ", messagesReceived, locations.size());

        broker.stop();
        provider.stop();
    }

}
