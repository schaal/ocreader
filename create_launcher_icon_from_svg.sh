#!/bin/bash

BASE_PATH=$(dirname "$0")
MIPMAP_PATH="$BASE_PATH/app/src/SECTION/res/mipmap"
LOGO_SVG="$BASE_PATH/ic_launcher_SECTION.svg"

convert_size() {
    SECTION=$1
    SIZE=$2
    RES=$3
    DIR="${MIPMAP_PATH/SECTION/$SECTION}-$RES"
    if [[ ! -e "$DIR" ]]
    then
        echo mkdir -p "$DIR"
    fi
    echo convert "${LOGO_SVG/SECTION/$SECTION}" -resize "${SIZE}x${SIZE}" "${DIR}/ic_launcher.png"
}

convert_to_mipmap() {
    SECTION=$1
    convert_size $SECTION 48  mdpi
    convert_size $SECTION 72  hdpi
    convert_size $SECTION 96  xhdpi
    convert_size $SECTION 144 xxhdpi
    convert_size $SECTION 192 xxxhdpi
}

convert_to_mipmap main
convert_to_mipmap debug