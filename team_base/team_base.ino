#include <LiquidCrystal.h>
#include <Wire.h>
#include <PinChangeInt.h>

int ir_led = 11;
int fortress_switch = 14;
int up_pin = 15;
int down_pin = 16;
int ok_pin = 17;
int rgb_pin0 = 8;
int rgb_pin1 = 9;
int rgb_pin2 = 10;
int lcd_d7 = 2;
int lcd_d6 = 3;
int lcd_d5 = 4;
int lcd_d4 = 5;
int lcd_enable = 6;
int lcd_rs = 7;

LiquidCrystal lcd(lcd_rs, lcd_enable, lcd_d4, lcd_d5, lcd_d6, lcd_d7);

int delayus=6;// 6 für empfang mit tsop4838
unsigned long duration_zero=500;
unsigned long duration_one=200;
unsigned long duration_start=800;
unsigned long pulse_delay=700;//ursprünglich 500
unsigned long bytes_delay=2000; // in us

unsigned int bytecount=65535;
unsigned char I2Cvalue_high=0;
unsigned char I2Cvalue_low=0;
unsigned char I2CID=0;
byte signal_counter=0;
unsigned long signal_timestamp=0;
byte signal_team=255;

byte command_ID = 0b00001; // 5 bit
byte base_team_id = 0b0000; // 4 bit
byte base_id = 0b0001; // 4 bit
volatile unsigned int ir_code_signal = 0 | (command_ID<<10) | (base_team_id<<5) | (base_id<<1);
unsigned long signal_send_timestamp = 0;
unsigned long signal_send_interval = 500; // in ms
unsigned int stop_game_signal = 0b1000010000000000;
boolean fortress = false; // false-> teambase, true -> fortress
unsigned long fortress_hold_times[4]={0,0,0,0};
unsigned long fortress_hold_timestamps[4]={0,0,0,0};
int fortress_current_holder=-1;
unsigned long fortress_take_duration = 4000;
unsigned long fortress_take_duration_max = 7000;
unsigned int catched_flags = 0;
boolean detect_flags=true;
const byte possible_colors_size=7;
String possible_colors[possible_colors_size]={"red","green","blue","yellow","violett","turkis","white"};
byte team_colors[4]={0,2,1,3};
unsigned long status_timestamp=0;

unsigned long button_timestamp=0;
unsigned long button_debounce=500;
int button_pushed_id=0;
unsigned long menu_visibility_time=4000;
boolean menu_active=false;
byte point_one[8] = {
  B00101,
  B00100,
  B00101,
  B00100,
  B00101,
  B00100,
  B00101,
};
byte point_two[8] = {
  B11101,
  B00100,
  B00101,
  B11100,
  B10001,
  B10000,
  B11101,
};
byte point_three[8] = {
  B11101,
  B00100,
  B00101,
  B11100,
  B00101,
  B00100,
  B11101,
};
byte point_zero[8] = {
  B11101,
  B10100,
  B10101,
  B10100,
  B10101,
  B10100,
  B11101,
};

const struct COLOR_CODE_STRUCT{ // has to be adapted to wiring of the RGB-LED at the expander
  const byte red=0b100;
  const byte green=0b010;
  const byte blue=0b001;
  const byte yellow=0b110;
  const byte violett=0b101;
  const byte turkis=0b011;
  const byte white=0b111;
  const byte black=0b000;
}COLOR_CODE;

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

  digitalWrite(rgb_pin0,0b001&send_byte);
  digitalWrite(rgb_pin1,(0b010&send_byte)>>1);
  digitalWrite(rgb_pin2,(0b100&send_byte)>>2);
}

class Menu{
  static const byte size_x=4;
  static const byte size_y=4;
  String menus[size_x][size_y];
  unsigned int xy[2]={0,0};

  public:
  Menu(boolean stronghold){
    if(stronghold){
      menus[0][0] = "Mode:Stronghold";
      menus[0][1] = "";
      menus[1][0] = "Teams&Colors   ";
      menus[1][1] = "<---           ";
      menus[1][2] = "Set Teamcolors ";
      menus[1][3] = "";
      menus[2][0] = "Stop Game      ";
      menus[3][0] = "Reset";
    } else {
      menus[0][0] = "Mode:Teambase  ";
      menus[1][0] = "Teams&Colors   ";
      menus[1][1] = "<---           ";
      menus[1][2] = "Set Base Team  ";
      menus[1][3] = "Set Teamcolors ";
      menus[2][0] = "Stop Game      ";
      menus[3][0] = "Reset";
    }
  }

  void show_menu(){
    reset(fortress);
    menu_active=true;
    load_menu_to_display();
  }

  void reset(boolean stronghold){
    if(stronghold){
      menus[0][0] = "Mode:Stronghold";
      menus[0][1] = "";
      menus[1][0] = "Teams&Colors   ";
      menus[1][1] = "<---           ";
      menus[1][2] = "Set Teamcolors ";
      menus[1][3] = "";
      menus[2][0] = "Stop Game      ";
      menus[3][0] = "Reset";
    } else {
      menus[0][0] = "Mode:Teambase  ";
      menus[1][0] = "Teams&Colors   ";
      menus[1][1] = "<---           ";
      menus[1][2] = "Set Base Team  ";
      menus[1][3] = "Set Teamcolors ";
      menus[2][0] = "Stop Game      ";
      menus[3][0] = "Reset";
    }
    xy[0]=0;
    xy[1]=0;
  }

  void load_menu_to_display(){
    lcd.clear();
    if(xy[1]==0){// first menu level
      if(xy[0]%2==0){// first menu point on display is active
        String text="*"+menus[xy[0]][xy[1]];
        if(xy[0]<size_x-1 && menus[xy[0]+1][xy[1]]!=""){
          text+=" "+menus[xy[0]+1][xy[1]];
        }
        lcd.print(text);
      } else { // second menu point on display is active
        lcd.print(" "+menus[xy[0]-1][xy[1]]+"*"+menus[xy[0]][xy[1]]);
      }
    } else { // second menu level
      if(xy[1]%2==1){ // first menu point on display is active
        String text="*"+menus[xy[0]][xy[1]];
        if(xy[1]<size_y-1 && menus[xy[0]][xy[1]+1]!=""){
          text+=" "+menus[xy[0]][xy[1]+1];
        }
        lcd.print(text);
      } else { // second menu point on display is active
        lcd.print(" "+menus[xy[0]][xy[1]-1]+"*"+menus[xy[0]][xy[1]]);
      }
    }
  }

  void update_active(){ // only update star for active menuitem, not complete display
    lcd.setCursor(0,0);
    lcd.write(' ');
    lcd.setCursor(0,1);
    lcd.write(' ');
    if(xy[1]==0){
      if(xy[0]%2==0){
        lcd.setCursor(0,0);
      } else {
        lcd.setCursor(0,1);
      }
    } else {
      if(xy[1]%2==1){
        lcd.setCursor(0,0);
      } else{
        lcd.setCursor(0,1);
      }
    }
    lcd.write('*');
  }

  void up_in_hierarchy(){
    if(xy[1]>0) return;
    if(xy[1]<size_y-1 && menus[xy[0]][xy[1]+1]=="") return;
    xy[1]+=1;
    load_menu_to_display();
  }

  void up(){
    if(xy[1]==0){ // first menu level
      if(xy[0]==0) return;
      if(xy[0]%2==0){ // first menu item on display
        xy[0]-=1;
        load_menu_to_display();
      } else { // second menu item on display
        xy[0]-=1;
        update_active();
      }
    } else { // second menu level
      if(xy[1]==1) return;
      if(xy[1]%2==1){ // first menu item on display
        xy[1]-=1;
        load_menu_to_display();
      } else { // second menu item on display
        xy[1]-=1;
        update_active();
      }
    }
  }

  void down(){
    if(xy[1]==0){ // first menu level
      if(xy[0]==size_x-1) return;
      if(xy[0]%2==0){ // first menu item on display
        xy[0]+=1;
        update_active();
      } else { // second menu item on display
        xy[0]+=1;
        load_menu_to_display();
      }
    } else { // second menu level
      if(xy[1]==size_y-1) return;
      if(xy[1]%2==1) { // first menu item on display
        xy[1]+=1;
        update_active();
      } else { // second menu item on display
        xy[1]+=1;
        load_menu_to_display();
      }
    }
  }

  void enter(){
    if(menus[xy[0]][xy[1]].indexOf("<---")!=-1){ // back in menu
      xy[1]=0;
      load_menu_to_display();
      return;
    }
    if(xy[1]>0 && xy[1]<size_y-1 && menus[xy[0]][xy[1]+1]!=""){ // up in menu hierarchy
      up_in_hierarchy();
      return;
    }

    if(xy[0]==0){ // change mode
      fortress=!fortress;
      if(fortress){
        menus[0][0] = "Mode:Stronghold";
        menus[0][1] = "";
        menus[1][0] = "Teams&Colors   ";
        menus[1][1] = "<---           ";
        menus[1][2] = "Set Teamcolors ";
        menus[1][3] = "";
        menus[2][0] = "Stop Game      ";
        menus[3][0] = "Reset";
        write_color("white");
      } else {
        menus[0][0] = "Mode:Teambase  ";
        menus[1][0] = "Teams&Colors   ";
        menus[1][1] = "<---           ";
        menus[1][2] = "Set Base Team  ";
        menus[1][3] = "Set Teamcolors ";
        menus[2][0] = "Stop Game      ";
        menus[3][0] = "Reset";
      }
      xy[0]=0;
      xy[1]=0;
      load_menu_to_display();
      return;
    }
    if(fortress){
      if(xy[0]==1 && xy[1]==2){ // Set Team Colors
        set_team_colors();
        return;
      }
    } else{
      if(xy[0]==1 && xy[1]==2){ // set base team
        set_base_team();
        return;
      }
      if(xy[0]==1 && xy[1]==3){ // set team colors
        set_team_colors();
        return;
      }
    }
    if(xy[0]==2){ // stop game signal (send 3 times, to ensure, that all players receive it)
      detect_flags = false;
      fortress_current_holder = -1;
      morse_word(stop_game_signal);
      delay(1000);
      morse_word(stop_game_signal);
      delay(1000);
      morse_word(stop_game_signal);
      return;
    }
    if(xy[0]==3){ // reset
      setup();
      return;
    }
  }

  void set_team_colors(){
    button_pushed_id=-1; // disable interrupt button presses
    for(int i=0;i<4;i++){
      lcd.clear();
      String text0 = "Team ";
      text0.concat(i);
      text0.concat("               ---> ");
      text0.concat(possible_colors[team_colors[i]]);
      lcd.print(text0);
      while(true){
        if(digitalRead(up_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(up_pin)==0){
            if(team_colors[i]==0) break;
            team_colors[i]-=1;
            lcd.clear();
            String text1 = "Team ";
            text1.concat(i);
            text1.concat("               ---> ");
            text1.concat(possible_colors[team_colors[i]]);
            lcd.print(text1);
            write_color(possible_colors[team_colors[i]]);
            break;
          }
        }
        if(digitalRead(down_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(down_pin)==0){
            if(team_colors[i]==possible_colors_size-1) break;
            team_colors[i]+=1;
            lcd.clear();
            String text1 = "Team ";
            text1.concat(i);
            text1.concat("               ---> ");
            text1.concat(possible_colors[team_colors[i]]);
            lcd.print(text1);
            write_color(possible_colors[team_colors[i]]);
            break;
          }
        }
        if(digitalRead(ok_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(ok_pin)==0){
            break;
          }
        }
      }
    }
    load_menu_to_display();
    if(fortress){
      write_color("black");
    } else {
      write_color(possible_colors[team_colors[base_team_id]]);
    }
    button_pushed_id=0;
  }

  void set_base_team(){
    button_pushed_id=-1;
    lcd.clear();
    String text="Base owned by:     ---> Team ";
    text.concat(base_team_id);
    lcd.print(text);
    while(true){
      if(digitalRead(up_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(up_pin)==0){
            if(base_team_id==0) break;
            base_team_id-=1;
            lcd.clear();
            String text0="Base owned by:     ---> Team ";
            text0.concat(base_team_id);
            lcd.print(text0);
            break;
          }
        }
        if(digitalRead(down_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(down_pin)==0){
            if(base_team_id==3) break;
            base_team_id+=1;
            lcd.clear();
            String text0="Base owned by:     ---> Team ";
            text0.concat(base_team_id);
            lcd.print(text0);
            break;
          }
        }
        if(digitalRead(ok_pin)==0){
          delay(button_debounce/2);
          if(digitalRead(ok_pin)==0){
            break;
          }
        }
    }
    load_menu_to_display();
    write_color(possible_colors[team_colors[base_team_id]]);
    button_pushed_id=0;
  }
};
Menu menu(false);

// Returns 1 (ODD) or 0 (EVEN) parity
int parity (unsigned char x)
{
    x = x ^ x >> 4;
    x = x ^ x >> 2;
    x = x ^ x >> 1;
    return x & 1;
}

void setup() {
  // init variables
  command_ID = 0b00001; // 5 bit
  base_team_id = 0b0000; // 4 bit
  base_id = 0b0001; // 4 bit
  ir_code_signal = 0 | (command_ID<<10) | (base_team_id<<5) | (base_id<<1);
  signal_send_timestamp = 0;
  fortress = false; // false-> teambase, true -> fortress
  fortress_hold_times[0]=0;
  fortress_hold_times[1]=0;
  fortress_hold_times[2]=0;
  fortress_hold_times[3]=0;
  fortress_hold_timestamps[0]=0;
  fortress_hold_timestamps[1]=0;
  fortress_hold_timestamps[2]=0;
  fortress_hold_timestamps[3]=0;
  catched_flags = 0;
  detect_flags = true;
  fortress_current_holder = -1;
  bytecount=65535;
  I2Cvalue_high=0;
  I2Cvalue_low=0;
  I2CID=0;
  signal_counter=0;
  signal_timestamp=0;
  signal_team=255;
  
  // configure outputs
  pinMode(ir_led, OUTPUT);
  pinMode(fortress_switch, INPUT_PULLUP);
  pinMode(rgb_pin0, OUTPUT);
  pinMode(rgb_pin1, OUTPUT);
  pinMode(rgb_pin2, OUTPUT);
  pinMode(up_pin,INPUT_PULLUP);
  pinMode(down_pin,INPUT_PULLUP);
  pinMode(ok_pin,INPUT_PULLUP);
  PCintPort::attachInterrupt(up_pin, up, FALLING);
  PCintPort::attachInterrupt(down_pin, down, FALLING);
  PCintPort::attachInterrupt(ok_pin, ok, FALLING);

  // fill shot parity bits for each shot byte (even parity) --> 0 --> even number of 1s,  1--> odd number of 1s
  ir_code_signal = (ir_code_signal | parity((ir_code_signal&0xFF00)>>8)<<8);
  ir_code_signal = (ir_code_signal | parity((ir_code_signal&0x00FF)));

  // lcd setup (16 digits, 2 rows)
  lcd.begin(16, 2);
  lcd.createChar(1,point_one);
  lcd.createChar(2,point_two);
  lcd.createChar(3,point_three);
  lcd.createChar(0,point_zero);
  if(fortress){
    write_color("white");
    lcd.print("No base owner");
  } else {
    write_color("red");
    char s[33]="";
    sprintf(s,"Team %02d         %d flags",base_team_id,catched_flags);
    lcd.print(s);
  }

  Wire.begin(10);                // join i2c bus with address #10
  Wire.onReceive(I2CreceiveEvent); // register event
}

void loop() {
  // send PLAYER_BASE_SIGNAL every signal_send_intervall milliseconds
  if(millis()-signal_send_timestamp > signal_send_interval){
    morse_word(ir_code_signal);
    signal_send_timestamp = millis();
  }

  if(button_pushed_id!=0){ // button was pushed
    switch(button_pushed_id){
      case 1:
        if(menu_active) menu.enter();
        else menu.show_menu();
        break;
      case 2:
        menu.up();
        break;
      case 3:
        menu.down();
        break;
    }
    button_pushed_id=0;
  }
  if(menu_active && millis()-button_timestamp>menu_visibility_time){
    show_status();
    menu_active=false;
  }
  if(fortress && !menu_active && millis()-status_timestamp>1000){
    unsigned long t = millis();
    fortress_hold_times[fortress_current_holder]+=t-fortress_hold_timestamps[fortress_current_holder];
    fortress_hold_timestamps[fortress_current_holder]=t;
    show_status();
  }
}

void up(){
  if(button_pushed_id==-1) return;
  if(millis()-button_timestamp > button_debounce){
    button_timestamp = millis();
    button_pushed_id=2;
  }
}

void down(){
  if(button_pushed_id==-1) return;
  if(millis()-button_timestamp > button_debounce){
    button_timestamp = millis();
    button_pushed_id=3;
  }
}

void ok(){
  if(button_pushed_id==-1) return;
  if(millis()-button_timestamp > button_debounce){
    button_timestamp = millis();
    button_pushed_id=1;
  }
}

void I2CreceiveEvent(int howMany) {
  while(Wire.available()>0)
  {
    unsigned char c=Wire.read();
    if(bytecount>1 && c=='s') //signal received
    {
      bytecount=0;
      continue;
    }
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
      bytecount=0;
      if(parity(I2Cvalue_low)==1 || parity(I2Cvalue_high)==1) {I2Cvalue_low=0;I2Cvalue_high=0;bytecount=65535;continue;}
      if(fortress){
        if((I2Cvalue_high&0b10000000)==0){ // shot received --> 
          if(signal_counter==0 || signal_team!=(I2Cvalue_high & 0b01100000) >> 5){
            signal_team = (I2Cvalue_high & 0b01100000) >> 5;
            signal_counter = 1;
            signal_timestamp = millis();
          } else{
            signal_counter++;
            if(millis()-signal_timestamp > fortress_take_duration){
              if(millis()-signal_timestamp < fortress_take_duration_max){
                signal_counter=0;
              } else {
                fortress_current_holder = signal_team;
                fortress_hold_timestamps[fortress_current_holder] = millis();
                write_color(possible_colors[team_colors[fortress_current_holder]]);
                signal_counter=0;
              }
            }
          }
        }
      } else {
        // TODO: Write code corresponding to solution for capture the flag games
        
      }
      I2Cvalue_high=0;
      I2Cvalue_low=0;
      I2CID=0;
    }
  }
}

void show_status(){
  lcd.clear();
  if(fortress){
    lcd.write(byte(0));
    lcd.print(format_fortress_hold_time(fortress_hold_times[0]));
    lcd.print(" ");
    lcd.write(byte(1));
    lcd.print(format_fortress_hold_time(fortress_hold_times[1]));
    lcd.setCursor(0,1);
    lcd.write(byte(2));
    lcd.print(format_fortress_hold_time(fortress_hold_times[2]));
    lcd.print(" ");
    lcd.write(byte(3));
    lcd.print(format_fortress_hold_time(fortress_hold_times[3]));
  } else {
    char s[33]="";
    sprintf(s,"Team %02d         %d flags",base_team_id,catched_flags);
    lcd.print(s);
  }
  status_timestamp = millis();
}

char* format_fortress_hold_time(unsigned long t){
  char text[16]="";
  t/=1000; // convert to seconds
  sprintf(text,"%d:%02d",t/60,t%60);
  return text;
}

void pulse(unsigned long duration)// duration in microseconds
{
  // times = duration/T = duration/(2*delayus)     | micros() will behave erratically from 1-2ms. I don't know, if this touches us, since we are staying under 1ms
                                                    // I am using a for loop here, but I have to check, if this has an impact on the precision of the pulse duration
  /*unsigned long start_time=micros();
  while(micros()-start_time<duration)*/
  for(unsigned int i=0;i<duration/(2*delayus);i++)
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

