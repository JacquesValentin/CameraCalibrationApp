# CameraCalibrationApp

## Setup project

### Add OpenCv dependencies
    In order for the app to work you need to add OpenCV dependencies.
        
        - clone the openCv library from https://github.com/BlueFrogRobotics/opencv453_Android_SDK.git 

        - in Android-Studio go to File > New > Import module

        - select the folder containing the openCv library you cloned before

        - click Finish

        - once the module is imported and gradle synced, go to File > Project Structure... > Dependencies > <All Modules> 

        - to add the opencv dependencie to the project click on + (the one in the window with all the dependencies) > 3 Module Dependency > app > OK

        - select the openCv module you just imported and click OK

        - click Apply an OK to apply the changes


## Use the App

    - First you have to specify the parameter of the calibration pattern, which should be a chessboard, in the respective fields
        - Square size : size of the squares int the chessboard
        - horizontal corners : number of (internals) horizontal corners in the chessboard
        - vertical corners : number of (internal) vertical corners in the chessboard
    
    - then you can click on 'SAVE SETTINGS' to save those settings
    
    - now you can start to gather the images for the calibration by clicking on the 'gather images' switch

    - click again when you're done gathering images

    - Finally you can click on 'CALIBRATE' to compute the camera matrix and distortion vector (it take some time if you gathered a lot of images)

    - Once it's done it should print the result of the calibration in the 'calibration result' region at the bottom left.

## TODO

    - add button to specify the camera to calibrate (for now it's the camera with camera_id=1)

    - create a new Activity to show results of calibration
    
    - clean the code
    


    
