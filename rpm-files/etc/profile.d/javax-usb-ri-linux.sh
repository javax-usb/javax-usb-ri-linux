
if test -n "$CLASSPATH" ; then
  CLASSPATH="/opt/javax-usb/lib/jsr80_linux.jar:${CLASSPATH}"
else
  CLASSPATH="/opt/javax-usb/lib/jsr80_linux.jar"
fi

CLASSPATH="/opt/javax-usb/etc:${CLASSPATH}"

export CLASSPATH
