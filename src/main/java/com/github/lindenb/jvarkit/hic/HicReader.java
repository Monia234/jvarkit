/*
The MIT License (MIT)

Copyright (c) 2019 Pierre Lindenbaum

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
package com.github.lindenb.jvarkit.hic;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.Locatable;

public interface HicReader extends Closeable {
	
/** get source of this reader (path, url...) or null */
public Object getSource();	

/** get dictionary */
public SAMSequenceDictionary getDictionary();

/** get genome build */
public String getBuild();

/** get attributes */
public Map<String,String> getAttributes();

/** get version of hic format */
public int getVersion();

/** get the base pair resolutions */
public Set<Integer> getBasePairResolutions();

/** get the fragment resolutions */
public Set<Integer> getFragmentResolutions();


public Optional<HicMatrix> query(
		final String interval1,
		final String interval2,
		final Normalization norm,
		final int binsize, 
		final Unit unit
		);


public Optional<HicMatrix> query(
		final Locatable interval1,
		final Locatable interval2,
		final Normalization norm,
		final int binsize, 
		final Unit unit
		);
}
