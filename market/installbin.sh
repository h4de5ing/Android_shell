#!/system/bin/sh
########################
# DESCRIPTION:
#    install binaries into device
#    安装二进制执行文件
#    usage: sh installbin.sh http://192.168.1.106/bin/busybox
#           sh -c "$(curl -fsSL http://192.168.1.106/installbin.sh)" -c "$(echo http://192.168.1.106/bin/busybox)"
#
#
url=$1
fileName="$(echo "$url" | sed 's/.*\///')"
localFile="/data/local/tmp/$fileName"
echo "download bin->$localFile"
if [ ! -f "$localFile" ]; then
  curl -k -s -o "${localFile}" "${url}"
  chmod 777 "$localFile"
  export PATH=/data/local/tmp:$PATH
fi
