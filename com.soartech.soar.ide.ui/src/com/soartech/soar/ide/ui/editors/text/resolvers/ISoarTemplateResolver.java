package com.soartech.soar.ide.ui.editors.text.resolvers;

import com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditorConfiguration;

public interface ISoarTemplateResolver {
	void setConfiguration(SoarDatabaseTextEditorConfiguration configuration);
	boolean canResolve();
}