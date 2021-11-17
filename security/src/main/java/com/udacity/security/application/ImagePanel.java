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

package com.udacity.security.application;


import com.udacity.security.data.AlarmStatus;
import com.udacity.security.service.SecurityService;
import com.udacity.security.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Panel containing the 'camera' output. Allows users to 'refresh' the camera
 * by uploading their own picture, and 'scan' the picture, sending it for image analysis
 */
public class ImagePanel extends JPanel implements StatusListener {
    private SecurityService securityService;

    private JLabel cameraHeader;
    private JLabel cameraLabel;
    private BufferedImage currentCameraImage;

    private int IMAGE_WIDTH = 300;
    private int IMAGE_HEIGHT = 225;

    public ImagePanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());
        this.securityService = securityService;
        securityService.addStatusListener(this);

        cameraHeader = new JLabel("Camera Feed");
        cameraHeader.setFont(StyleService.HEADING_FONT);

        cameraLabel = new JLabel();
        cameraLabel.setBackground(Color.WHITE);
        cameraLabel.setPreferredSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
        cameraLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        //button allowing users to select a file to be the current camera image
        JButton addPictureButton = new JButton("Refresh Camera");
        addPictureButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Select Picture");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            try {
                currentCameraImage = ImageIO.read(chooser.getSelectedFile());
                Image tmp = new ImageIcon(currentCameraImage).getImage();
                cameraLabel.setIcon(new ImageIcon(tmp.getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_SMOOTH)));
            } catch (IOException | NullPointerException ioe) {
                JOptionPane.showMessageDialog(null, "Invalid image selected.");
            }
            repaint();
        });

        //button that sends the image to the image service
        JButton scanPictureButton = new JButton("Scan Picture");
        scanPictureButton.addActionListener(e -> {
            securityService.processImage(currentCameraImage);
        });

        add(cameraHeader, "span 3, wrap");
        add(cameraLabel, "span 3, wrap");
        add(addPictureButton);
        add(scanPictureButton);
    }

    @Override
    public void notify(AlarmStatus status) {
        //no behavior necessary
    }

    @Override
    public void catDetected(boolean catDetected) {
        if (catDetected) {
            cameraHeader.setText("DANGER - CAT DETECTED");
        } else {
            cameraHeader.setText("Camera Feed - No Cats Detected");
        }
    }

    @Override
    public void sensorStatusChanged() {
        //no behavior necessary
    }
}
