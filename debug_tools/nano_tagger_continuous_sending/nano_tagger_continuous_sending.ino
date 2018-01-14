
#include <Wire.h>
//#include <PinChangeInt.h>
//#include <PinChangeIntConfig.h>

int delayus=6;// 6 für empfang mit tsop4838
unsigned long duration_zero=500;
unsigned long duration_one=200;
unsigned long duration_start=800;
unsigned long pulse_delay=500;//ursprünglich 500
unsigned long bytes_delay=2000; // in us
int ir_led = 5;
int rled=6;
int trigger_pin=2;
int recharge_pin=3;
int ammo_indicator_pin=7;
int muzzlefire_pin=8;
int laser_gunsight_pin=9;
const byte expander_address=0b0111000;
volatile unsigned long trigger_timestamp=0;
volatile unsigned long recharge_timestamp=0;
unsigned int bytecount=65535;
unsigned char I2Cvalue_high=0;
unsigned char I2Cvalue_low=0;
unsigned char I2CID=0;
boolean rled_state=LOW;

unsigned long timestamp=0;

volatile boolean tagger_enabled=true;
volatile boolean lasergunsight_enabled = false;
volatile boolean muzzlefire_enabled = true;

float blink_frequency=1.0;
unsigned int blink_duration=1000;
boolean blink_until_stopped=false;
unsigned long blink_start_timestamp=0;
unsigned long blink_timestamp=0;
boolean blink_enable=false;
boolean blink_state=false;

boolean muzzlefire_led_state=false;
unsigned long muzzlefire_start_timestamp=0;
unsigned long muzzlefire_timestamp=0;
#define MUZZLEFIRE_DURATION 500
#define MUZZLEFIRE_BLINK_DURATION 50

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
//unsigned int send_state=0;

String serial_string = "";         // a string to hold incoming data
boolean serial_stringComplete = false;  // whether the string is complete
char serial_trigger[5]="t\n";
char serial_recharge[5]="r\n";
volatile char *serial_write_pt=0;
String receivers="";

byte team_ID = 0b01; // 2 bit
byte player_ID = 0b00001; // 5 bit, Player 1 reserved for enviroment
byte weapon_type = 0b00; // 2 bit
byte shot_damage = 0b1111; // 4 bit
struct PLAYER_STATS_STRUCT
{
  unsigned int life_points;
  unsigned int shield_points;
  unsigned int ammo_points;
  unsigned int ammo_packs;
  unsigned int extra_lifes;
}player_stats;
volatile unsigned int shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1);//0b0111110100001001
volatile unsigned int shot_delay=200;
volatile boolean shot_active=false;
unsigned int minimum_button_delay=10; // required for debouncing of the buttons
String player_color="red";


// Returns 1 (ODD) or 0 (EVEN) parity
int parity (unsigned char x)
{
    x = x ^ x >> 4;
    x = x ^ x >> 2;
    x = x ^ x >> 1;
    return x & 1;
}


void setup() {
  
  // fill shot parity bits for each shot byte (even parity) --> 0 --> even number of 1s,  1--> odd number of 1s
  shot = (shot | parity((shot&0xFF00)>>8)<<8);
  shot = (shot | parity((shot&0x00FF)));
  
  Wire.begin(10);                // join i2c bus with address #10
  Wire.onReceive(I2CreceiveEvent); // register event
  Serial.begin(9600);           // start serial for communication with smartphone

  // configure buttons
  pinMode(trigger_pin,INPUT_PULLUP);
  //digitalWrite(trigger_pin,HIGH); // enable internal pullup resistor
  pinMode(recharge_pin,INPUT_PULLUP);
  pinMode(10,OUTPUT);
  digitalWrite(recharge_pin,HIGH); // enable internal pullup resistor
  attachInterrupt(digitalPinToInterrupt(trigger_pin),trigger_pulled,FALLING);
  attachInterrupt(digitalPinToInterrupt(recharge_pin),recharge_pressed, FALLING);
  //PCintPort::attachInterrupt(trigger_pin, trigger_pulled, FALLING); // enable Interrupt on falling edge and bind to function
  //PCintPort::attachInterrupt(recharge_pin, recharge_pressed, FALLING);

  // configure outputs
  pinMode(ir_led, OUTPUT);
  pinMode(rled,OUTPUT);
  pinMode(ammo_indicator_pin,OUTPUT);
  pinMode(laser_gunsight_pin,OUTPUT);
}

void loop() {
  /*if(digitalRead(trigger_pin)==1)
  {
    if(tagger_enabled)
    {
      Serial.write('t');
      Serial.write('\n');
      morse_word(shot);
      delay(shot_delay);
    }
  }
  if(digitalRead(recharge_pin)==1)
  {
    Serial.write('r');
    Serial.write('\n');
  }*/
  
  //RGB-LED blink code
  long current_timestamp=millis();
  if(current_timestamp-timestamp>10){
    shot_active=true;
    timestamp=millis();
  }
  
  if(blink_enable && (current_timestamp-blink_start_timestamp<blink_duration || blink_until_stopped))
  {
    if(current_timestamp-blink_timestamp>1.0/(2*blink_frequency)){
      if(blink_state) write_color(player_color);
      else            write_color("black");
      blink_timestamp=current_timestamp;
      blink_state=!blink_state;
    }
  }else{
    blink_enable=false;
  }

  //Muzzlefire blink code
  current_timestamp=millis();
  if(current_timestamp-muzzlefire_start_timestamp<MUZZLEFIRE_DURATION){
    if(current_timestamp-muzzlefire_timestamp>MUZZLEFIRE_BLINK_DURATION){
      digitalWrite(muzzlefire_pin,muzzlefire_led_state);
      muzzlefire_timestamp = current_timestamp;
      muzzlefire_led_state=!muzzlefire_led_state;
    }
  }

  //Button press serial send code
  if(shot_active) {
    detachInterrupt(digitalPinToInterrupt(trigger_pin));
    detachInterrupt(digitalPinToInterrupt(recharge_pin));
    digitalWrite(10,HIGH);
    morse_word(shot);
    digitalWrite(10,LOW);
    attachInterrupt(digitalPinToInterrupt(trigger_pin),trigger_pulled,FALLING);
    attachInterrupt(digitalPinToInterrupt(recharge_pin),recharge_pressed, FALLING);
    shot_active=false;
  }
  noInterrupts(); //disable interrupt for checking and sending of button presses
  if(serial_write_pt!=0){
    char *pt=serial_write_pt;
    serial_write_pt=0;
    interrupts(); // enable interrupt after copying the pointer to literal
    for(unsigned int i=0;i<5 && pt[i]!=0;i++) Serial.write(pt[i]);
  } else interrupts(); // reenable interrupts
}

void trigger_pulled() {
  if((millis()-trigger_timestamp>shot_delay && millis()-trigger_timestamp>minimum_button_delay) && tagger_enabled)
  {
    trigger_timestamp=millis();
    shot_active=true;
    serial_write_pt=serial_trigger;
    if(muzzlefire_enabled){
      muzzlefire_start_timestamp = millis();
      muzzlefire_timestamp = millis();
    }
  }
}

void recharge_pressed() {
  if(millis()-recharge_timestamp>minimum_button_delay){
    recharge_timestamp=millis();
    serial_write_pt=serial_recharge;
  }
}

// function that executes whenever data is received from master
// this function is registered as an event, see setup()
void I2CreceiveEvent(int howMany) {
  while(Wire.available()>0)
  {
    unsigned char c=Wire.read();
    /*Serial.println(c,DEC);
    if(bytecount>2 && c=='s') //signal received
    {
      Serial.write('s');
      bytecount=0;
      continue;
    } else*/
    if(bytecount==0)
    {
      I2CID=c;
      bytecount++;
    }
    else if(bytecount==1)
    {
      I2Cvalue_high=c;
      bytecount++;
    }
    else if(bytecount==2)
    {
      I2Cvalue_low=c;
      bytecount=65535;
      if(parity(I2Cvalue_low)==1 || parity(I2Cvalue_high)==1) {I2Cvalue_low=0;I2Cvalue_high=0;bytecount=65535;continue;}
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
  if(serial_stringComplete)
  {
    switch(serial_string[0])
    {
      case '.': // ping for ok
        Serial.println("ok");
        break;
      case 'i': // send command over IR
        rled_state=!rled_state;
        digitalWrite(rled,rled_state);
        break;
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
            break;}
          case 't':{//type
            byte temp = serial_string.substring(2,serial_string.indexOf("|")).toInt();
            if(temp<4) {
              weapon_type = temp;
              shot_delay= 1000 * (1/serial_string.substring(serial_string.indexOf("|")+1,serial_string.indexOf("\n")).toFloat());
            }
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
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
            break;}
          case 'p':{ // change playerID
            byte tempID=serial_string.substring(2,serial_string.indexOf("\n")).toInt();
            if(tempID<32) player_ID = tempID;
            shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1); // update shot bytes
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
          case 'b': // blink LEDs;  frequency|duration
            blink_frequency = serial_string.substring(2,serial_string.indexOf("|")).toFloat();
            blink_duration = serial_string.substring(serial_string.indexOf("|")+1,serial_string.indexOf("\n")).toInt();
            blink_start_timestamp=millis();
            blink_timestamp=millis();
            blink_enable = true;
            write_color(player_color);
            break;
          case 'u': // blink LEDs until stopped; frequency
            blink_frequency = serial_string.substring(2,serial_string.indexOf("\n")).toFloat();
            blink_until_stopped = true;
            blink_timestamp=millis();
            blink_enable = true;
            write_color(player_color);
          case 'v': // stop blink LEDs
            blink_until_stopped = false;
            blink_enable = false;
          case 's': // shot delay
            shot_delay=(serial_string[3]<<8)+serial_string[4];
            break;
        }
      break;
      case 'c': // config
        switch(serial_string[1]){
          case 'r': // send IDs of connected receivers
            Serial.println("c"+receivers+"\n"); // receivers = "1,2,3,4,5"
            break;
          case 'i': // config of IR
            switch(serial_string[2]){
              case 's': // set start pulse length in us
                break;
              case '1': // set pulse length for logic '1' in us
                break;
              case '0': // set pulse length for logic '0' in us
                break;
              case 'd': // set pulse delay between two pulses in us
                break;
              case 'e': // set epsilon value for detection
                break;
            }
            break;
          case 'b': // config of bluetooth
            switch(serial_string[2]){
              case 'n': // change name of bluetooth device
                break;
              case 'p': // change pin
                break;
              case 'v': // check version of bluetooth serial device
                break;
            }
            break;
          case 'm': // config of muzzlefire
            break;
          case 'l': // config of RGB LED blinking
            break;
        }
        break;
    }
    serial_string = "";
    serial_stringComplete = false;
  }
}

void pulse(unsigned long duration)// duration in microseconds
{
  //unsigned int times = duration/(2*delayus);//times = duration/T = duration/(2*delayus)     | micros() will behave erratically from 1-2ms. I don't know, if this touches us, since we are staying under 1ms
                                                    // I am using a for loop here, but I have to check, if this has an impact on the precision of the pulse duration
  unsigned long start_time=micros();
  while(micros()-start_time<duration) 
  //for(unsigned int i=0;i<times;i++)
  {
    digitalWrite(ir_led, HIGH);
    delayMicroseconds(delayus);
    digitalWrite(ir_led, LOW);
    delayMicroseconds(delayus);
  }
  delayMicroseconds(pulse_delay);
}

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

void write_color(String color){
  byte send_byte;
  if(color=="black")        send_byte=COLOR_CODE.black;
  else if(color=="white")   send_byte=COLOR_CODE.white;
  else if(color=="yellow")  send_byte=COLOR_CODE.yellow;
  else if(color=="violett") send_byte=COLOR_CODE.violett;
  else if(color=="turkis")  send_byte=COLOR_CODE.turkis;
  else if(color=="blue")    send_byte=COLOR_CODE.blue;
  else if(color=="green")   send_byte=COLOR_CODE.green;
  else if(color=="red")     send_byte=COLOR_CODE.red;
  else return;
  Wire.beginTransmission(expander_address);
  Wire.write(send_byte);
  Wire.endTransmission();
}

