#!/system/bin/sh
########################
# DESCRIPTION:
#    install remote apk in http&https
#    安装apk
#    usage:  sh installapk.sh http://192.168.1.106/apk/app.apk
#            sh -c "$(curl -fsSL http://192.168.1.106/installapk.sh)" -c "$(echo http://192.168.1.106/apk/shell2.apk)"
# wc acdm.apk
# cat /system/media/acdm.apk | pm install -d -t -S 24288027
#

url=$1
fileName="$(echo "$url" | sed 's/.*\///')"
localFile="/data/local/tmp/$fileName"
echo "download-> ${localFile}"
curl -k -s -o "$localFile" "$url"
#length="$(wc -c "$localFile" | sed 's/ .*$//')"
if [ -f "$localFile" ]; then
  #  echo "fileLength-> ${length}"
  #  cat $localFile | pm install -d -t -S $length
  pm install "$localFile"
  rm "$localFile"
fi
