#include <TinyWire.h>
#include <EEPROM.h>

// definitions of pins
#define IR_pin 4
#define led_pin 1
#define ir_led 3

// generic definies
#define address_ID_offset 11 // receiver addresses start at 11
#define timeout 20000 // detection timeout for IR code
#define I2Ctransmission_retries 3

// definition of IR default values
#define DEFAULT_DURATION_START 800
#define DEFAULT_DURATION_ONE   200
#define DEFAULT_DURATION_ZERO  500
#define DEFAULT_PULSE_DELAY    500

byte ID = 0; // ID of this receiver. Has to be set different for every used receiver in one system
byte nano_address = 10;

byte I2C_buffer[6]={0,0,0,0,0,0};
byte I2C_pos=0;

unsigned int pulse_length[16];
unsigned int bitvalues16[16] = {32768, 16384, 8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1};
unsigned int bit_num=0;
boolean start_detected = false;
boolean send_please=false;
unsigned int value=0;

unsigned int I2Canswer=0;

byte blink_state=0;
unsigned long blink_timestamp=0;

// IR pulse lengths
#define delayus             6  // pulse length for IR modulation. value of 6 for use with tsop4838 IR Receiver chip
unsigned long duration_zero=500;
unsigned long duration_one=200;
unsigned long duration_start=800;
unsigned long pulse_delay=500;
unsigned long bytes_delay=2000;
unsigned long start_lower=750;
unsigned long start_upper=1000;
unsigned long one_lower=150;
unsigned long one_upper=400;
unsigned long zero_lower=400;
unsigned long zero_upper=750;

// variable, that holds the IR-Code, that should be send
unsigned int shot=0;


// functions for detecting the correspondig IR pulse lengths
boolean is_one(unsigned long l){
  if(l>one_lower && l<=one_upper) return true;
  return false;
}

boolean is_zero(unsigned long l){
  if(l>zero_lower && l<=zero_upper) return true;
  return false;
}

boolean is_start(unsigned long l){
  if(l>start_lower && l<=start_upper) return true;
  return false;
}

// Parity Check function
// Returns 1 (ODD) or 0 (EVEN) parity
int parity (unsigned char x)
{
    x = x ^ x >> 4;
    x = x ^ x >> 2;
    x = x ^ x >> 1;
    return x & 1;
}

// displays I2C errors as LED blinking for debug purposes
void show_error(byte error){
  for(int i=0;i<error;i++){
    digitalWrite(led_pin,HIGH);
    delay(400);
    digitalWrite(led_pin,LOW);
    delay(400);
  }
}

void setup() {
  // set pinmodes
  pinMode(IR_pin, INPUT);
  pinMode(led_pin, OUTPUT);
  digitalWrite(led_pin,HIGH);
  pinMode(ir_led, OUTPUT);

  // load ir config constants from EEPROM
  // duration start
  if(EEPROM.read(0)!= 255 || EEPROM.read(1)!= 255){
    duration_start = (EEPROM.read(0)<<8) | EEPROM.read(1);
  } else {
    duration_start = DEFAULT_DURATION_START;
    EEPROM.update(0, (duration_start&0xFF00)>>8);
    EEPROM.update(1, duration_start&0xFF);
  }
  // duration one
  if(EEPROM.read(2)!= 255 || EEPROM.read(3)!= 255){
    duration_one = (EEPROM.read(2)<<8) | EEPROM.read(3);
  } else {
    duration_one = DEFAULT_DURATION_ONE;
    EEPROM.update(2, (duration_one&0xFF00)>>8);
    EEPROM.update(3, duration_one&0xFF);
  }
  // duration zero
  if(EEPROM.read(4)!= 255 || EEPROM.read(5)!= 255){
    duration_zero = (EEPROM.read(4)<<8) | EEPROM.read(5);
  } else {
    duration_zero = DEFAULT_DURATION_ZERO;
    EEPROM.update(4, (duration_zero&0xFF00)>>8);
    EEPROM.update(5, duration_zero&0xFF);
  }
  // pulse delay
  if(EEPROM.read(6)!= 255 || EEPROM.read(7)!= 255){
    pulse_delay = (EEPROM.read(6)<<8) | EEPROM.read(7);
  } else {
    pulse_delay = DEFAULT_PULSE_DELAY;
    EEPROM.update(6, (pulse_delay&0xFF00)>>8);
    EEPROM.update(7, pulse_delay&0xFF);
  }
  update_pulse_limits();

  // Initiate the TinyWire library (for I2C communication) as slave
  TinyWire.begin(ID+address_ID_offset);

  // clear pulse length array
  for(int i=0;i<16;i++) pulse_length[i]=0;
}

void loop() {
  // I2C Code
  if(TinyWire.available()>0){
    switch(TinyWire.read()){
      case 'I': // ID was requested
        TinyWire.beginTransmission(nano_address);
        TinyWire.send('I');
        TinyWire.send(ID);
        I2Canswer = TinyWire.endTransmission();
        break;
      case 'i': // new IR pulse config was transmitted
        byte i2cbuffer[8];
        for(int i=0;i<8;i++){
          if(TinyWire.available()==0) return;
          i2cbuffer[i]=TinyWire.read();
        }
        duration_start = i2cbuffer[0]<<8;
        duration_start |= i2cbuffer[1];
        EEPROM.update(0, (duration_start&0xFF00)>>8);
        EEPROM.update(1, duration_start&0xFF);
        duration_one = i2cbuffer[2]<<8;
        duration_one |= i2cbuffer[3];
        EEPROM.update(2, (duration_one&0xFF00)>>8);
        EEPROM.update(3, duration_one&0xFF);
        duration_zero = i2cbuffer[4]<<8;
        duration_zero |= i2cbuffer[5];
        EEPROM.update(4, (duration_zero&0xFF00)>>8);
        EEPROM.update(5, duration_zero&0xFF);
        pulse_delay = i2cbuffer[6]<<8;
        pulse_delay |= i2cbuffer[7];
        EEPROM.update(6, (pulse_delay&0xFF00)>>8);
        EEPROM.update(7, pulse_delay&0xFF);
        update_pulse_limits();
        break ;
      case 'c': // send IR command through receiver IR LED
        if(TinyWire.available()==0) break;
        shot = TinyWire.read() << 8;
        if(TinyWire.available()==0) break;
        shot |= TinyWire.read();
        morse_word(shot);
        break;
      case 'b': // blink for identifying the receiver
        blink_state=1;
        break;
      case 'p': // send receiver ping to nano (for time measurement)
        TinyWire.beginTransmission(nano_address);
        TinyWire.send('p');
        I2Canswer = TinyWire.endTransmission();
        break;
    }
  }

  // Received shot I2C code (sending shot bytes to nano)
  if(send_please) {
    value=0;
    // check pulse lengths for adding up the whole transmitted value
    for(int i=0;i<16;i++){
      if(is_one(pulse_length[i])){
        value+=bitvalues16[i];
      } else if(!is_zero(pulse_length[i])){ // pulse was invalid
        send_please = false;
        for(int i=0;i<16;i++) pulse_length[i]=0;
        return;
      }
      pulse_length[i]=0;
    }

    // divide in bytes, check parity and transmit bytes to nano
    byte high_value = (value & 0xFF00) >> 8;
    byte low_value = value & 0xFF;
    if(parity(high_value)==0 || parity(low_value)==0){ // parity check was ok
      TinyWire.beginTransmission(nano_address);
      TinyWire.send('s');
      TinyWire.send(ID); // send ID for checking, where player was hit
      TinyWire.send(high_value);
      TinyWire.send(low_value);
      I2Canswer = TinyWire.endTransmission();
      if(I2Canswer!=0){
        byte i=0;
        do{
          if(i==I2Ctransmission_retries) {
            show_error(I2Canswer); // on I2C error, show through blinking for debug purposes
            return;
          }
          delay(4);
          TinyWire.beginTransmission(nano_address);
          TinyWire.send('s');
          TinyWire.send(ID); // send ID for checking, where player was hit
          TinyWire.send(high_value);
          TinyWire.send(low_value);
          I2Canswer = TinyWire.endTransmission();
          i++;
        } while(I2Canswer!=0); // retry was successfull
      }
      blink_state = 1;// activate blink code
    }

    // reset for next pulse
    for(int i=0;i<16;i++) pulse_length[i]=0;
    send_please=false;
  }

  // Blink code (non-blocking)
  if(blink_state > 0 && millis()-blink_timestamp > 50){
    blink_timestamp = millis();
    PORTB ^= 1 << led_pin;
    blink_state++;
    if(blink_state > 14) {blink_state=0;PORTB |= 1 << led_pin;}
  }

  // IR Code
  unsigned long timestamp = micros();
  while(PINB & 0b10000) { //wait for ir pin to go low
    if(micros()-timestamp > timeout){
      return;
    }
  }
  timestamp = micros();
  while(!(PINB & 0b10000)) { // wait for ir pin to go high again
    if(micros()-timestamp > timeout) {
      delay(1);
      return;
    }
  }
  unsigned long temp_length = micros() - timestamp;
  // put measured pulse lengths in array corresponding to detected start puls
  if(is_start(temp_length)) {
    bit_num = 0;
    start_detected = true;
  } else {
    pulse_length[bit_num] = temp_length;
    bit_num++;
    if(bit_num>15 && start_detected) {
      bit_num = 0;
      send_please=true;
      start_detected = false;
    }
  }

  // Reset I2C Software to prevet block of library (hotfix, quick and dirty)
  if(millis()-TinyWire.LastActiveTimestamp()>1000){
    TinyWire.begin(ID+address_ID_offset);
  }
}


// update limits for pulse detection
//    50 is added to the duration due to effects of the receiver IC
void update_pulse_limits(){
  unsigned long one_epsilon=(duration_zero-duration_one)/2;
  one_upper = duration_one + 50 + one_epsilon;
  one_lower = duration_one + 50 - one_epsilon;
  if(one_lower > 40000) one_lower = 1;
  unsigned long start_epsilon=(duration_start-duration_zero)/2;
  start_upper = duration_start + 50 + start_epsilon;
  start_lower = duration_start + 50 - start_epsilon;
  zero_upper = start_lower;
  zero_lower = one_upper;
}

// Pulses the IR LED for the specified duration, so that the receiver
//    IC detects it
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

