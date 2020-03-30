/*
  The MIT License (MIT)

  Copyright (c) 2014 Amulya Khare

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */
package email.schaal.ocreader.util

import androidx.annotation.ColorRes
import email.schaal.ocreader.R
import kotlin.math.abs

/**
 * @author amulya
 */
internal class ColorGenerator private constructor(private val mColors: List<Int>) {
    @ColorRes
    fun getColor(key: Any?): Int {
        return if (key != null) mColors[abs(key.hashCode()) % mColors.size] else mColors[0]
    }

    companion object {
        val MATERIAL = ColorGenerator(listOf(
                R.color.tdb_Red,
                R.color.tdb_Purple,
                R.color.tdb_Indigo,
                R.color.tdb_Blue,
                R.color.tdb_LightBlue,
                R.color.tdb_Cyan,
                R.color.tdb_Teal,
                R.color.tdb_Green,
                R.color.tdb_LightGreen,
                R.color.tdb_Lime,
                R.color.tdb_Amber,
                R.color.tdb_DeepOrange,
                R.color.tdb_Brown,
                R.color.tdb_BlueGrey
        ))
    }

}