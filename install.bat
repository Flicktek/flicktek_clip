adb -s %1 forward tcp:4444 localabstract:/adb-hub
adb -s %1 connect 127.0.0.1:4444
rem adb -s 127.0.0.1:4444 uninstall com.flicktek.clip
adb -s 127.0.0.1:4444 install %2 

adb -s 127.0.0.1:4444 shell am start -n com.flicktek.clip/.MainActivity
