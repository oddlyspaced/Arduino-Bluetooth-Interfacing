int  OUTPUT_PIN = 5;//Pin D5 Provides PWM Support on Arduino Nano
char data;
void setup() {
  // put your setup code here, to run once:
  pinMode(OUTPUT_PIN, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly
  if(Serial.available() > 0){
    data = Serial.read();
    Serial.print((int)data+"\n");
    analogWrite(OUTPUT_PIN, (int)data);
  }            
}
