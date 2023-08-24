#!/system/bin/sh
########################
# DESCRIPTION:
#    Reboot Android
#
# NOTE:Run this script as root or system
#
setprop sys.powerctl reboot
sleep 2
reboot