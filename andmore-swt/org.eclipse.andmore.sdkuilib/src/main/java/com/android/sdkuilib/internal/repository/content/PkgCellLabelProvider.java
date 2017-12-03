package com.android.sdkuilib.internal.repository.content;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.android.sdkuilib.internal.repository.content.INode.LabelFont;
import org.eclipse.andmore.base.resources.ImageFactory;

public class PkgCellLabelProvider extends ColumnLabelProvider implements ITableFontProvider {

	public static final int NAME = 1;
	public static final int API = 2;
	public static final int REVISION = 3;
	public static final int STATUS = 4;
    private final int columnIndex;
    private final PkgCellAgent agent;
 
    public PkgCellLabelProvider(PkgCellAgent agent, int columnIndex) {
        super();
        this.columnIndex = columnIndex;
        this.agent = agent;
    }

    @Override
    public String getText(Object element) {
    	INode node = (INode)element;
    	String text = node.getText(element, columnIndex);
    	return text != INode.VOID ? text : null;
    }
    
    /**
     * The image is owned by the label provider and must not be disposed directly.
     */
    @Override
    public Image getImage(Object element) {
        ImageFactory imgFactory = agent.getImgFactory();
        if (imgFactory != null) {
        	INode node = (INode)element;
        	String reference = node.getImage(element, columnIndex);
        	if (reference != INode.VOID)
        		return imgFactory.getImageByName(reference);
        }
        return super.getImage(element);
    }

    // -- ITableFontProvider

    @Override
    public Font getFont(Object element, int columnIndex) {
    	INode node = (INode)element;
    	LabelFont fontType = node.getFont(element, columnIndex);
    	if (fontType == LabelFont.italic)
            return agent.getTreeFontItalic();
        return super.getFont(element);
    }

    // -- Tooltip support 
    @Override
    public String getToolTipText(Object element) {
    	INode node = (INode)element;
    	String text = node.getToolTipText(element);
    	if (text != INode.VOID)
    		return text;
        return super.getToolTipText(element);
    }

    @Override
    public Point getToolTipShift(Object object) {
        return new Point(15, 5);
    }

    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        return 500;
    }
}
