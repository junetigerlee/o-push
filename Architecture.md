# Introduction #

This page describes the software architecture of O-Push.

# Eclipse Server Software #

O-Push is based on [eclipse server-side architecture](http://www.eclipse.org/equinox-portal/tutorials/server-side/demo/).

Eclipse plug-ins and OSGi are used to tie all O-Push parts together.

Two things can be replaced / extended with plugins in o-push:
  * the storage part, where o-push tracks last synchronisation dates, devices, security policies. The default implementation uses jdbc & reads obm configuration to store its data in the OBM database.
  * the backend part, which is the data provider/storage component. Using this extension-point, you can plug any calendar/contact/task/mail source into O-Push. The default implementation uses OBM-Sync for calendar/contact/task data and IMAP for emails.

# OBM Only ? #

Even if O-Push stands for OBM Push, only the default implementation requires an OBM software to work. With the eclipse extension points in place, you can use o-push as an ActiveSync protocol proxy to any groupware you want.