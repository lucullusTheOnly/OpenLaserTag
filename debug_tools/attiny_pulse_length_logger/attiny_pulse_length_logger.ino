#include <SoftwareSerial.h>

unsigned int temp_pulse = 0;
//byte nano_address=10;

#define receiver 4
#define l        20
#define timeout 20000
//int led = 1;
//int I2Canswer=0;

unsigned int buf[l];
unsigned int pos = 0;
SoftwareSerial mySerial(1, 3); // RX, TX

void setup() {
  // put your setup code here, to run once:
  pinMode(receiver, INPUT);
  for (pos = 0; pos < l; pos++) {
    buf[pos] = 0;
  }
  pos = 0;
  mySerial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  unsigned long timestamp = micros();
  while (PINB & 0b10000) { //wait for ir pin to go low
    if (micros() - timestamp > timeout) {
      return;
    }
  }
  PORTB |= 0b10;
  timestamp = micros();
  while (!(PINB & 0b10000)) { // wait for ir pin to go high again
    if (micros() - timestamp > timeout) {
      return;
    }
  }
  buf[pos] = micros() - timestamp;
  pos++;
  if (pos == l) {
    for (pos = 0; pos < l; pos++) {
      mySerial.write(buf[pos]);
    }
    pos = 0;
  }
  /*temp_pulse=pulseIn(receiver,LOW,10000);
    if(temp_pulse>0){
    buf[pos]=temp_pulse;
    pos++;
    if(pos==l){
      for(pos=0;pos<l;pos++){
        mySerial.println(buf[pos]);
      }
      pos=0;
    }*/
  /*USICR&= 0b11001111; // USI-module off, so that I2C bus is free
    pinMode(0,INPUT);
    pinMode(2,INPUT);*/
  //}
}
