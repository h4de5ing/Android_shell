#!/system/bin/sh
########################
# DESCRIPTION:
#    shutdown Android
#
# NOTE:Run this script as root or system
#
setprop sys.powerctl shutdown
sleep 3
reboot -p