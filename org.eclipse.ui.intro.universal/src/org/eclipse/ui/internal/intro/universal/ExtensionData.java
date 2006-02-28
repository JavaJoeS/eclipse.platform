package org.eclipse.ui.internal.intro.universal;

import java.io.PrintWriter;

public class ExtensionData extends BaseData {

	public static final int HIDDEN = -1;

	public static final int CALLOUT = 0;

	public static final int LOW = 1;

	public static final int MEDIUM = 2;

	public static final int HIGH = 3;

	public static final int NEW = 4;

	private String name;

	private int fImportance = LOW;

	private boolean implicit = false;

	public static final String[] IMPORTANCE_TABLE = {
			IUniversalIntroConstants.CALLOUT, IUniversalIntroConstants.LOW,
			IUniversalIntroConstants.MEDIUM, IUniversalIntroConstants.HIGH,
			IUniversalIntroConstants.NEW };

	public static final String[] IMPORTANCE_STYLE_TABLE = {
			IUniversalIntroConstants.STYLE_CALLOUT,
			IUniversalIntroConstants.STYLE_LOW,
			IUniversalIntroConstants.STYLE_MEDIUM,
			IUniversalIntroConstants.STYLE_HIGH,
			IUniversalIntroConstants.STYLE_NEW };

	public static final String[] IMPORTANCE_NAME_TABLE = {
			org.eclipse.ui.internal.intro.universal.Messages.ExtensionData_callout,
			Messages.ExtensionData_low, Messages.ExtensionData_medium,
			Messages.ExtensionData_high, Messages.ExtensionData_new };

	public ExtensionData(String id, String name) {
		this(id, name, IUniversalIntroConstants.LOW, false);
	}

	public ExtensionData(String id, String name, int importance) {
		this.id = id;
		this.name = name;
		this.fImportance = importance;
		this.implicit = false;
	}

	public boolean isImplicit() {
		return implicit;
	}

	public ExtensionData(String id, String name, String importance,
			boolean implicit) {
		this.id = id;
		this.name = name;
		this.implicit = implicit;
		if (importance != null) {
			if (importance.equals(IUniversalIntroConstants.HIGH))
				fImportance = HIGH;
			else if (importance.equals(IUniversalIntroConstants.MEDIUM))
				fImportance = MEDIUM;
			else if (importance.equals(IUniversalIntroConstants.LOW))
				fImportance = LOW;
			else if (importance.equals(IUniversalIntroConstants.CALLOUT))
				fImportance = CALLOUT;
			else if (importance.equals(IUniversalIntroConstants.NEW))
				fImportance = NEW;
			else if (importance.equals(IUniversalIntroConstants.HIDDEN))
				fImportance = HIDDEN;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getImportance() {
		return fImportance;
	}

	public void setImportance(int newValue) {
		fImportance = newValue;
	}

	public boolean isHidden() {
		return fImportance == HIDDEN;
	}

	String getImportanceAttributeValue() {
		return IMPORTANCE_TABLE[fImportance];
	}

	public String toString() {
		return name != null ? name : id;
	}

	public void write(PrintWriter writer, String indent) {
		writer.print(indent);
		writer.print("<extension id=\"" + id + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (!isHidden())
			writer
					.println(" importance=\"" + getImportanceAttributeValue() + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			writer.println("/>"); //$NON-NLS-1$
	}
}