EESchema Schematic File Version 2
LIBS:power
LIBS:device
LIBS:transistors
LIBS:conn
LIBS:linear
LIBS:regul
LIBS:74xx
LIBS:cmos4000
LIBS:adc-dac
LIBS:memory
LIBS:xilinx
LIBS:microcontrollers
LIBS:dsp
LIBS:microchip
LIBS:analog_switches
LIBS:motorola
LIBS:texas
LIBS:intel
LIBS:audio
LIBS:interface
LIBS:digital-audio
LIBS:philips
LIBS:display
LIBS:cypress
LIBS:siliconi
LIBS:opto
LIBS:atmel
LIBS:contrib
LIBS:valves
LIBS:switches
LIBS:bbd
LIBS:Arduino_Nano-cache
LIBS:arduino
LIBS:Tagger-cache
EELAYER 25 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title ""
Date ""
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L PCF8574A U1
U 1 1 592B4845
P 7650 2600
F 0 "U1" H 7300 3200 50  0000 L CNN
F 1 "PCF8574A" H 7750 3200 50  0000 L CNN
F 2 "Housings_DIP:DIP-16_W7.62mm" H 7650 2600 50  0001 C CNN
F 3 "" H 7650 2600 50  0001 C CNN
	1    7650 2600
	0    -1   -1   0   
$EndComp
$Comp
L LED_ARGB D4
U 1 1 592B48CA
P 10400 1050
F 0 "D4" H 10400 1420 50  0000 C CNN
F 1 "LED_ARGB" H 10400 700 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 10400 1000 50  0001 C CNN
F 3 "" H 10400 1000 50  0001 C CNN
	1    10400 1050
	1    0    0    -1  
$EndComp
$Comp
L CONN_01X04_FEMALE J4
U 1 1 592B4B54
P 10600 4500
F 0 "J4" H 10600 4900 50  0000 C CNN
F 1 "ToSerialBT" H 10700 4100 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Angled_1x04_Pitch2.54mm" H 10600 4800 50  0001 C CNN
F 3 "" H 10600 4800 50  0001 C CNN
	1    10600 4500
	1    0    0    -1  
$EndComp
$Comp
L R R1
U 1 1 592B4C19
P 9550 4400
F 0 "R1" V 9630 4400 50  0000 C CNN
F 1 "R" V 9550 4400 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9480 4400 50  0001 C CNN
F 3 "" H 9550 4400 50  0001 C CNN
	1    9550 4400
	0    -1   -1   0   
$EndComp
$Comp
L R R2
U 1 1 592B4C68
P 9750 4600
F 0 "R2" V 9830 4600 50  0000 C CNN
F 1 "R" V 9750 4600 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9680 4600 50  0001 C CNN
F 3 "" H 9750 4600 50  0001 C CNN
	1    9750 4600
	1    0    0    -1  
$EndComp
$Comp
L LED D2
U 1 1 592B5015
P 7450 5100
F 0 "D2" H 7450 5200 50  0000 C CNN
F 1 "AmmoLED" H 7450 5000 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm_FlatTop" H 7450 5100 50  0001 C CNN
F 3 "" H 7450 5100 50  0001 C CNN
	1    7450 5100
	0    -1   -1   0   
$EndComp
$Comp
L LED D1
U 1 1 592B50B6
P 8000 5100
F 0 "D1" H 8000 5200 50  0000 C CNN
F 1 "StatusLED" H 8000 5000 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm_FlatTop" H 8000 5100 50  0001 C CNN
F 3 "" H 8000 5100 50  0001 C CNN
	1    8000 5100
	0    -1   -1   0   
$EndComp
$Comp
L GND #PWR01
U 1 1 592B6E86
P 9100 5550
F 0 "#PWR01" H 9100 5300 50  0001 C CNN
F 1 "GND" H 9100 5400 50  0000 C CNN
F 2 "" H 9100 5550 50  0001 C CNN
F 3 "" H 9100 5550 50  0001 C CNN
	1    9100 5550
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR02
U 1 1 592B7930
P 7450 5350
F 0 "#PWR02" H 7450 5100 50  0001 C CNN
F 1 "GND" H 7450 5200 50  0000 C CNN
F 2 "" H 7450 5350 50  0001 C CNN
F 3 "" H 7450 5350 50  0001 C CNN
	1    7450 5350
	1    0    0    -1  
$EndComp
$Comp
L +5V #PWR03
U 1 1 592B7FC1
P 10300 4600
F 0 "#PWR03" H 10300 4450 50  0001 C CNN
F 1 "+5V" H 10300 4740 50  0000 C CNN
F 2 "" H 10300 4600 50  0001 C CNN
F 3 "" H 10300 4600 50  0001 C CNN
	1    10300 4600
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR04
U 1 1 592B80E1
P 10300 4900
F 0 "#PWR04" H 10300 4650 50  0001 C CNN
F 1 "GND" H 10300 4750 50  0000 C CNN
F 2 "" H 10300 4900 50  0001 C CNN
F 3 "" H 10300 4900 50  0001 C CNN
	1    10300 4900
	1    0    0    -1  
$EndComp
$Comp
L LED_ARGB D8
U 1 1 592B91C2
P 1750 1550
F 0 "D8" H 1750 1920 50  0000 C CNN
F 1 "MuzzleFireLED" H 1750 1200 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 1750 1500 50  0001 C CNN
F 3 "" H 1750 1500 50  0001 C CNN
	1    1750 1550
	0    -1   1    0   
$EndComp
$Comp
L R R9
U 1 1 592B9379
P 1550 1000
F 0 "R9" V 1630 1000 50  0000 C CNN
F 1 "R" V 1550 1000 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 1480 1000 50  0001 C CNN
F 3 "" H 1550 1000 50  0001 C CNN
	1    1550 1000
	-1   0    0    1   
$EndComp
$Comp
L R R10
U 1 1 592B93D6
P 1750 1000
F 0 "R10" V 1830 1000 50  0000 C CNN
F 1 "R" V 1750 1000 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 1680 1000 50  0001 C CNN
F 3 "" H 1750 1000 50  0001 C CNN
	1    1750 1000
	-1   0    0    1   
$EndComp
$Comp
L R R11
U 1 1 592B941D
P 1950 1000
F 0 "R11" V 2030 1000 50  0000 C CNN
F 1 "R" V 1950 1000 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 1880 1000 50  0001 C CNN
F 3 "" H 1950 1000 50  0001 C CNN
	1    1950 1000
	-1   0    0    1   
$EndComp
$Comp
L GND #PWR05
U 1 1 592B978E
P 2000 700
F 0 "#PWR05" H 2000 450 50  0001 C CNN
F 1 "GND" H 2000 550 50  0000 C CNN
F 2 "" H 2000 700 50  0001 C CNN
F 3 "" H 2000 700 50  0001 C CNN
	1    2000 700 
	0    -1   -1   0   
$EndComp
$Comp
L LED_ARGB D9
U 1 1 592B9A10
P 900 1550
F 0 "D9" H 900 1920 50  0000 C CNN
F 1 "MuzzleFireLED" H 900 1200 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 900 1500 50  0001 C CNN
F 3 "" H 900 1500 50  0001 C CNN
	1    900  1550
	0    -1   1    0   
$EndComp
$Comp
L Q_NMOS_DGS T3
U 1 1 592BA33C
P 1850 2100
F 0 "T3" H 2050 2150 50  0000 L CNN
F 1 "Q_NMOS_DGS" H 2050 2050 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-126_Vertical" H 2050 2200 50  0001 C CNN
F 3 "" H 1850 2100 50  0001 C CNN
	1    1850 2100
	-1   0    0    1   
$EndComp
$Comp
L +5V #PWR06
U 1 1 592BACB1
P 1600 2350
F 0 "#PWR06" H 1600 2200 50  0001 C CNN
F 1 "+5V" H 1600 2490 50  0000 C CNN
F 2 "" H 1600 2350 50  0001 C CNN
F 3 "" H 1600 2350 50  0001 C CNN
	1    1600 2350
	0    -1   -1   0   
$EndComp
$Comp
L CONN_01X04_FEMALE J3
U 1 1 592BB96B
P 10850 3700
F 0 "J3" H 10850 4100 50  0000 C CNN
F 1 "I2C_ToReceiverModules" V 11050 3650 50  0000 C CNN
F 2 "Connectors_Samtec:SDL-104-X-XX_2x02" H 10850 4000 50  0001 C CNN
F 3 "" H 10850 4000 50  0001 C CNN
	1    10850 3700
	1    0    0    -1  
$EndComp
$Comp
L R R4
U 1 1 592BBA42
P 9850 3550
F 0 "R4" V 9930 3550 50  0000 C CNN
F 1 "2k" V 9850 3550 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9780 3550 50  0001 C CNN
F 3 "" H 9850 3550 50  0001 C CNN
	1    9850 3550
	1    0    0    -1  
$EndComp
$Comp
L R R3
U 1 1 592BBA9B
P 10100 3550
F 0 "R3" V 10180 3550 50  0000 C CNN
F 1 "2k" V 10100 3550 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 10030 3550 50  0001 C CNN
F 3 "" H 10100 3550 50  0001 C CNN
	1    10100 3550
	1    0    0    -1  
$EndComp
$Comp
L +5V #PWR07
U 1 1 592BBD67
P 10400 3300
F 0 "#PWR07" H 10400 3150 50  0001 C CNN
F 1 "+5V" H 10400 3440 50  0000 C CNN
F 2 "" H 10400 3300 50  0001 C CNN
F 3 "" H 10400 3300 50  0001 C CNN
	1    10400 3300
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR08
U 1 1 592BBDAF
P 10400 3600
F 0 "#PWR08" H 10400 3350 50  0001 C CNN
F 1 "GND" H 10400 3450 50  0000 C CNN
F 2 "" H 10400 3600 50  0001 C CNN
F 3 "" H 10400 3600 50  0001 C CNN
	1    10400 3600
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR09
U 1 1 592BDBC5
P 9150 2000
F 0 "#PWR09" H 9150 1750 50  0001 C CNN
F 1 "GND" H 9150 1850 50  0000 C CNN
F 2 "" H 9150 2000 50  0001 C CNN
F 3 "" H 9150 2000 50  0001 C CNN
	1    9150 2000
	1    0    0    -1  
$EndComp
$Comp
L R R12
U 1 1 592BDEC5
P 9350 850
F 0 "R12" V 9430 850 50  0000 C CNN
F 1 "R" V 9350 850 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9280 850 50  0001 C CNN
F 3 "" H 9350 850 50  0001 C CNN
	1    9350 850 
	0    -1   -1   0   
$EndComp
$Comp
L R R13
U 1 1 592BDF10
P 9350 1050
F 0 "R13" V 9430 1050 50  0000 C CNN
F 1 "R" V 9350 1050 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9280 1050 50  0001 C CNN
F 3 "" H 9350 1050 50  0001 C CNN
	1    9350 1050
	0    -1   -1   0   
$EndComp
$Comp
L R R14
U 1 1 592BDF6F
P 9350 1250
F 0 "R14" V 9430 1250 50  0000 C CNN
F 1 "R" V 9350 1250 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 9280 1250 50  0001 C CNN
F 3 "" H 9350 1250 50  0001 C CNN
	1    9350 1250
	0    -1   -1   0   
$EndComp
$Comp
L +5V #PWR010
U 1 1 592BF02D
P 6800 2600
F 0 "#PWR010" H 6800 2450 50  0001 C CNN
F 1 "+5V" H 6800 2740 50  0000 C CNN
F 2 "" H 6800 2600 50  0001 C CNN
F 3 "" H 6800 2600 50  0001 C CNN
	1    6800 2600
	0    -1   -1   0   
$EndComp
$Comp
L GND #PWR011
U 1 1 592BF25A
P 8450 2600
F 0 "#PWR011" H 8450 2350 50  0001 C CNN
F 1 "GND" H 8450 2450 50  0000 C CNN
F 2 "" H 8450 2600 50  0001 C CNN
F 3 "" H 8450 2600 50  0001 C CNN
	1    8450 2600
	0    -1   -1   0   
$EndComp
$Comp
L GND #PWR012
U 1 1 592BF3CB
P 7800 3200
F 0 "#PWR012" H 7800 2950 50  0001 C CNN
F 1 "GND" H 7800 3050 50  0000 C CNN
F 2 "" H 7800 3200 50  0001 C CNN
F 3 "" H 7800 3200 50  0001 C CNN
	1    7800 3200
	0    -1   -1   0   
$EndComp
$Comp
L +5V #PWR013
U 1 1 592BD3E8
P 10800 1050
F 0 "#PWR013" H 10800 900 50  0001 C CNN
F 1 "+5V" H 10800 1190 50  0000 C CNN
F 2 "" H 10800 1050 50  0001 C CNN
F 3 "" H 10800 1050 50  0001 C CNN
	1    10800 1050
	0    1    1    0   
$EndComp
$Comp
L MCP6001R U2
U 1 1 592BF179
P 6300 1750
F 0 "U2" H 6350 1950 50  0000 C CNN
F 1 "MCP6001R" H 6500 1550 50  0000 C CNN
F 2 "Housings_DIP:DIP-8_W7.62mm" H 6250 1850 50  0001 C CNN
F 3 "" H 6350 1950 50  0001 C CNN
	1    6300 1750
	0    1    -1   0   
$EndComp
$Comp
L R R8
U 1 1 592BF364
P 5000 3200
F 0 "R8" V 5080 3200 50  0000 C CNN
F 1 "R" V 5000 3200 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 4930 3200 50  0001 C CNN
F 3 "" H 5000 3200 50  0001 C CNN
	1    5000 3200
	-1   0    0    1   
$EndComp
$Comp
L R R7
U 1 1 592BF3C9
P 5700 1450
F 0 "R7" V 5780 1450 50  0000 C CNN
F 1 "R" V 5700 1450 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 5630 1450 50  0001 C CNN
F 3 "" H 5700 1450 50  0001 C CNN
	1    5700 1450
	-1   0    0    1   
$EndComp
$Comp
L R R6
U 1 1 592BF41E
P 5350 1450
F 0 "R6" V 5430 1450 50  0000 C CNN
F 1 "R" V 5350 1450 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 5280 1450 50  0001 C CNN
F 3 "" H 5350 1450 50  0001 C CNN
	1    5350 1450
	-1   0    0    1   
$EndComp
$Comp
L R R5
U 1 1 592BF68C
P 5000 1450
F 0 "R5" V 5080 1450 50  0000 C CNN
F 1 "R" V 5000 1450 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 4930 1450 50  0001 C CNN
F 3 "" H 5000 1450 50  0001 C CNN
	1    5000 1450
	-1   0    0    1   
$EndComp
$Comp
L +5V #PWR014
U 1 1 592C29DC
P 5900 1850
F 0 "#PWR014" H 5900 1700 50  0001 C CNN
F 1 "+5V" H 5900 1990 50  0000 C CNN
F 2 "" H 5900 1850 50  0001 C CNN
F 3 "" H 5900 1850 50  0001 C CNN
	1    5900 1850
	0    -1   -1   0   
$EndComp
$Comp
L GND #PWR015
U 1 1 592C2A3A
P 6700 1850
F 0 "#PWR015" H 6700 1600 50  0001 C CNN
F 1 "GND" H 6700 1700 50  0000 C CNN
F 2 "" H 6700 1850 50  0001 C CNN
F 3 "" H 6700 1850 50  0001 C CNN
	1    6700 1850
	0    -1   -1   0   
$EndComp
$Comp
L CQY99 D3
U 1 1 592C2BE3
P 6450 1100
F 0 "D3" H 6470 1170 50  0000 L CNN
F 1 "CQY99" H 6410 990 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm_FlatTop" H 6450 1275 50  0001 C CNN
F 3 "" H 6400 1100 50  0000 C CNN
	1    6450 1100
	-1   0    0    1   
$EndComp
$Comp
L GND #PWR016
U 1 1 592C2F1A
P 6700 1100
F 0 "#PWR016" H 6700 850 50  0001 C CNN
F 1 "GND" H 6700 950 50  0000 C CNN
F 2 "" H 6700 1100 50  0001 C CNN
F 3 "" H 6700 1100 50  0001 C CNN
	1    6700 1100
	0    -1   -1   0   
$EndComp
$Comp
L Laserdiode_1A3C LD1
U 1 1 5966F646
P 4100 1200
F 0 "LD1" H 4050 1375 50  0000 C CNN
F 1 "Lasergunsight_diode" H 4050 1100 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02_Pitch2.54mm" H 4000 1175 50  0001 C CNN
F 3 "" H 4130 1000 50  0001 C CNN
	1    4100 1200
	0    -1   1    0   
$EndComp
$Comp
L +5V #PWR017
U 1 1 5966F785
P 3400 1850
F 0 "#PWR017" H 3400 1700 50  0001 C CNN
F 1 "+5V" H 3400 1990 50  0000 C CNN
F 2 "" H 3400 1850 50  0001 C CNN
F 3 "" H 3400 1850 50  0001 C CNN
	1    3400 1850
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR018
U 1 1 5966F7E7
P 4100 750
F 0 "#PWR018" H 4100 500 50  0001 C CNN
F 1 "GND" H 4100 600 50  0000 C CNN
F 2 "" H 4100 750 50  0001 C CNN
F 3 "" H 4100 750 50  0001 C CNN
	1    4100 750 
	1    0    0    1   
$EndComp
$Comp
L SW_Push SW1
U 1 1 5A52618C
P 9350 5050
F 0 "SW1" H 9400 5150 50  0000 L CNN
F 1 "SW_Push" H 9350 4990 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02_Pitch2.54mm" H 9350 5250 50  0001 C CNN
F 3 "" H 9350 5250 50  0001 C CNN
	1    9350 5050
	0    -1   -1   0   
$EndComp
$Comp
L SW_Push SW2
U 1 1 5A526211
P 8850 5050
F 0 "SW2" H 8900 5150 50  0000 L CNN
F 1 "SW_Push" H 8850 4990 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02_Pitch2.54mm" H 8850 5250 50  0001 C CNN
F 3 "" H 8850 5250 50  0001 C CNN
	1    8850 5050
	0    -1   -1   0   
$EndComp
$Comp
L ArduinoNano A1
U 1 1 5A5288C6
P 1650 3300
F 0 "A1" H 1250 4050 60  0000 C CNN
F 1 "ArduinoNano" H 2900 1400 60  0000 C CNN
F 2 "Arduino:ArduinoNano" H 1650 3300 60  0001 C CNN
F 3 "" H 1650 3300 60  0001 C CNN
	1    1650 3300
	1    0    0    -1  
$EndComp
Wire Wire Line
	3400 4450 9350 4450
Wire Wire Line
	3400 4550 8850 4550
Wire Wire Line
	3400 4350 9400 4350
Wire Wire Line
	3400 4250 9400 4250
Wire Wire Line
	10300 4900 10300 4800
Wire Wire Line
	9750 4800 10500 4800
Wire Wire Line
	10300 4600 10500 4600
Wire Wire Line
	9700 4400 10500 4400
Wire Wire Line
	9750 4450 9750 4400
Connection ~ 9750 4400
Wire Wire Line
	9750 4750 9750 4800
Connection ~ 10300 4800
Wire Wire Line
	1550 1150 1550 1350
Wire Wire Line
	1950 1150 1950 1350
Wire Wire Line
	1550 700  1550 850 
Wire Wire Line
	1750 850  1750 700 
Connection ~ 1750 700 
Wire Wire Line
	1950 700  1950 850 
Connection ~ 1950 700 
Connection ~ 1950 1200
Connection ~ 1750 1250
Wire Wire Line
	1750 1150 1750 1350
Connection ~ 1550 1300
Wire Wire Line
	1750 1750 1750 1900
Wire Wire Line
	900  1750 900  1800
Wire Wire Line
	900  1800 2650 1800
Connection ~ 1750 1800
Wire Wire Line
	1600 2350 1750 2350
Wire Wire Line
	1550 700  2000 700 
Wire Wire Line
	8000 5350 8000 5250
Wire Wire Line
	3400 4950 7450 4950
Wire Wire Line
	3400 4850 8000 4850
Wire Wire Line
	8000 4850 8000 4950
Wire Wire Line
	10750 3600 10400 3600
Wire Wire Line
	10400 3400 10400 3300
Wire Wire Line
	9850 3400 10750 3400
Wire Wire Line
	10100 3800 10100 3700
Connection ~ 10400 3400
Connection ~ 10100 3400
Wire Wire Line
	9850 3700 9850 4000
Connection ~ 9850 4000
Connection ~ 10100 3800
Wire Wire Line
	7650 900  7650 850 
Wire Wire Line
	9150 1250 9150 1500
Wire Wire Line
	8400 1050 8400 1200
Wire Wire Line
	7650 1300 7650 1900
Wire Wire Line
	7650 1900 9150 1900
Wire Wire Line
	8400 1600 8400 1900
Connection ~ 8400 1900
Wire Wire Line
	9150 1900 9150 2000
Wire Wire Line
	7450 1700 8850 1700
Wire Wire Line
	6950 2600 6800 2600
Wire Wire Line
	8450 2600 8350 2600
Wire Wire Line
	7550 3200 7800 3200
Wire Wire Line
	7750 3200 7750 3100
Wire Wire Line
	7650 3200 7650 3100
Connection ~ 7750 3200
Wire Wire Line
	7550 3200 7550 3100
Connection ~ 7650 3200
Wire Wire Line
	1750 2350 1750 2300
Wire Wire Line
	9150 1250 9200 1250
Wire Wire Line
	8400 1050 9200 1050
Wire Wire Line
	7650 850  9200 850 
Wire Wire Line
	9500 850  10200 850 
Wire Wire Line
	9500 1050 10200 1050
Wire Wire Line
	9500 1250 10200 1250
Wire Wire Line
	10600 1050 10800 1050
Wire Wire Line
	7250 2100 7250 1100
Wire Wire Line
	7250 1100 7350 1100
Wire Wire Line
	8100 1400 7350 1400
Wire Wire Line
	7350 1400 7350 2100
Wire Wire Line
	7450 1700 7450 2100
Wire Wire Line
	5700 2400 5700 1600
Wire Wire Line
	5350 2050 5350 1600
Wire Wire Line
	5000 1600 5000 1700
Wire Wire Line
	6300 1100 6300 1450
Wire Wire Line
	5000 1100 6350 1100
Wire Wire Line
	5700 1100 5700 1300
Wire Wire Line
	5350 1300 5350 1100
Connection ~ 5700 1100
Wire Wire Line
	5000 1300 5000 1100
Connection ~ 5350 1100
Wire Wire Line
	5000 2100 5000 3050
Wire Wire Line
	5000 3000 6200 3000
Wire Wire Line
	5700 2800 5700 3000
Connection ~ 5700 3000
Wire Wire Line
	5350 2450 5350 3000
Connection ~ 5350 3000
Wire Wire Line
	6700 1850 6600 1850
Connection ~ 6300 1100
Wire Wire Line
	6650 1100 6700 1100
Wire Wire Line
	8050 3800 10750 3800
Wire Wire Line
	9350 4450 9350 4850
Wire Wire Line
	8850 4550 8850 4850
Wire Wire Line
	8850 5250 8850 5550
Wire Wire Line
	8850 5550 9350 5550
Wire Wire Line
	9350 5550 9350 5250
Connection ~ 9100 5550
Wire Wire Line
	9400 4350 9400 4400
Wire Wire Line
	9400 4250 9400 4200
Wire Wire Line
	9400 4200 10500 4200
Wire Wire Line
	8050 3900 8050 3800
Wire Wire Line
	4100 900  4100 750 
Wire Wire Line
	3450 1900 3400 1900
Wire Wire Line
	3400 1900 3400 1850
Wire Wire Line
	3400 4000 10750 4000
Wire Wire Line
	3400 3900 8050 3900
Wire Wire Line
	3550 2100 3550 2750
Wire Wire Line
	3550 2750 3400 2750
Wire Wire Line
	6200 3000 6200 2050
Connection ~ 5000 3000
Wire Wire Line
	7450 5350 7450 5250
Wire Wire Line
	7450 5350 8000 5350
$Comp
L LED_ARGB D7
U 1 1 5A53C5DB
P 2650 1550
F 0 "D7" H 2650 1920 50  0000 C CNN
F 1 "MuzzleFireLED" H 2650 1200 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 2650 1500 50  0001 C CNN
F 3 "" H 2650 1500 50  0001 C CNN
	1    2650 1550
	0    -1   1    0   
$EndComp
Wire Wire Line
	2650 1800 2650 1750
$Comp
L +5V #PWR019
U 1 1 5A53BD80
P 600 2850
F 0 "#PWR019" H 600 2700 50  0001 C CNN
F 1 "+5V" H 600 2990 50  0000 C CNN
F 2 "" H 600 2850 50  0001 C CNN
F 3 "" H 600 2850 50  0001 C CNN
	1    600  2850
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR020
U 1 1 5A53BDE0
P 750 4400
F 0 "#PWR020" H 750 4150 50  0001 C CNN
F 1 "GND" H 750 4250 50  0000 C CNN
F 2 "" H 750 4400 50  0001 C CNN
F 3 "" H 750 4400 50  0001 C CNN
	1    750  4400
	1    0    0    -1  
$EndComp
Wire Wire Line
	600  2850 600  2950
Wire Wire Line
	600  2950 950  2950
Wire Wire Line
	750  4250 750  4400
Wire Wire Line
	750  4350 950  4350
Wire Wire Line
	750  4250 950  4250
Connection ~ 750  4350
$Comp
L R R15
U 1 1 5A53C754
P 4100 1700
F 0 "R15" V 4180 1700 50  0000 C CNN
F 1 "R" V 4100 1700 50  0000 C CNN
F 2 "Resistors_THT:R_Axial_DIN0207_L6.3mm_D2.5mm_P2.54mm_Vertical" V 4030 1700 50  0001 C CNN
F 3 "" H 4100 1700 50  0001 C CNN
	1    4100 1700
	1    0    0    -1  
$EndComp
Wire Wire Line
	3650 2200 3650 2850
Wire Wire Line
	3850 1900 4100 1900
Wire Wire Line
	4100 1900 4100 1850
Wire Wire Line
	4100 1550 4100 1400
Wire Wire Line
	2050 2100 3550 2100
Wire Wire Line
	3650 2850 3400 2850
Wire Wire Line
	6000 1850 5900 1850
Wire Wire Line
	6600 1850 6600 2050
Wire Wire Line
	6600 2050 6400 2050
Wire Wire Line
	5000 4750 5000 3350
Wire Wire Line
	4350 2950 4350 1900
Wire Wire Line
	4350 1900 4700 1900
Wire Wire Line
	4550 3050 4550 2250
Wire Wire Line
	4550 2250 5050 2250
Wire Wire Line
	4750 3150 4750 2600
Wire Wire Line
	4750 2600 5400 2600
$Comp
L BC237 T7
U 1 1 5A53D2BD
P 4900 1900
F 0 "T7" H 5100 1975 50  0000 L CNN
F 1 "BC237" H 5100 1900 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 5100 1825 50  0001 L CIN
F 3 "" H 4900 1900 50  0001 L CNN
	1    4900 1900
	1    0    0    1   
$EndComp
$Comp
L BC237 T8
U 1 1 5A53D5BC
P 5250 2250
F 0 "T8" H 5450 2325 50  0000 L CNN
F 1 "BC237" H 5450 2250 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 5450 2175 50  0001 L CIN
F 3 "" H 5250 2250 50  0001 L CNN
	1    5250 2250
	1    0    0    1   
$EndComp
$Comp
L BC237 T9
U 1 1 5A53D657
P 5600 2600
F 0 "T9" H 5800 2675 50  0000 L CNN
F 1 "BC237" H 5800 2600 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 5800 2525 50  0001 L CIN
F 3 "" H 5600 2600 50  0001 L CNN
	1    5600 2600
	1    0    0    1   
$EndComp
$Comp
L BC237 T4
U 1 1 5A53FFE3
P 7550 1100
F 0 "T4" H 7750 1175 50  0000 L CNN
F 1 "BC237" H 7750 1100 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 7750 1025 50  0001 L CIN
F 3 "" H 7550 1100 50  0001 L CNN
	1    7550 1100
	1    0    0    1   
$EndComp
$Comp
L BC237 T5
U 1 1 5A5413C3
P 8300 1400
F 0 "T5" H 8500 1475 50  0000 L CNN
F 1 "BC237" H 8500 1400 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 8500 1325 50  0001 L CIN
F 3 "" H 8300 1400 50  0001 L CNN
	1    8300 1400
	1    0    0    1   
$EndComp
$Comp
L BC237 T6
U 1 1 5A5414DF
P 9050 1700
F 0 "T6" H 9250 1775 50  0000 L CNN
F 1 "BC237" H 9250 1700 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 9250 1625 50  0001 L CIN
F 3 "" H 9050 1700 50  0001 L CNN
	1    9050 1700
	1    0    0    1   
$EndComp
$Comp
L BC237 T2
U 1 1 5A541BC5
P 3650 2000
F 0 "T2" H 3850 2075 50  0000 L CNN
F 1 "BC237" H 3850 2000 50  0000 L CNN
F 2 "TO_SOT_Packages_THT:TO-92_Molded_Narrow" H 3850 1925 50  0001 L CIN
F 3 "" H 3650 2000 50  0001 L CNN
	1    3650 2000
	0    -1   -1   0   
$EndComp
Wire Wire Line
	5000 4750 3400 4750
Wire Wire Line
	4750 3150 3400 3150
Wire Wire Line
	4550 3050 3400 3050
Wire Wire Line
	4350 2950 3400 2950
Wire Wire Line
	7350 3100 7350 3900
Connection ~ 7350 3900
Wire Wire Line
	7250 3100 7250 4000
Connection ~ 7250 4000
$Comp
L LED_ARGB D5
U 1 1 5A540C71
P 10400 1850
F 0 "D5" H 10400 2220 50  0000 C CNN
F 1 "LED_ARGB" H 10400 1500 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 10400 1800 50  0001 C CNN
F 3 "" H 10400 1800 50  0001 C CNN
	1    10400 1850
	1    0    0    -1  
$EndComp
$Comp
L LED_ARGB D6
U 1 1 5A540D43
P 10400 2650
F 0 "D6" H 10400 3020 50  0000 C CNN
F 1 "LED_ARGB" H 10400 2300 50  0000 C CNN
F 2 "LEDs:LED_D5.0mm-4" H 10400 2600 50  0001 C CNN
F 3 "" H 10400 2600 50  0001 C CNN
	1    10400 2650
	1    0    0    -1  
$EndComp
Wire Wire Line
	10100 1250 10100 2850
Wire Wire Line
	10100 2050 10200 2050
Connection ~ 10100 1250
Wire Wire Line
	9950 1050 9950 2650
Wire Wire Line
	9950 1850 10200 1850
Connection ~ 9950 1050
Wire Wire Line
	9800 850  9800 2450
Wire Wire Line
	9800 1650 10200 1650
Connection ~ 9800 850 
Wire Wire Line
	10100 2850 10200 2850
Connection ~ 10100 2050
Wire Wire Line
	9950 2650 10200 2650
Connection ~ 9950 1850
Wire Wire Line
	9800 2450 10200 2450
Connection ~ 9800 1650
Wire Wire Line
	10700 1050 10700 2650
Wire Wire Line
	10700 1850 10600 1850
Connection ~ 10700 1050
Wire Wire Line
	10700 2650 10600 2650
Connection ~ 10700 1850
Wire Wire Line
	1100 1200 2850 1200
Wire Wire Line
	2850 1200 2850 1350
Wire Wire Line
	2650 1250 2650 1350
Wire Wire Line
	900  1250 2650 1250
Wire Wire Line
	700  1300 2450 1300
Wire Wire Line
	2450 1300 2450 1350
Wire Wire Line
	1100 1200 1100 1350
Wire Wire Line
	900  1250 900  1350
Wire Wire Line
	700  1300 700  1350
$EndSCHEMATC
