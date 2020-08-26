var PoiRadar = {

        show: function initFn(){
        //add radar screen as an image then add to the top left corner of the screen
	    AR.radar.background = new AR.ImageResource("assets/radar_bg.png");
	    AR.radar.positionX = 0.05;
	    AR.radar.positionY = 0.05;
	    //set radar width to 20% of the screen
	    AR.radar.width = 0.2;

	    //set properties of the radar against the image
	    AR.radar.centerX = 0.5;
	    AR.radar.centerY = 0.5;
	    AR.radar.radius = 0.3;

	    //add north arrow
	    AR.radar.northIndicator.image = new AR.ImageResource("assets/radar_north.png");
	    AR.radar.northIndicator.radius = 0.0

	    AR.radar.enabled = true;
	},

	updatePosition: function updatePositionFn() {
		if (AR.radar.enabled) {
			AR.radar.notifyUpdateRadarPosition();
			}
	},


}
