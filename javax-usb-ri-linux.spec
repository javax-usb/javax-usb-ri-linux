Summary: javax.usb Linux Implementation
Name: javax-usb-ri-linux
Version: 1.0.1
Release: 1
Copyright: (c)Copyright International Business Machines Corporation 2001. All rights reserved.
Group: System Environment/Libraries
%description
Linux implementation of the javax.usb API using the Common Reference Implementation.
%files
%defattr(644, root, root)
/opt/javax-usb/etc/javax.usb.properties
/etc/profile.d/javax-usb-ri-linux.*
%defattr(755, root, root)
/opt/javax-usb/lib/jsr80*.jar
/usr/lib/libJavaxUsb*
