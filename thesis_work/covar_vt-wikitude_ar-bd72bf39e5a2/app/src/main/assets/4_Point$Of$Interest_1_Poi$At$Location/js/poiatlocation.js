// implementation of AR-Experience (aka "World")
var World = {
	// true once data was fetched
	initiallyLoadedData: false,
	radarLoaded: false,

	// POI-Marker asset
	markerDrawable_idle1: null,
	markerDrawable_idle2: null,
	markerDrawable_ap: null,
	markerDrawable_directionIndicator: null,

	//angle to rotate the model towards the user
	targetAngle: null,
	markerList: [],
	ap_id: [],
	ang_ctr : 0,


	/*

	createJsonFromAndroid: function createJsonFromAndroidFn(){

	}
	*/

	// called to inject new POI data
	loadPoisFromJsonData: function loadPoisFromJsonDataFn(poiData) {

        //var poinData = []

        //World.markerList = [];

        while(World.markerList.length){
            markerList.pop();
        }

	    World.ap_id.push(poiData.id)
		/*
			The example Image Recognition already explained how images are loaded and displayed in the augmented reality view. This sample loads an AR.ImageResource when the World variable was defined. It will be reused for each marker that we will create afterwards.
		*/
		World.markerDrawable_ap = new AR.ImageResource("assets/AccessPoint.png");
		//create new 3d model to display at a geolocation


        /*
		if(poiData.id == "frontDoor")
		{
         var modelC = new AR.Model("assets/car.wt3",{

            scale: {
                        x:1,
                        y:1,
                        z:1
                    }
          });
          }
          else{

          var modelC = new AR.Model("assets/cyl1.wt3",{

                      scale: {
                          x:1,
                          y:1,
                          z:1
                      }
                  });
                  }
         */

		/*
		// Create an AR.ImageResource referencing the image that should be displayed for a direction indicator.
        World.markerDrawable_directionIndicator = new AR.ImageResource("assets/indi.png");

		var directionIndicatorDrawable = new AR.ImageDrawable(World.markerDrawable_directionIndicator, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP});
            */

        if(poiData.latitude!=""){
            var markerLocation = new AR.GeoLocation(parseFloat(poiData.latitude), parseFloat(poiData.longitude), parseFloat(poiData.altitude));
            var angle_rat;

            var markerImageDrawable_idle = new AR.ImageDrawable(World.markerDrawable_ap, 1.0, {
                zOrder: 0,
                opacity: .75
             });

        }



        //add radar objects to display on the radar
         var radarCircle = new AR.Circle(0.05, {
            horizontalAnchor: AR.CONST.HORIZONTAL_ANCHOR.CENTER,
            opacity: 0.8,
            style: {fillColor: "#ffffff"}
          });

          //alert(World.targetAngle);
            /*
          var modelC = new AR.Model("assets/hose.wt3",{
            scale: {
                x:1,
                y:1,
                z:2.5
                },



            rotate: {

                heading: 45

                }


          });
          */


                    // create GeoObject
          var markerObject = new AR.GeoObject(markerLocation, {
            drawables: {
                cam:    [markerImageDrawable_idle],//, modelC],
                        //indicator: [directionIndicatorDrawable],
                    	radar: radarCircle
                }
            });

         World.markerList.push(markerObject);

          // Updates status message as a user feedback that everything was loaded properly.
         World.updateStatusMessage('Successful Load');

         World.apConnection(markerLocation.latitude);

            /*

            var markerObject = new AR.GeoObject(World.markerLocation, {
                        drawables: {
                            cam:    modelC
                                    //indicator: [directionIndicatorDrawable],
                                	//radar: radarCircle
                            }
                        });
                        */

        /*
        //create new 3d model to display at a geolocation
        var modelCar = new AR.Model("assets/car.wt3",{

            scale: {
                x:1,
                y:1,
                z:4
            }
        });
        */

		/*
			For creating the marker a new object AR.GeoObject will be created at the specified geolocation. An AR.GeoObject connects one or more AR.GeoLocations with multiple AR.Drawables. The AR.Drawables can be defined for multiple targets. A target can be the camera, the radar or a direction indicator. Both the radar and direction indicators will be covered in more detail in later examples.
		*/

		/*
		for (var i = 0; i < poiData.length; i++){
		    var markerLocation = new AR.GeoLocation(poiData[i].latitude, poiData[i].longitude, poiData[i].altitude);
		    if(i > 0){

		        var markerImageDrawable_idle = new AR.ImageDrawable(World.markerDrawable_ap, 1.0, {
                            			zOrder: 0,
                            			opacity: .75
                            		});


		    } else{

		        var markerImageDrawable_idle = new AR.ImageDrawable(World.markerDrawable_ap, 1.0, {
                            			zOrder: 0,
                            			opacity: .75
                            		});
                    alert("alert3: imageDrawable created")


                var threeD_Obj = new AR.GeoObject(markerLocation, {

                    drawables: {

                        cam: [modelCar]

                    }


                });


                markerlist.push(threeD_Obj);

		    }

            markerlist.push(markerObject);

		}
        */

		//make new relative location for testing
		//var rel_loc = new AR.RelativeLocation(null,5,0,2);

        /*
		var titleLabel = new AR.Label("Mac Add!", 1, {
                zOrder: 1,
                offsetY: 0.55,
                style: {
                    textColor: '#FFFFFF',
                    fontStyle: AR.CONST.FONT_STYLE.BOLD
                }
            });
         */



		//World.markerObject.directionIndicatorDrawable.enabled = true;
	},

	// updates status message shown in small "i"-button aligned bottom center
	updateStatusMessage: function updateStatusMessageFn(message, isWarning) {

		var themeToUse = isWarning ? "e" : "c";
		var iconToUse = isWarning ? "alert" : "info";

		$("#status-message").html(message);
		$("#popupInfoButton").buttonMarkup({
			theme: themeToUse
		});
		$("#popupInfoButton").buttonMarkup({
			icon: iconToUse
		});
	},

	// location updates, fired every time you call architectView.setLocation() in native environment
	locationChanged: function locationChangedFn(lat, lon, alt, acc) {
		/*
			The custom function World.onLocationChanged checks with the flag World.initiallyLoadedData if the function was already called. With the first call of World.onLocationChanged an object that contains geo information will be created which will be later used to create a marker using the World.loadPoisFromJsonData function.
		*/

		//show Radar
		if(!World.radarLoaded){
		 	PoiRadar.show();
		 	World.radarLoaded = true;
		 }
		PoiRadar.updatePosition();


		World.apConnection(lat);

	},



	apConnection: function apConnectionFn(lat){

	    World.ang_ctr++;

	    if(World.markerList != null){

	        for(var i = 0; i < World.markerList.length; i++){



                if(World.ap_id[i]!=null){
                    var dist = World.markerList[i].locations[0].distanceToUser();


                    var latDif = Math.abs(World.markerList[i].locations[0].latitude - lat)*111120;
                    var radians = Math.asin(dist/latDif);
                    var pi = Math.PI;
                    World.targetAngle = 90+(1*(Math.asin(dist/latDif)*(180/Math.PI)));



                    //alert(World.targetAngle);

                    var modelC = new AR.Model("assets/hose.wt3",{
                        scale: {
                            x:1,
                            y:1,
                            z:1
                        },



                        rotate: {

                            heading: 45

                        }


                     });

                    var markerObject = new AR.GeoObject(World.markerList[i].locations[0], {
                        drawables: {
                            cam:    modelC
                            //indicator: [directionIndicatorDrawable],
                            //radar: radarCircle
                        }
                     });
                        /*
                     if(World.ang_ctr % 12 == 0) {

                     		    alert(latDif+ " radians: "+radians);

                     		}
                    */
                }
                else{}
            }

	    }

	}

};

/* 
	Set a custom function where location changes are forwarded to. There is also a possibility to set AR.context.onLocationChanged to null. In this case the function will not be called anymore and no further location updates will be received. 
*/

AR.context.onLocationChanged = World.locationChanged;



//Deprecated code (just in case)----------------------------------------------------------------------------------------------------------------------

/*


		//World.initiallyLoadedData = true;

		if (!World.initiallyLoadedData) {



            AR.hardware.sensors.enabled = true;
			// creates a poi object with a random location near the user's location. make it a
			//collection to push two poi objects
			var poiData = [];

			poiData.push({
				"id": 1,
				"longitude": -80.41922858,//(lon + (Math.random() / 5 - 0.1)), //37.229237,//37.22927248983065, //
				"latitude": 37.22929400,//(lat + (Math.random() / 5 - 0.1)), //-80.419075,//-80.41921720419147,//
				"altitude": 0,
				//"mac": router
			});



			poiData.push({
            	"id": 2,
            	"longitude": -80.41911288,//(lon + (Math.random() / 5 - 0.1)), //37.229237,//37.22927248983065, //
            	"latitude": 37.22917532,//(lat + (Math.random() / 5 - 0.1)), //-80.419075,//-80.41921720419147,//
            	"altitude": 0,
            	//"mac": router
            });


			//alert(poiData.latitude+ " "+poiData.longitude)

			World.loadPoisFromJsonData(poiData);


			World.initiallyLoadedData = true;

		}
		*/
