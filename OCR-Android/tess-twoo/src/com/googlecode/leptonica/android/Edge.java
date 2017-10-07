/*
 * Copyright (C) 2014 Robert Theis
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.leptonica.android;

/**
 * Edge detection.
 */
@SuppressWarnings("WeakerAccess")
public class Edge {
    static {
        System.loadLibrary("jpgt");
        System.loadLibrary("pngt");
        System.loadLibrary("lept");
    }

    // Edge orientation flags

    /** Filters for horizontal edges */
    public static final int L_HORIZONTAL_EDGES = 0;

    /** Filters for vertical edges */
    public static final int L_VERTICAL_EDGES = 1;

    /** Filters for all edges */
    public static final int L_ALL_EDGES = 2;

    
    public static Pix pixSobelEdgeFilter(Pix pixs, int orientFlag) {
        if (pixs == null)
            throw new IllegalArgumentException("Source pix must be non-null");
        if (pixs.getDepth() != 8)
            throw new IllegalArgumentException("Source pix depth must be 8bpp");
        if (orientFlag < 0 || orientFlag > 2)
            throw new IllegalArgumentException("Invalid orientation flag");

        long nativePix = nativePixSobelEdgeFilter(pixs.getNativePix(), 
                orientFlag);

        if (nativePix == 0)
            throw new RuntimeException("Failed to perform Sobel edge filter on image");

        return new Pix(nativePix);
    }

    // ***************
    // * NATIVE CODE *
    // ***************

    private static native long nativePixSobelEdgeFilter(long nativePix, int orientFlag);
}
