#!/system/bin/sh
########################
# DESCRIPTION:
#    Reboot systemui app
#
# NOTE:Run this script as root or system
#
service call activity 42 s16 com.android.systemui
am startservice -n com.android.systemui/.SystemUIService