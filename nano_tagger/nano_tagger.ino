#include <Wire.h>
#include <TimerOne.h>
#include <EEPROM.h>

// Pin definitions
#define ir_led              5
#define rled                6
#define trigger_pin         2
#define recharge_pin        3
#define ammo_indicator_pin  7
#define muzzlefire_pin      8
#define laser_gunsight_pin  9

#define minReceiverID       11
#define maxReceiverID       30

#define expander_address    0b0111000  // address of all expander chips for driving the RGB LEDs

#define button_delay        3 // debounce time for button presses in units of 20ms

#define MUZZLEFIRE_DURATION 18      // Muzzlefire duration in units of 20ms
#define MUZZLEFIRE_BLINK_DURATION 2 // half period of muzzlefire in units of 20ms

// definition of IR default values
#define DEFAULT_DURATION_START 800
#define DEFAULT_DURATION_ONE   200
#define DEFAULT_DURATION_ZERO  500
#define DEFAULT_PULSE_DELAY    500

// IR pulse lengths
#define delayus             6  // pulse length for IR modulation. value of 6 for use with tsop4838 IR Receiver chip
unsigned long duration_zero=500;
unsigned long duration_one=200;
unsigned long duration_start=800;
unsigned long pulse_delay=500;
unsigned long bytes_delay=2000;

// counters for buttons (for debounce and shot delay) and muzzlefire
volatile int trigger_counter=button_delay;
volatile int recharge_counter=button_delay;
volatile int muzzlefire_counter = 0;
volatile int shot_delay=12; // in units of 20ms

// I2C variables
unsigned int bytecount=0;
unsigned char I2Cvalue_high=0;
unsigned char I2Cvalue_low=0;
unsigned char I2CID=0;

// tagger state variables
volatile boolean tagger_enabled=true;
volatile boolean lasergunsight_enabled = false;
volatile boolean muzzlefire_enabled = true;
volatile boolean machinegun_enabled = false;

// blink variables
float blink_frequency=1.0;
unsigned int blink_duration=1000;
boolean blink_until_stopped=false;
unsigned long blink_start_timestamp=0;
unsigned long blink_timestamp=0;
boolean blink_enable=false;
boolean blink_state=false;

// Struct, that holds the expander bytes for each color
const struct COLOR_CODE_STRUCT{ // has to be adapted to wiring of the RGB-LED at the expander
  const byte red=0b10000000;
  const byte green=0b01000000;
  const byte blue=0b00100000;
  const byte yellow=0b11000000;
  const byte violett=0b10100000;
  const byte turkis=0b01100000;
  const byte white=0b11100000;
  const byte black=0b00000000;
}COLOR_CODE;

// Serial variables
String serial_string = "";         // a string to hold incoming data
boolean serial_stringComplete = false;  // whether the string is complete
const char serial_trigger[5]="t\n"; // constants to send, when the corresponding event has occured in the Timer1 interrupt routine (sending in loop)
const char serial_recharge[5]="r\n";
volatile char *serial_write_pt=0; // pointer, that can be set to one of the constants above
String receivers=""; // can hold a string with all IDs of the connected receivers (for sending to smartphone)
char bt_version[20]; // array to hold string with version of the serial bluetooth adapter

// Game variables, that are send over IR
byte team_ID = 0b01; // 2 bit
byte player_ID = 0b00001; // 5 bit, Player 0 reserved for enviroment
byte weapon_type = 0b00; // 2 bit
byte shot_damage = 0b1111; // 4 bit, mapping to actual damage value happens in the app
// struct, that hold the important player stats
struct PLAYER_STATS_STRUCT
{
  unsigned int life_points;
  unsigned int shield_points;
  unsigned int ammo_points;
  unsigned int ammo_packs;
  unsigned int extra_lifes;
}player_stats;

// variable with data, that is send over IR (parity bits are first set in setup, then on every change)
volatile unsigned int shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1);//0b0011111100001001
volatile boolean shot_active=false; // triggers IR shot sending, which happens in loop function and not in Timer1 ISR
String player_color="white"; // current playercolor is hold, to enable blinking to the correct color

// Parity Check function
// Returns 1 (ODD) or 0 (EVEN) parity
int parity (unsigned char x)
{
    x = x ^ x >> 4;
    x = x ^ x >> 2;
    x = x ^ x >> 1;
    return x & 1;
}

void update_shot_parity_bits(){
  // fill shot parity bits for each shot byte (even parity) --> 0 --> even number of 1s,  1--> odd number of 1s
  shot = (shot | parity((shot&0xFF00)>>8)<<8);
  shot = (shot | parity((shot&0x00FF)));
}


void setup() {
  update_shot_parity_bits();
  
  Wire.begin(10);                // join i2c bus with address #10
  Wire.onReceive(I2CreceiveEvent); // register event
  write_color("white");         // set initial RGB LED color to white
  Serial.begin(9600);           // start serial for communication with smartphone
  check_bt_version();           // request version of the serial bluetooth module through AT commands
  
  // configure button pins
  pinMode(trigger_pin,INPUT_PULLUP);
  pinMode(recharge_pin,INPUT_PULLUP);

  // configure outputs
  pinMode(ir_led, OUTPUT);
  pinMode(rled,OUTPUT);
  pinMode(ammo_indicator_pin,OUTPUT);
  pinMode(laser_gunsight_pin,OUTPUT);
  pinMode(muzzlefire_pin,OUTPUT);

  // load ir config constants from EEPROM
  // duration start
  if(EEPROM.read(0)!= 255 || EEPROM.read(1)!= 255){
    duration_start = (EEPROM.read(0)<<8) | EEPROM.read(1);
  } else {
    duration_start = DEFAULT_DURATION_START;
    EEPROM.update(0, (duration_start&0xFF00)>>8);
    EEPROM.update(1, duration_start&0xFF);
  }
  if(EEPROM.read(2)!= 255 || EEPROM.read(3)!= 255){
    duration_one = (EEPROM.read(2)<<8) | EEPROM.read(3);
  } else {
    duration_one = DEFAULT_DURATION_ONE;
    EEPROM.update(2, (duration_one&0xFF00)>>8);
    EEPROM.update(3, duration_one&0xFF);
  }
  if(EEPROM.read(4)!= 255 || EEPROM.read(5)!= 255){
    duration_zero = (EEPROM.read(4)<<8) | EEPROM.read(5);
  } else {
    duration_zero = DEFAULT_DURATION_ZERO;
    EEPROM.update(4, (duration_zero&0xFF00)>>8);
    EEPROM.update(5, duration_zero&0xFF);
  }
  if(EEPROM.read(6)!= 255 || EEPROM.read(7)!= 255){
    pulse_delay = (EEPROM.read(6)<<8) | EEPROM.read(7);
  } else {
    pulse_delay = DEFAULT_PULSE_DELAY;
    EEPROM.update(6, (pulse_delay&0xFF00)>>8);
    EEPROM.update(7, pulse_delay&0xFF);
  }

  // Init Timer1 with its corresponding ISR to detect button presses and handle muzzlefire
  Timer1.initialize(20000); // 20000us = 20ms between each interrupt
  Timer1.attachInterrupt( button_timer_ISR ); // attach the service routine here
}

void loop() {
  //RGB-LED blink code
  long current_timestamp=millis();
  if(blink_enable){
    if(current_timestamp-blink_start_timestamp<blink_duration || blink_until_stopped)
    {
      if(current_timestamp-blink_timestamp>1000.0/(2*blink_frequency)){
        if(blink_state) write_color(player_color);
        else            write_color("black");
        blink_timestamp=current_timestamp;
        blink_state=!blink_state;
      }
    }else{
      write_color(player_color);
      blink_enable=false;
    }
  }

  // send shot over IR (Timer1 interrupt has to be disabled, so it cannot disturb this)
  if(shot_active) {
    Timer1.detachInterrupt();
    morse_word(shot);
    Timer1.attachInterrupt( button_timer_ISR );
    shot_active=false;
  }

  //Button press serial send code
  noInterrupts(); //disable all interrupts for checking and sending of button presses (so it cannot change during this)
  if(serial_write_pt!=0){
    char *pt=serial_write_pt;
    serial_write_pt=0;
    interrupts(); // enable interrupt after copying the pointer to local variable
    for(unsigned int i=0;i<5 && pt[i]!=0;i++) Serial.write(pt[i]); // send over serial
  } else interrupts(); // reenable interrupts
}

// Timer 1 ISR
//    checks for button presses and muzzlefire
void button_timer_ISR(){
  if(tagger_enabled){
    if(machinegun_enabled){ // machinegun modus --> shooting again and again without releasing the trigger button
      if(!(PIND & (1 << trigger_pin))){ // check if trigger pin is low
        trigger_counter--;
        if(trigger_counter == 0){
          shot_active=true;
          serial_write_pt=serial_trigger;
          if(muzzlefire_enabled){
            muzzlefire_counter = MUZZLEFIRE_DURATION;
          }
        }
        if(trigger_counter < -shot_delay){ // count to -shot_delay, to disable shooting for a specific time (define over shot frequency)
          trigger_counter = button_delay;
        }
      } else {
        if(trigger_counter < -shot_delay){
          trigger_counter=button_delay;
        } else if(trigger_counter != button_delay) trigger_counter--;
      }
    } else { // pistol modus --> after every shot the trigger has to be released in order to shoot again
      if(!(PIND & (1 << trigger_pin))){ // check if trigger pin is low
        if(trigger_counter >= -shot_delay ) trigger_counter--;
        if(trigger_counter == 0){
          shot_active=true;
          serial_write_pt=serial_trigger;
          trigger_counter = -1;
          if(muzzlefire_enabled){
            muzzlefire_counter = MUZZLEFIRE_DURATION;
          }
        }
      } else {
        if(trigger_counter < -shot_delay) trigger_counter = button_delay;// count to -shot_delay, to disable shooting for a specific time (define over shot frequency)
        else if(trigger_counter != button_delay) trigger_counter--;
      }
    }
  }

  if(!(PIND & (1 << recharge_pin))){ // check if recharge pin is low
    if(recharge_counter > -1) recharge_counter--;
    if(recharge_counter == 0){
      serial_write_pt=serial_recharge;
      recharge_counter = -1;
    }
  } else {
    recharge_counter = button_delay;
  }

  // Muzzlefire Code
  if(muzzlefire_counter > 0){
    if(muzzlefire_counter % MUZZLEFIRE_BLINK_DURATION == 0){
      digitalWrite(muzzlefire_pin,!digitalRead(muzzlefire_pin));
    }
    muzzlefire_counter--;
  } else {
    digitalWrite(muzzlefire_pin,LOW);
  }
}


// function that executes whenever data is received from master
// this function is registered as an event, see setup()
void I2CreceiveEvent(int howMany) {
  while(Wire.available()>0)
  {
    unsigned char c=Wire.read();
    // check for first byte, to detect the kind of transmission
    if(bytecount==0){
      switch(c){
        case 's': // IR Signal was received
          bytecount=1;
          break;
        case 'p': // Receiver ping signal
          Serial.print("Tpr\n");
          break;
        case 'I': // Receiver ID
          c=Wire.read();
          break;
      }
      continue;
    }
    
    // on IR signal, receive the other bytes, check parity and write them to serial
    if(bytecount==1)
    {
      I2CID=c;
      bytecount++;
    }
    else if(bytecount==2)
    {
      I2Cvalue_high=c;
      bytecount++;
    }
    else if(bytecount==3)
    {
      I2Cvalue_low=c;
      bytecount=0;
      if(parity(I2Cvalue_low)==1 || parity(I2Cvalue_high)==1) {I2Cvalue_low=0;I2Cvalue_high=0;continue;}
      if((I2Cvalue_high&0b10000000)==0) // check for shot or game command
      {
        Serial.write('s'); // shot
        Serial.write(I2CID);
        Serial.write(I2Cvalue_high);
        Serial.write(I2Cvalue_low);
        Serial.write('\n');
      } else
      {
        Serial.write('g'); // game command
        Serial.write(I2CID);
        Serial.write(I2Cvalue_high);
        Serial.write(I2Cvalue_low);
        Serial.write('\n');
      }
      I2Cvalue_high=0;
      I2Cvalue_low=0;
      I2CID=0;
    }
  }
}
// Function to handle data received over Serial
//    is called between loop calls
void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the serial_string:
    serial_string += inChar;
    // if the incoming character is a newline, set a flag
    if (inChar == '\n') {
      serial_stringComplete = true;
    }
  }

  // process whole serial message
  if(serial_stringComplete)
  {
    switch(serial_string[0])
    {
      case '.': // ping for ok
        Serial.print("ok\n");
        break;
      case 'i':{ // send command over IR
        // load bytes from serial string and clear LSB, because that is meant for parity
        byte high_byte = serial_string[1] & (~(0b1));
        byte low_byte = serial_string[2] & (~(0b1));
        // calculate parity bits
        high_byte = high_byte | parity(high_byte);
        low_byte = low_byte | parity(low_byte);
        // calculate total word
        unsigned int value = (high_byte<<8) | low_byte;
        // send command over IR
        morse_word(value);
        break;}
      case 'l': // set life points
        player_stats.life_points = serial_string.substring(1,serial_string.indexOf("\n")).toInt();
        break;
      case 's': // set shield points
        player_stats.shield_points = serial_string.substring(1,serial_string.indexOf("\n")).toInt();
        break;
      case 'a': // set ammo
        player_stats.ammo_points = serial_string.substring(1,serial_string.indexOf("\n")).toInt();
        break;
      case 'p': // set ammo packs
        player_stats.ammo_packs = serial_string.substring(1,serial_string.indexOf("\n")).toInt();
        break;
      case 'e': // set extra lifes
        player_stats.extra_lifes = serial_string.substring(1,serial_string.indexOf("\n")).toInt();
        break;
      case 'w': // set weapon
        switch(serial_string[1]){
          case 'd':{//damage
            byte temp = serial_string.substring(2,serial_string.indexOf("\n")).toInt();
            if(temp<16) shot_damage = temp;
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
            update_shot_parity_bits();
            break;}
          case 't':{//type --> type|frequency
            byte temp = serial_string.substring(2,serial_string.indexOf("|")).toInt();
            if(temp<4) {
              weapon_type = temp;
              shot_delay= 1.0/(serial_string.substring(serial_string.indexOf("|")+1,serial_string.indexOf("\n")).toFloat()*0.02); // *0.02 because shot_delay is in units of 20ms
            }
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
            update_shot_parity_bits();
            break;}
        }
        break;
      case 'f': // set color
        serial_string.toLowerCase();
        player_color=serial_string.substring(1,serial_string.indexOf("\n"));
        write_color(player_color);
        break;
      case 'g': // game command to tagger
        switch(serial_string[1])
        {
          case 't':{ // change team
            byte tempID=serial_string.substring(2,serial_string.indexOf("\n")).toInt();
            if(tempID<4) team_ID = tempID;
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
            update_shot_parity_bits();
            break;}
          case 'p':{ // change playerID
            byte tempID=serial_string.substring(2,serial_string.indexOf("\n")).toInt();
            if(tempID<32) player_ID = tempID;
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
            update_shot_parity_bits();
            break;}
          case 'd': // disable tagger
            tagger_enabled = false;
            break;
          case 'e': // enable tagger
            tagger_enabled = true;
            break;
          case 'l': // Laser Gunsight
            switch(serial_string[2])
            {
              case 'e': // enable
                lasergunsight_enabled = true;
                digitalWrite(laser_gunsight_pin,HIGH);
                break;
              case 'd': // disable
                lasergunsight_enabled = false;
                digitalWrite(laser_gunsight_pin,LOW);
                break;
            }
            break;
          case 'm': // muzzle fire
            switch(serial_string[2])
            {
              case '1': // on
                muzzlefire_enabled = true;
                break;
              case '0': // off
                muzzlefire_enabled = false;
                break;
            }
            break;
          case 'g': // machinegun functionality
            switch(serial_string[2])
            {
              case '1': // on
                machinegun_enabled = true;
                break;
              case '0': // off
                machinegun_enabled = false;
                break;
            }
            trigger_counter = button_delay;
            recharge_counter = button_delay;
            break;
          case 'a': // ammo indicator
            switch(serial_string[2])
            {
              case '1': // on
                digitalWrite(ammo_indicator_pin,HIGH);
                break;
              case '0':; // off
                digitalWrite(ammo_indicator_pin,LOW);
                break;
            }
            break;
          case 'b': // blink LEDs;  frequency|duration  ; frequency in Hz, duration in ms
            blink_frequency = serial_string.substring(2,serial_string.indexOf("|")).toFloat();
            blink_duration = serial_string.substring(serial_string.indexOf("|")+1,serial_string.indexOf("\n")).toInt();
            blink_start_timestamp=millis();
            blink_timestamp=millis();
            blink_enable = true;
            write_color(player_color);
            break;
          case 'u': // blink LEDs until stopped; frequency in Hz
            blink_frequency = serial_string.substring(2,serial_string.indexOf("\n")).toFloat();
            blink_until_stopped = true;
            blink_timestamp=millis();
            blink_enable = true;
            write_color(player_color);
            break;
          case 'v': // stop blink LEDs
            blink_until_stopped = false;
            blink_enable = false;
            write_color(player_color);
            break;
          case 's': // shot delay
            shot_delay=(serial_string[3]<<8) | serial_string[4];
            break;
        }
      break;
      case 'c': // config
        switch(serial_string[1]){
          case 'r':{ // send IDs of connected receivers
            receivers = "";
            for(byte i=minReceiverID;i<=maxReceiverID;i++){
              Wire.beginTransmission(i);
              Wire.write('I');
              if(Wire.endTransmission()==0){
                receivers+=String(i-minReceiverID)+",";
              }
              delay(5);
            }
            if(receivers.length()>0) receivers = receivers.substring(0,receivers.length()-1); // cut last comma
            Serial.print("c"+receivers+"\n"); // e.g. receivers = "1,2,3,4,5"
            break;}
          case 'i': // config of IR
            switch(serial_string[2]){
              case 's':{ // set start pulse length in us
                unsigned long temp_start=duration_start;
                duration_start = serial_string.substring(3,serial_string.indexOf("\n")).toInt();
                if(!update_ir_pulse_config()) {duration_start = temp_start;break;}
                EEPROM.update(0, (duration_start&0xFF00)>>8);
                EEPROM.update(1, duration_start&0xFF);
                break;}
              case '1':{ // set pulse length for logic '1' in us
                unsigned long temp_one = duration_one;
                duration_one = serial_string.substring(3,serial_string.indexOf("\n")).toInt();
                if(!update_ir_pulse_config()) {duration_one = temp_one;break;}
                EEPROM.update(2, (duration_one&0xFF00)>>8);
                EEPROM.update(3, duration_one&0xFF);
                break;}
              case '0':{ // set pulse length for logic '0' in us
                unsigned long temp_zero = duration_zero;
                duration_zero = serial_string.substring(3,serial_string.indexOf("\n")).toInt();
                if(!update_ir_pulse_config()) {duration_zero = temp_zero;break;}
                EEPROM.update(4, (duration_zero&0xFF00)>>8);
                EEPROM.update(5, duration_zero&0xFF);
                break;}
              case 'd':{ // set pulse delay between two pulses in us
                unsigned long temp_delay = pulse_delay;
                pulse_delay = serial_string.substring(3,serial_string.indexOf("\n")).toInt();
                if(!update_ir_pulse_config()) {pulse_delay = temp_delay;break;}
                EEPROM.update(6, (pulse_delay&0xFF00)>>8);
                EEPROM.update(7, pulse_delay&0xFF);
                break;}
            }
            break;
          case 'b': // config of bluetooth serial module
            switch(serial_string[2]){
              case 'n': // change name of bluetooth device
                // if muzzlefire is active, it is triggered if an error occured during configuration of BT module (when the answer of the module times out)
                if(!change_BT_config(true, serial_string.substring(3,serial_string.indexOf("\n")).c_str())) muzzlefire_counter = MUZZLEFIRE_DURATION;
                break;
              case 'p': // change pin
                // if muzzlefire is active, it is triggered if an error occured during configuration of BT module (when the answer of the module times out)
                if(!change_BT_config(false, serial_string.substring(3,serial_string.indexOf("\n")).c_str())) muzzlefire_counter = MUZZLEFIRE_DURATION;
                break;
              case 'v': {// check version of bluetooth serial device (which is received by the tagger at powerup/reset)
                char temp[23];
                sprintf(temp,"bv%s\n",bt_version);
                Serial.print(temp);
                break;}
            }
            break;
          case 'm': // config of muzzlefire
            break;
          case 'l': // config of RGB LED blinking
            break;
          case 'I':{ // Identify Receiver through blinking
            byte id = serial_string.substring(2,serial_string.indexOf("\n")).toInt();
            Wire.beginTransmission(id+minReceiverID);
            Wire.write('b');
            Wire.endTransmission();
            break;}
          case 'p': // send ping
            switch(serial_string[2]){
              case 't': // tagger ping
                Serial.print("Tpt\n");
                break;
              case 'r': // send receiver ping (ot first available receiver only)
                for(byte i=minReceiverID;i<=maxReceiverID;i++){
                  Wire.beginTransmission(i);
                  Wire.write('p');
                  if(Wire.endTransmission()==0){
                    break;
                  }
                }
                break;
            }
            break;
        }
        break;
    }
    serial_string = "";
    serial_stringComplete = false;
  }
}


// Function, that handles config changes of the Bluetooth serial module
bool change_BT_config(bool change_name, char *str){
  delay(1000);
  if(change_name){ // change name
    char cmd[28];
    sprintf(cmd,"AT+NAME%s",str);
    Serial.print("AT");
    if(!wait_for_ok()) {
      while(Serial.available()>0) Serial.read();
      return false;
    }
    Serial.print(cmd);
    if(!wait_for_ok()) {
      while(Serial.available()>0) Serial.read();
      return false;
    }
    while(Serial.available()>0) Serial.read();
  } else { // change pin
    char cmd[12];
    sprintf(cmd,"AT+PIN%s",str);
    Serial.print("AT");
    if(!wait_for_ok()) {
      while(Serial.available()>0) Serial.read();
      return false;
    }
    Serial.print(cmd);
    if(!wait_for_ok()) {
      while(Serial.available()>0) Serial.read();
      return false;
    }
    while(Serial.available()>0) Serial.read();
  }
  return true;
}

// Wait for "OK" to be received over serial
//     returns false if a timeout of 1 second has occured (the BT module has to receive "AT" at least every second to stay in config mode)
bool wait_for_ok(){
  unsigned long timestamp=0;
  byte state=0;
  timestamp=millis();
  while(state<2){
    if(millis()-timestamp > 1000) {return false;}
    if(Serial.available()>0){
      char c=Serial.read();
      if(c=='O') state=1;
      if(c=='K') state=2;
    }
  }
  return true;
}

// Function, that requests the version of the Bluetooth serial module
bool check_bt_version(){
  delay(200);
  byte i;
  Serial.print("AT");
  if(!wait_for_ok()){bt_version[0]=0;return false;}
  Serial.print("AT+VERSION");
  if(!wait_for_ok()){bt_version[0]=0;return false;}
  delay(100);
  for(i=0;i<20 && Serial.available()>0;i++){
    bt_version[i]=Serial.read();
  }
  bt_version[i]=0;
  return true;
}

// Sends new IR pulse durations to all receivers
bool update_ir_pulse_config(){
  for(byte i=minReceiverID;i<=maxReceiverID;i++){
    Wire.beginTransmission(i);
    Wire.write('i');
    Wire.write((duration_start&0xFF00)>>8);
    Wire.write(duration_start&0xFF);
    Wire.write((duration_one&0xFF00)>>8);
    Wire.write(duration_one&0xFF);
    Wire.write((duration_zero&0xFF00)>>8);
    Wire.write(duration_zero&0xFF);
    Wire.write((pulse_delay&0xFF00)>>8);
    Wire.write(pulse_delay&0xFF);
    Wire.endTransmission();
    delay(1);
  }
  return true;
}

// Pulses the IR LED for the specified duration, so that the receiver
//    IC detects it
void pulse(unsigned long duration)// duration in microseconds
{
  unsigned long start_time=micros();
  while(micros()-start_time<duration) 
  {
    digitalWrite(ir_led, HIGH);
    delayMicroseconds(delayus);
    digitalWrite(ir_led, LOW);
    delayMicroseconds(delayus);
  }
  delayMicroseconds(pulse_delay);
}

// Send one byte over IR
void morse_byte(unsigned char c)
{
  pulse(duration_start);//startpulse
  if((c&0b10000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b01000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00100000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00010000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00001000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00000100)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00000010)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b00000001)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  delayMicroseconds(bytes_delay);
}

// Send two bytes (one word) over IR
void morse_word(unsigned int c)
{
  pulse(duration_start);//startpulse
  if((c&0b1000000000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0100000000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0010000000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0001000000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000100000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000010000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000001000000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000100000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000010000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000001000000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000100000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000010000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000001000)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000000100)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000000010)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  if((c&0b0000000000000001)!=0) pulse(duration_one);
  else      pulse(duration_zero);
  delayMicroseconds(bytes_delay);
}

// Set RGB LED color through sending correspondig byte code to all expander ICs (have all the same address)
void write_color(String color){
  digitalWrite(ammo_indicator_pin,HIGH);
  delayMicroseconds(1);
  byte send_byte;
  if(color=="black")        send_byte=COLOR_CODE.black;
  else if(color=="white")   send_byte=COLOR_CODE.white;
  else if(color=="yellow")  send_byte=COLOR_CODE.yellow;
  else if(color=="violett") send_byte=COLOR_CODE.violett;
  else if(color=="turkis")  send_byte=COLOR_CODE.turkis;
  else if(color=="blue")    send_byte=COLOR_CODE.blue;
  else if(color=="green")   send_byte=COLOR_CODE.green;
  else if(color=="red")     send_byte=COLOR_CODE.red;
  else {return;}
  Wire.beginTransmission(expander_address);
  Wire.write(send_byte);
  Wire.endTransmission();
  digitalWrite(ammo_indicator_pin,LOW);
}

