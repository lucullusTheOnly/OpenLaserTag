#include <TinyWire.h>

byte nano_address=10;
byte address=9;
const byte rgb_address=0b0111001;

int ir_led = 1;
int trigger_switch = 3;
int config_switch = 4;

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

// IR pulse lengths
#define delayus             6  // pulse length for IR modulation. value of 6 for use with tsop4838 IR Receiver chip
unsigned long duration_zero=500;
unsigned long duration_one=200;
unsigned long duration_start=800;
unsigned long pulse_delay=500;
unsigned long bytes_delay=2000;

unsigned long countdown=5000;
byte team_ID = 0b01; // 2 bit
byte weapon_type = 0b00; // 2 bit
byte shot_damage = 0b1111; // 4 bit
volatile unsigned int shot = 0 | (team_ID<<13) | (shot_damage<<9) | (weapon_type<<1);
byte current_color=COLOR_CODE.white;
boolean triggered = false;

byte i2c_received=0;
unsigned long timestamp=0;

void setup() {
  // put your setup code here, to run once:
  pinMode(trigger_switch, INPUT_PULLUP);
  pinMode(config_switch, INPUT_PULLUP);
  pinMode(ir_led, OUTPUT);

  // get team_ID preset from jumpers
  TinyWire.begin(address);
  TinyWire.requestFrom(rgb_address, 3);
  if(TinyWire.available()){
      i2c_received = TinyWire.read();
      countdown = i2c_received;
    }
    if(TinyWire.available()){
      i2c_received = TinyWire.read();
      team_ID = i2c_received & 0b01111000;
      weapon_type = i2c_received & 0b00000110;
    }
    if(TinyWire.available()){
      i2c_received = TinyWire.read();
      shot_damage = i2c_received & 0b01111000;
      current_color = (i2c_received & 0b00000111) << 5;
    }
  TinyWire.end();
  shot = 0 | (team_ID<<13) | (shot_damage<<9) | (weapon_type<<1);

  // write color to RGB LEDs
  TinyWire.begin(9);
  TinyWire.beginTransmission(rgb_address);
  TinyWire.send(current_color);
  i2c_received = TinyWire.endTransmission();
  TinyWire.end();
  
}

void loop() {
  if(digitalRead(trigger_switch)==0){
    triggered = true;
    //delay(countdown);
    timestamp = millis();
    while(timestamp + countdown < millis()){
      TinyWire.begin(9);
      TinyWire.beginTransmission(rgb_address);
      TinyWire.send(current_color);
      i2c_received = TinyWire.endTransmission();
      TinyWire.end();

      delay(200);

      TinyWire.begin(9);
      TinyWire.beginTransmission(rgb_address);
      TinyWire.send(COLOR_CODE.black);
      i2c_received = TinyWire.endTransmission();
      TinyWire.end();
    }
    morse_word(shot);
    TinyWire.begin(9);
    TinyWire.beginTransmission(rgb_address);
    TinyWire.send(COLOR_CODE.black);
    i2c_received = TinyWire.endTransmission();
    TinyWire.end();
  }

  if(digitalRead(config_switch)==0){
    TinyWire.begin(9);
    TinyWire.requestFrom(nano_address, 3);// byte 1: countdown in seconds, byte 2: 4bit teamID  2bit weapontype, byte 3:  4bit damage 3bit colorbitcode
    if(TinyWire.available()){
      i2c_received = TinyWire.read();
      countdown = i2c_received;
    }
    if(TinyWire.available()){
      i2c_received = TinyWire.read();
      team_ID = i2c_received & 0b01111000;
      weapon_type = i2c_received & 0b00000110;
    }
    if(TinyWire.available()){
      i2c_received = TinyWire.read();
      shot_damage = i2c_received & 0b01111000;
      current_color = (i2c_received & 0b00000111) << 5;
    }

    TinyWire.beginTransmission(rgb_address);
    TinyWire.send(current_color);
    i2c_received = TinyWire.endTransmission();
    TinyWire.end();
  }

}

void pulse(unsigned long duration)// duration in microseconds
{
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
