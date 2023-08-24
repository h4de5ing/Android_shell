#!/system/bin/sh
########################
# DESCRIPTION:
#    Reboot Android into recovery
#
# NOTE:Run this script as root or system
#
setprop ctl.start pre-recovery
sleep 30
reboot recovery