int receiver = 6;
unsigned int temp_pulse=0;
#define timeout 20000


void setup() {
  // put your setup code here, to run once:
  pinMode(receiver, INPUT);
  Serial.begin(9600);
}

void loop() {
  /*temp_pulse=pulseIn(receiver,LOW,100000);
  if(temp_pulse>0){
    Serial.println(temp_pulse);
    temp_pulse=0;
  }*/

  unsigned long timestamp = micros();
  while(PIND & 0b10000) { //wait for ir pin to go low
    if(micros()-timestamp > timeout){
      return;
    }
  }
  timestamp = micros();
  while(!(PIND & 0b10000)) { // wait for ir pin to go high again
    if(micros()-timestamp > timeout) {
       return;
    }
  }
  unsigned int temp_length = micros() - timestamp;
  Serial.println(temp_length,DEC);
}
