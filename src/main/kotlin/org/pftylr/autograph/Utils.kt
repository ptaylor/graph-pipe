/*
 * MIT License
 *
 * Copyright (c) 2017 ptaylor
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
 *
 */

package org.pftylr.autograph;

fun splitIntoStrings(s: String): List<String> {
   
    var strings: List<String>?

    if (s.contains(",")) {
        strings = s.split(",").map {it.trim()}
    } else {
        strings = s.trim().split("\\s+".toRegex())
    }
    if (strings.size == 1 && strings[0].length == 0) {
        return listOf<String>()
    } else {
        return strings
    }
}


fun toNumList(s: List<String>): List<Double>? {

    val d: MutableList<Double> = mutableListOf<Double>()

    try {
        s.forEach { v ->
            d.add(v.toDouble())
        }
    } catch (e: NumberFormatException) {
        return null
    }

    return d
}
