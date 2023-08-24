# 开机后需要做的事情

TODO
三个tab 1 测试命令 2 管理命令 3管理脚本
1.自定义命令执行
2.常见功能一键开启
3.libbusybox.so 直接在代码里面调用busybox的命令 参考 https://github.com/topjohnwu/Magisk
4.任意远程apk执行
5.最近执行命令缓存
实现方式做成lib，方便一键配置

```
安装apk
sh -c "$(curl -fsSL http://192.168.1.106/installapk.sh)" -c "$(echo http://192.168.1.106/apk/h.apk)"
;echo "echo 0 >/dev/gpio_ctl_drv" >/sdcard/init.txt;reboot

安装二进制文件

sh -c "$(curl -fsSL http://192.168.1.106/installbin.sh)" -c "$(echo http://192.168.1.106/tools/busybox)"

adb shell am broadcast -a com.android.shell2.exec --es "com" "sh/sdcard/q86115mbootinstallbusybox.sh"
sh -c "$(curl -fsSL http://192.168.1.107/installbin.sh)" -c "$(echo https://matt.ucc.asn.au/dropbear/dropbear-2022.82.tar.bz2
busybox bzip2 -cd dropbear-2022.82.tar.bz2 |busybox tar xvf -sh -c "$(curl -fsSL http://192.168.1.107/installapk.sh)" -c "$(echo https://f-droid.org/repo/com.termux_118.apk)"
curl -k https://f-droid.org/repo/com.termux_118.apk
curl -k https://nmap.org/dist/nmap-7.92.tar.bz2 -O
```