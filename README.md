Magnetic Scale Speedometer
===========================

**Scale speedometer using an Android phone and two magnets**

Concept based on a suggestion by *Brendan S.*

This is an experimental app that uses the uses the phone's magnetometer sensor to calculate scale speed.

You just need:
* the app, and 
* two similar magnets stuck on a carriage or carriages at a fixed distance apart. 
 
The app reads the passing of the magnets and calculates the speed.

Note that the magnets in the loco's motor are likely to trigger the sensor, so there is an option to ignore the first reading (the loco).

Setup
-----

**Magnets**

* It is important that the two magnets are 'similar' in size and magnetic properties.  (They don't have to absolutely identical.)
* The polarity of the magnets does not matter
* They should be placed a 'similar' distance to the left/right 'edge' of each carriage.  i.e. So they will pass the phone at a similar distance.
* The distance between the magnets does not matter, other than if they are too close the app may not recognise that the passing of the first magnet has ended.
* Similarly, the carriage carrying the magnets should not be too close to the loco.

Screen
------

* **Scale** - The model train scale to be used for the speed calculation
* **Distance** - Distance between the two magnets in centimeters. This must be measured from leading edge to leading edge (or middle to middle). 
* **Threshold** - The minimum amount, above or below, the ambient level which the app will think it sees the magnet.  It will then continue measuring till the value reaches a peak and then starts to drop.  Adjust this as needed to ignore background 'noise'.
* **Axis** - X, Y or Z.  Which magnetometer sensor axis to use to measure the speed.  Change as needed to pick the axis that is giving the best responses.  This will depend on where the sensor is in the phone, and how the phone is oriented to the track.
* **Ignore first response** - The magnets in the loco's motor are likely to trigger the sensor. This option allows you to ignore the first reading (the loco).
* **Restart (sec)** - Time in seconds. If this is set at anything above zero, the values will automatically clear X seconds after the last successful reading.  Note that This does not reset the ambient values. 
* **Reset** - Click this to clear both the current ambient values, and the current speed reading, and start a new pass.

Usage
-----

As the magnets pass the phone what you are wanting to see is the sensor readings go smoothly up then down. (Or down then up depending on the polarity of either or both the magnets.)
The indicator beside the axis will start blank, then go to ▲ as the magnet approaches, then as it starts to pass will go to ▲▼, then finally blank again.  (or ▼ then ▲▼ if the polarity is reversed.)
If the indicator flickers it means the magnetic field is probably too weak for a clean read. Try moving the phone closer, or try a stronger magnet (or two magnets together)

Change Log
----------

See [Change Log](change_log.md).

Copyright
---------

Copyright © 2025 Peter Akers ``akersp62 @ gmail.com``   *(remove the spaces)*

This app is free; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

This app is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the [GNU General Public License](copying.md) along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA