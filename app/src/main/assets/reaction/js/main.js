var state = 0;
var start_time = 0;
var light_time = 0;

// Util functions to simulate Arduino

function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function random(start, end) {
	var len = end - start;
	var ret = Math.floor(Math.random() * len) + start;
	return ret;
}

function millis() {
	var time = new Date();
	var millis = time.getMilliseconds() + time.getSeconds() * 1000;
	return millis;
}
// Our display using the 6 digit display library
var display = new SegmentDisplay('display');
display.pattern = '####';
display.segmentCount = SegmentDisplay.SevenSegment
display.cornerType = SegmentDisplay.RoundedCorner;
display.digitHeight = 20;
display.digitWidth = 11;
display.digitDistance = 4;
display.displayAngle = 6;
display.segmentWidth = 2;
display.segmentDistance = 0.4;
display.colorOn = 'rgb(134, 215, 255)';
display.colorOff = 'rgb(70, 71, 69)';
display.setValue("0");

var current_timeout = null;
var redbutton = null;
var on_click_old = null;

function on_keydown(e) {
	redbutton.click();
}

document.addEventListener("keydown", on_keydown, false);

function show_score() {
	clearTimeout(current_timeout);
	current_timeout = setTimeout('clear()', 5000);
	//document.getElementById("light").className = "";
}

function pulsing() {
	clearTimeout(current_timeout);
	document.getElementById("light").className = "pulsing";
  show_score();
}

function blinking() {
	clearTimeout(current_timeout);
	current_timeout = setTimeout('pulsing()', 500);
	document.getElementById("light").className = "blinking";	
}

function next_event(title, display_text, on_click) {
	console.log(title);
	clearTimeout(current_timeout);
	if (display_text != null) display.setValue(display_text);
  redbutton.removeEventListener('click', on_click_old);
	redbutton.addEventListener('click', on_click);
	
	on_click_old = on_click;
}

function clear() {
	next_event("--- CLEAR --- ", "----", on_click_start);
	document.getElementById("light").className = "pulsing";
}

function on_click_failed() {
	next_event("--- FAILED --- ", "nope", on_click_start);
	blinking();
}

function on_click_finished() {
	next_event("--- FINISHED --- ", null, on_click_start);
	blinking();
}

function set_red_color() {
	next_event("--- SET RED --- ", "0000", on_click_finished);
	start_time = millis();
	document.getElementById("light").className = "on";
	countdown();
}

function countdown() {
	var time = millis() - start_time;
	if (time > 5000 || time < 0) {
		on_click_failed();
		return;
	}
	var value = pad(time, 4);
	display.setValue(value);
	clearTimeout(current_timeout);
	current_timeout = setTimeout('countdown()', 10);
}

function on_click_start() {
	next_event("--- ON START --- ", "0000", on_click_failed);
	light_time = random(4000, 6000); // get random number of microseconds - from 2 to 6 seconds
	console.log(" Light time " + light_time);
	current_timeout = setTimeout(set_red_color, light_time);
	document.getElementById("light").className = "";
}

function init() {
	display.setValue("H1YA");
  redbutton = document.getElementsByClassName("redbutton")[0];
  redbutton.addEventListener('click', on_click_start);
  on_click_old = on_click_start;
  document.getElementById("light").className = "pulsing";
}
document.addEventListener('DOMContentLoaded', init);
