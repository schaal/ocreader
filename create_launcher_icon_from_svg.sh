#!/bin/bash

BASE_PATH=$(dirname "$0")
MIPMAP_PATH="$BASE_PATH/app/src/main/res/mipmap"
LOGO_SVG="$BASE_PATH/ic_launcher.svg"

convert_size() {
    SIZE=$1
    RES=$2
    convert "$LOGO_SVG" -resize "${SIZE}x${SIZE}" "$MIPMAP_PATH-$RES/ic_launcher.png"
}

convert_size 48  mdpi
convert_size 72  hdpi
convert_size 96  xhdpi
convert_size 144 xxhdpi
convert_size 192 xxxhdpi
