package com.github.lindenb.jvarkit.variant.swing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.lindenb.jvarkit.util.swing.AbstractGenericTable;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;

@SuppressWarnings("serial")
public class SwingVCFGenotypesTableModel extends AbstractGenericTable<Genotype>{
private final Vector<ColumnInfo> columns = new Vector<>();
private class ColumnInfo {
	String name;
	Class<?> clazz;
	Function<Genotype, Object> extractor;
	}

public SwingVCFGenotypesTableModel() {
	super();
	}

public void setVariant(final VariantContext ctx) {
	final List<Genotype> genotypes;
    this.columns.clear();

	if(ctx==null || !ctx.hasGenotypes()) {
		genotypes = Collections.emptyList();
		}
	else {
		genotypes = ctx.getGenotypes();
	    
	    final Vector<ColumnInfo> columns = new Vector<>();
	    ColumnInfo ci = new ColumnInfo();
	    ci.clazz = String.class;
	    ci.name= "Sample";
	    ci.extractor = GT->GT.getSampleName();
	    columns.add(ci);
	    if(genotypes.stream().anyMatch(G->G.isAvailable())) {
	    	ci = new ColumnInfo();
	        ci.clazz = String.class;
	        ci.name= VCFConstants.GENOTYPE_KEY;
	        ci.extractor = GT->GT.getGenotypeString();
	        columns.add(ci);
	    }
	    if(genotypes.stream().anyMatch(G->G.hasDP())) {
	    	ci = new ColumnInfo();
	        ci.clazz = String.class;
	        ci.name= VCFConstants.DEPTH_KEY;
	        ci.extractor = GT->GT.hasDP()?GT.getDP():null;
	        columns.add(ci);
	    }
	    if(genotypes.stream().anyMatch(G->G.hasGQ())) {
	    	ci = new ColumnInfo();
	        ci.clazz = String.class;
	        ci.name= VCFConstants.GENOTYPE_QUALITY_KEY;
	        ci.extractor = GT->GT.hasGQ()?GT.getGQ():null;
	        columns.add(ci);
	    }
	    if(genotypes.stream().anyMatch(G->G.hasAD())) {
	    	ci = new ColumnInfo();
	        ci.clazz = String.class;
	        ci.name= VCFConstants.GENOTYPE_ALLELE_DEPTHS;
	        ci.extractor = GT->GT.hasAD()?Arrays.stream(GT.getAD()).mapToObj(P->String.valueOf(P)).collect(Collectors.joining(",")):null;
	        columns.add(ci);
	    }
	    if(genotypes.stream().anyMatch(G->G.hasPL())) {
	    	ci = new ColumnInfo();
	        ci.clazz = String.class;
	        ci.name= VCFConstants.GENOTYPE_PL_KEY;
	        ci.extractor = GT->GT.hasPL()?Arrays.stream(GT.getPL()).mapToObj(P->String.valueOf(P)).collect(Collectors.joining(",")):null;
	        columns.add(ci);
	    }
	    for(final String att: genotypes.stream().
	    		flatMap(G->G.getExtendedAttributes().keySet().stream()).
	    		collect(Collectors.toSet())) {
	    	ci = new ColumnInfo();
	        ci.clazz = Object.class;
	        ci.name= att;
	        ci.extractor = GT->GT.hasExtendedAttribute(att)?GT.getExtendedAttribute(att):null;
	        columns.add(ci);
	    	}
	    this.columns.addAll(columns);
		}
    
    super.setRows(genotypes);
	fireTableStructureChanged();
	}

@Override
public int getColumnCount() {
	return this.columns.size();
	}

@Override
public Class<?> getColumnClass(int column) {
	return this.columns.get(column).clazz;
	}
@Override
public String getColumnName(int column) {
	return this.columns.get(column).name;
	}

@Override
public Object getValueOf(final Genotype F, int column) {
	return this.columns.get(column).extractor.apply(F); 
	}
}