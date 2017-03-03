#!/bin/bash

BASE_PATH="../$(dirname "$0")"
MIPMAP_PATH="$BASE_PATH/app/src/SECTION/res/mipmap"
LOGO_SVG="$BASE_PATH/icons/ic_launcher_main.svg"

convert_size() {
    SECTION=$1
    SVG="$2"
    SIZE=$3
    RES=$4
    DIR="${MIPMAP_PATH/SECTION/$SECTION}-$RES"

    mkdir -p "$DIR"

    convert "$SVG" -resize "${SIZE}x${SIZE}" "${DIR}/ic_launcher.png"
}

convert_to_mipmap() {
    SECTION=$1
    SVG="$2"

    convert_size ${SECTION} "$SVG" 48  mdpi
    convert_size ${SECTION} "$SVG" 72  hdpi
    convert_size ${SECTION} "$SVG" 96  xhdpi
    convert_size ${SECTION} "$SVG" 144 xxhdpi
    convert_size ${SECTION} "$SVG" 192 xxxhdpi
}

convert_to_mipmap main "$LOGO_SVG"

# create temporary directory and put the debug.svg with a different circle background there
#TMPPATH="$(mktemp --directory)"
#DEBUG_SVGPATH="$TMPPATH/debug.svg"

#sed -e 's/style="fill:#0288d1/style="fill:#f44336/' "$LOGO_SVG" > "$DEBUG_SVGPATH"

#convert_to_mipmap debug "$DEBUG_SVGPATH"

#rm -f "$DEBUG_SVGPATH"
#rmdir "$TMPPATH"