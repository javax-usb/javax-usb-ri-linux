
if ( ${?CLASSPATH} ) then
  setenv CLASSPATH "/opt/javax-usb/lib/jsr80_linux.jar:${CLASSPATH}"
else
  setenv CLASSPATH "/opt/javax-usb/lib/jsr80_linux.jar"
fi

setenv CLASSPATH "/opt/javax-usb/etc:${CLASSPATH}"
