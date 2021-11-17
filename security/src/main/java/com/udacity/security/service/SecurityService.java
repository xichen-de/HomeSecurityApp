/*
 * PROJECT LICENSE
 *
 * This project was submitted by Xi Chen as part of the Nanodegree At Udacity.
 *
 * As part of Udacity Honor code, your submissions must be your own work, hence
 * submitting this project as yours will cause you to break the Udacity Honor Code
 * and the suspension of your account.
 *
 * Me, the author of the project, allow you to check the code as a reference, but if
 * you submit it, it's your own responsibility if you get expelled.
 *
 * Copyright (c) 2021 Xi Chen
 *
 * Besides the above notice, the following license applies and this license notice
 * must be included in all works derived from this project.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.udacity.security.service;


import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * <p>
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     *
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && allSensorsInactive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     *
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleInactiveSensorActivated() {
        ArmingStatus armingStatus = getArmingStatus();
        AlarmStatus alarmStatus = getAlarmStatus();
        if (armingStatus == ArmingStatus.ARMED_AWAY || armingStatus == ArmingStatus.ARMED_HOME) {
            switch (alarmStatus) {
                case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
                case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
            }

        }

    }


    private void handleActiveSensorActivated() {
        AlarmStatus alarmStatus = getAlarmStatus();
        if (alarmStatus == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void resetAllSensors() {
        Set<Sensor> sensors = getSensors().stream().peek(s -> s.setActive(false)).collect(Collectors.toSet());
        sensors.forEach(s -> securityRepository.updateSensor(s));
    }

    private boolean allSensorsInactive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    private void handleAllSensorsDeactivated() {
        AlarmStatus alarmStatus = getAlarmStatus();
        if (alarmStatus == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     *
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // deactivate the sensor if it is active
        if (sensor.getActive() && !active) {
            sensor.setActive(false);
        }
        // activate the sensor if it is inactive
        else if (!sensor.getActive() && active) {
            sensor.setActive(true);
            handleInactiveSensorActivated();
        }
        // deactivate the sensor if it is inactive
        else if (!sensor.getActive() && !active) {
        }
        // activate the sensor if it is active
        else {
            handleActiveSensorActivated();
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        if (allSensorsInactive()) {
            handleAllSensorsDeactivated();
        }
    }


    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     *
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     *
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     *
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        if (armingStatus == ArmingStatus.ARMED_HOME || armingStatus == ArmingStatus.ARMED_AWAY) {
            resetAllSensors();
        }
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }
}
