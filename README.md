Magnetic Scale Speedometer
===========================

**Scale speedometer using an Android phone and two magnets**

Concept based on a suggestion by *Brendan Schyve*

This is an experimental app that uses the uses the phone's magnetometer sensor to calculate scale speed.

You just need:
* the app, and 
* two similar magnets stuck on a carriage or carriages at a fixed distance apart. 
 
The app reads the passing of the magnets and calculates the speed.

It is in a very early state at the moment and may yet prove impractical or unworkable.

Note that the magnets in the loco's motor are likely to trigger the sensor, so there is an option to ignore the first reading (the loco).

Setup
-----

**Magnets**

* It is important that the two magnets are similar in size and magnetic properties.  
* They must be placed a similar distance to the left/right 'edge' of each carriage.  i.e. they will pass the phone at a similar distance.
* The distance between them does not matter, other than if they are too close the app may not recognise that the passing of the first magnet has ended.
* Similarly, they should not be too close to the loco.

Screen
------

* **Scale** - the Model train scale to be used for the speed calculation
* **Distance** - Distance between the two magnets in centimeters. This must be measured from leading edge to leading edge (or middle to middle). 
* **Threshold** - the minimum level which the app will think it sees the magnet.  It will then continue measuring till the value reaches a peak and then starts to drop.  Adjust this as needed to ignore background 'noise'
* ** Axis** - X, Y or Z.  Which magnetometer sensor axis to use to measure the speed.  Change as need to pick the axis that is giving the best responses.  This will depend on where the sensor is in the phone, and how the phone is oriented to the track.
* **Ignore first response** - The magnets in the loco's motor are likely to trigger the sensor. This option allows you to ignore the first reading (the loco).
* **Reset** - click this to clear the current reading and start a new one