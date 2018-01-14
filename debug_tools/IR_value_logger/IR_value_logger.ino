int receiver = 6;

unsigned int bit_number=0;
unsigned int temp_pulse=0;
unsigned int value=0;
unsigned int bitvalues16[16]={32768,16384,8192,4096,2048,1024,512,256,128,64,32,16,8,4,2,1};

unsigned long duration_zero=545;
unsigned long duration_one=245;
unsigned long duration_start=845;
unsigned long pulse_epsilon=150;

byte team_ID = 0b01; // 2 bit
byte player_ID = 0b00010; // 5 bit, Player 1 reserved for enviroment
byte weapon_type = 0b00; // 2 bit
byte shot_damage = 0b1111; // 4 bit
volatile unsigned int shot = 0 | (team_ID<<13) | (shot_damage<<9) | (player_ID<<3) | (weapon_type<<1);//0b010011

unsigned int total=0;
unsigned int correct=0;
unsigned int parity_correct=0;

// Returns 1 (ODD) or 0 (EVEN) parity
int parity (unsigned char x)
{
    x = x ^ x >> 4;
    x = x ^ x >> 2;
    x = x ^ x >> 1;
    return x & 1;
}

boolean approx(unsigned long value, unsigned long reference, unsigned long epsilon)
{
  if(value>reference-epsilon && value<reference+epsilon) return true;
  else return false;
}

boolean is_one(unsigned long value){
  if(value>100 && value < 350) return true;
  return false;
}

boolean is_zero(unsigned long value){
  if(value>380 && value < 700) return true;
  return false;
}

boolean is_start(unsigned long value){
  if(value>790 && value < 900) return true;
  return false;
}

void setup() {
  shot = (shot | parity((shot&0xFF00)>>8)<<8);
  shot = (shot | parity((shot&0x00FF)));
  // put your setup code here, to run once:
  pinMode(receiver, INPUT);
  Serial.begin(9600);
  Serial.print("Shot info: ");
  Serial.println(shot,BIN);
}

void loop() {
  // put your main code here, to run repeatedly:
  temp_pulse=pulseIn(receiver,LOW,10000);
  if(is_start(temp_pulse))
  {
    if(bit_number>0) {Serial.print(bit_number);Serial.print(".");}
    bit_number=1;
    value=0;
  }
  if(is_zero(temp_pulse) && bit_number>0)
  {
    bit_number++;
  }
  if(is_one(temp_pulse) && bit_number>0)
  {
    value+=bitvalues16[bit_number-1];
    bit_number++;
  }
  if(bit_number>16)
  {
    total++;
      if(parity(value&0x00FF)==0 && parity(value&0xFF00)==0){
      parity_correct++;}
      if(value==shot) {Serial.print("\nCorrect shot!    ");correct++;}
      else Serial.print("\nIncorrect shot!  ");
      Serial.print("total=");
      Serial.print(total);
      Serial.print("   parity_correct=");
      Serial.print(parity_correct);
      Serial.print("   correct=");
      Serial.print(correct);
      Serial.print("   value=");
      Serial.println(value,BIN);
    //}
    value=0;
    
    bit_number=0;
  }
}
