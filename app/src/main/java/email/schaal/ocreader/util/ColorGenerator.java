/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Amulya Khare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package email.schaal.ocreader.util;

import com.mikepenz.materialize.color.Material;

import java.util.Arrays;
import java.util.List;

/**
 * @author amulya
 */
public class ColorGenerator {
    public final static ColorGenerator MATERIAL = new ColorGenerator(Arrays.asList(
                Material.Red._900.getAsColor(),
                Material.Purple._900.getAsColor(),
                Material.Indigo._900.getAsColor(),
                Material.Blue._900.getAsColor(),
                Material.LightBlue._900.getAsColor(),
                Material.Cyan._900.getAsColor(),
                Material.Teal._900.getAsColor(),
                Material.Green._900.getAsColor(),
                Material.LightGreen._900.getAsColor(),
                Material.Lime._900.getAsColor(),
                Material.Amber._900.getAsColor(),
                Material.DeepOrange._900.getAsColor(),
                Material.Brown._900.getAsColor(),
                Material.BlueGrey._900.getAsColor()
        ));

    private final List<Integer> mColors;

    private ColorGenerator(List<Integer> colorList) {
        mColors = colorList;
    }

    public int getColor(Object key) {
        if(key != null)
            return mColors.get(Math.abs(key.hashCode()) % mColors.size());
        else
            return mColors.get(0);
    }
}